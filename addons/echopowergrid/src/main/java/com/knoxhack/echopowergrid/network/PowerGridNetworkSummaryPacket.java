package com.knoxhack.echopowergrid.network;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.PowerGridNetworkSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record PowerGridNetworkSummaryPacket(List<Entry> networks, String statusLine, long gameTime)
        implements CustomPacketPayload {
    private static final int MAX_NETWORKS = 96;
    private static final int MAX_TEXT = 192;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "network_summary");
    public static final Type<PowerGridNetworkSummaryPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerGridNetworkSummaryPacket> CODEC =
            StreamCodec.of(PowerGridNetworkSummaryPacket::write, PowerGridNetworkSummaryPacket::read);

    public PowerGridNetworkSummaryPacket {
        networks = List.copyOf(networks == null ? List.of() : networks.stream().limit(MAX_NETWORKS).toList());
        statusLine = clean(statusLine, "PowerGrid awaiting sync.");
        gameTime = Math.max(0L, gameTime);
    }

    public static PowerGridNetworkSummaryPacket current(ServerPlayer player) {
        if (player == null) {
            return new PowerGridNetworkSummaryPacket(List.of(), "PowerGrid offline.", 0L);
        }
        List<Entry> entries = EchoPowerGridApi.loadedNetworkSummaries(player.level()).stream()
                .sorted(Comparator
                        .comparing((PowerGridNetworkSummary summary) -> summary.dimension().identifier().toString())
                        .thenComparing(summary -> summary.networkId().toString()))
                .limit(MAX_NETWORKS)
                .map(Entry::from)
                .toList();
        long stressed = entries.stream()
                .filter(entry -> "BROWNOUT".equals(entry.state()) || "OVERLOADED".equals(entry.state()))
                .count();
        String status = "Networks " + entries.size() + " / stressed " + stressed + " / t+" + player.level().getGameTime();
        return new PowerGridNetworkSummaryPacket(entries, status, player.level().getGameTime());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, PowerGridNetworkSummaryPacket packet) {
        buffer.writeVarInt(packet.networks().size());
        for (Entry entry : packet.networks()) {
            buffer.writeLong(entry.networkId().getMostSignificantBits());
            buffer.writeLong(entry.networkId().getLeastSignificantBits());
            buffer.writeUtf(entry.dimension(), MAX_TEXT);
            buffer.writeBlockPos(entry.anchorPos());
            buffer.writeUtf(entry.state(), 32);
            buffer.writeUtf(entry.quality(), 32);
            buffer.writeLong(entry.totalGeneration());
            buffer.writeLong(entry.totalDemand());
            buffer.writeLong(entry.availablePower());
            buffer.writeLong(entry.totalStored());
            buffer.writeLong(entry.totalCapacity());
            buffer.writeVarInt(entry.nodeCount());
            buffer.writeLong(entry.transferLimit());
        }
        buffer.writeUtf(packet.statusLine(), MAX_TEXT);
        buffer.writeVarLong(packet.gameTime());
    }

    private static PowerGridNetworkSummaryPacket read(RegistryFriendlyByteBuf buffer) {
        int count = Math.max(0, Math.min(MAX_NETWORKS, buffer.readVarInt()));
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID networkId = new UUID(buffer.readLong(), buffer.readLong());
            String dimension = buffer.readUtf(MAX_TEXT);
            BlockPos anchorPos = buffer.readBlockPos();
            String state = buffer.readUtf(32);
            String quality = buffer.readUtf(32);
            long totalGeneration = buffer.readLong();
            long totalDemand = buffer.readLong();
            long availablePower = buffer.readLong();
            long totalStored = buffer.readLong();
            long totalCapacity = buffer.readLong();
            int nodeCount = buffer.readVarInt();
            long transferLimit = buffer.readLong();
            entries.add(new Entry(networkId, dimension, anchorPos, state, quality, totalGeneration, totalDemand,
                    availablePower, totalStored, totalCapacity, nodeCount, transferLimit));
        }
        return new PowerGridNetworkSummaryPacket(entries, buffer.readUtf(MAX_TEXT), buffer.readVarLong());
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    public record Entry(
            UUID networkId,
            String dimension,
            BlockPos anchorPos,
            String state,
            String quality,
            long totalGeneration,
            long totalDemand,
            long availablePower,
            long totalStored,
            long totalCapacity,
            int nodeCount,
            long transferLimit) {
        public Entry {
            networkId = networkId == null ? new UUID(0L, 0L) : networkId;
            dimension = clean(dimension, "minecraft:overworld");
            anchorPos = anchorPos == null ? BlockPos.ZERO : anchorPos.immutable();
            state = clean(state, "OFFLINE");
            quality = clean(quality, "STABLE");
            totalGeneration = Math.max(0L, totalGeneration);
            totalDemand = Math.max(0L, totalDemand);
            availablePower = Math.max(0L, availablePower);
            totalStored = Math.max(0L, totalStored);
            totalCapacity = Math.max(0L, totalCapacity);
            nodeCount = Math.max(0, nodeCount);
            transferLimit = Math.max(0L, transferLimit);
        }

        static Entry from(PowerGridNetworkSummary summary) {
            return new Entry(
                    summary.networkId(),
                    summary.dimension().identifier().toString(),
                    summary.anchorPos(),
                    summary.state().name(),
                    summary.quality().name(),
                    summary.totalGeneration(),
                    summary.totalDemand(),
                    summary.availablePower(),
                    summary.totalStored(),
                    summary.totalCapacity(),
                    summary.nodeCount(),
                    summary.transferLimit());
        }
    }
}
