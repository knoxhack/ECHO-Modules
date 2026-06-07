package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncNpcScreenStatePacket(EchoNpcScreenState state) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "sync_npc_screen_state");
    public static final Type<SyncNpcScreenStatePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SyncNpcScreenStatePacket> CODEC = StreamCodec.of(
            (buf, packet) -> EchoNpcScreenState.write(buf, packet.state),
            buf -> new SyncNpcScreenStatePacket(EchoNpcScreenState.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
