package com.knoxhack.echoindex.network;

import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IndexStateSyncPacket(CompoundTag state) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoIndex.MODID, "state_sync");
    public static final Type<IndexStateSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, IndexStateSyncPacket> CODEC =
            StreamCodec.of(IndexStateSyncPacket::write, IndexStateSyncPacket::read);

    public IndexStateSyncPacket {
        state = state == null ? new CompoundTag() : state.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, IndexStateSyncPacket packet) {
        buffer.writeNbt(packet.state());
    }

    private static IndexStateSyncPacket read(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new IndexStateSyncPacket(tag == null ? new CompoundTag() : tag);
    }
}
