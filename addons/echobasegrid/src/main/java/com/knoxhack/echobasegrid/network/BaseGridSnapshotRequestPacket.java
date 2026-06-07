package com.knoxhack.echobasegrid.network;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BaseGridSnapshotRequestPacket(String selectedDimension, int selectedChunkX, int selectedChunkZ)
        implements CustomPacketPayload {
    private static final int MAX_DIMENSION = 160;

    public static final Identifier ID = EchoBaseGrid.id("snapshot_request");
    public static final Type<BaseGridSnapshotRequestPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseGridSnapshotRequestPacket> CODEC =
            StreamCodec.of(BaseGridSnapshotRequestPacket::write, BaseGridSnapshotRequestPacket::read);

    public BaseGridSnapshotRequestPacket {
        selectedDimension = selectedDimension == null ? "" : selectedDimension.strip();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, BaseGridSnapshotRequestPacket packet) {
        buffer.writeUtf(packet.selectedDimension(), MAX_DIMENSION);
        buffer.writeInt(packet.selectedChunkX());
        buffer.writeInt(packet.selectedChunkZ());
    }

    private static BaseGridSnapshotRequestPacket read(RegistryFriendlyByteBuf buffer) {
        return new BaseGridSnapshotRequestPacket(
                buffer.readUtf(MAX_DIMENSION),
                buffer.readInt(),
                buffer.readInt());
    }
}
