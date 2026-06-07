package com.knoxhack.echoindex.network;

import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IndexActionPacket(Action action, Identifier targetId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoIndex.MODID, "index_action");
    public static final Type<IndexActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, IndexActionPacket> CODEC =
            StreamCodec.of(IndexActionPacket::write, IndexActionPacket::read);

    public IndexActionPacket {
        action = action == null ? Action.REQUEST_SYNC : action;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, IndexActionPacket packet) {
        buffer.writeUtf(packet.action().name(), 32);
        buffer.writeUtf(packet.targetId() == null ? "" : packet.targetId().toString(), 192);
    }

    private static IndexActionPacket read(FriendlyByteBuf buffer) {
        Action action = safeAction(buffer.readUtf(32));
        Identifier id = Identifier.tryParse(buffer.readUtf(192));
        return new IndexActionPacket(action, id);
    }

    private static Action safeAction(String value) {
        try {
            return Action.valueOf(value);
        } catch (RuntimeException exception) {
            return Action.REQUEST_SYNC;
        }
    }

    public enum Action {
        REQUEST_SYNC,
        MARK_READ,
        BOOKMARK,
        UNBOOKMARK,
        PIN_RECIPE,
        UNPIN_RECIPE,
        TRANSFER_RECIPE
    }
}
