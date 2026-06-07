package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenNpcScreenPacket(EchoNpcScreenState state) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "open_npc_screen");
    public static final Type<OpenNpcScreenPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, OpenNpcScreenPacket> CODEC = StreamCodec.of(
            (buf, packet) -> EchoNpcScreenState.write(buf, packet.state),
            buf -> new OpenNpcScreenPacket(EchoNpcScreenState.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
