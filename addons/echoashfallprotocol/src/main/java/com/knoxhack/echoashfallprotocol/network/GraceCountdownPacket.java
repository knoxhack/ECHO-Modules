package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Syncs the server-owned new-player grace countdown to the local HUD.
 */
public record GraceCountdownPacket(
    long graceTicksRemaining,
    boolean graceActive
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "grace_countdown");

    public static final StreamCodec<FriendlyByteBuf, GraceCountdownPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeLong(packet.graceTicksRemaining);
            buf.writeBoolean(packet.graceActive);
        },
        buf -> new GraceCountdownPacket(
            buf.readLong(),
            buf.readBoolean()
        )
    );

    public static final Type<GraceCountdownPacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
