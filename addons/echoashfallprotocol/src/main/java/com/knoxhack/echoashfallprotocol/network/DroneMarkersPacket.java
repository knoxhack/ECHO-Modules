package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMarker;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneScanCategory;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record DroneMarkersPacket(List<Entry> markers, long gameTime) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drone_markers");
    public static final Type<DroneMarkersPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, DroneMarkersPacket> CODEC =
            StreamCodec.of(DroneMarkersPacket::write, DroneMarkersPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static DroneMarkersPacket of(List<EchoDroneMarker> markers, long gameTime) {
        List<Entry> entries = markers == null ? List.of() : markers.stream()
                .map(Entry::of)
                .toList();
        return new DroneMarkersPacket(entries, gameTime);
    }

    private static void write(FriendlyByteBuf buf, DroneMarkersPacket packet) {
        List<Entry> entries = packet.markers == null ? List.of() : packet.markers;
        buf.writeVarInt(Math.min(entries.size(), 32));
        for (int i = 0; i < Math.min(entries.size(), 32); i++) {
            Entry entry = entries.get(i);
            buf.writeEnum(entry.category);
            EchoPayloadCodecs.writeUtf(buf, entry.label, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buf, entry.detail, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buf, entry.dimension, EchoPayloadCodecs.ID);
            buf.writeBlockPos(entry.pos);
            buf.writeLong(entry.expiresAt);
            buf.writeBoolean(entry.precise);
        }
        buf.writeLong(packet.gameTime);
    }

    private static DroneMarkersPacket read(FriendlyByteBuf buf) {
        int count = Math.max(0, Math.min(32, buf.readVarInt()));
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(
                    buf.readEnum(EchoDroneScanCategory.class),
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID),
                    buf.readBlockPos(),
                    buf.readLong(),
                    buf.readBoolean()));
        }
        return new DroneMarkersPacket(entries, buf.readLong());
    }

    public record Entry(EchoDroneScanCategory category, String label, String detail, String dimension,
                        BlockPos pos, long expiresAt, boolean precise) {
        private static Entry of(EchoDroneMarker marker) {
            ResourceKey<Level> dimension = marker.dimension();
            return new Entry(
                    marker.category(),
                    marker.label(),
                    marker.detail(),
                    dimension == null ? Level.OVERWORLD.identifier().toString() : dimension.identifier().toString(),
                    marker.pos(),
                    marker.expiresAt(),
                    marker.precise());
        }
    }
}
