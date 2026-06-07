package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseNpcInteractionPacket(int entityId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "close_interaction");
    public static final Type<CloseNpcInteractionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, CloseNpcInteractionPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeVarInt(packet.entityId),
            buf -> new CloseNpcInteractionPacket(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
