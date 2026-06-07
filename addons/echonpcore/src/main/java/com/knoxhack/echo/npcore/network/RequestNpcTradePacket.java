package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestNpcTradePacket(int entityId, String offerId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "request_trade");
    public static final Type<RequestNpcTradePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RequestNpcTradePacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                EchoPayloadCodecs.writeUtf(buf, packet.offerId, EchoPayloadCodecs.ID);
            },
            buf -> new RequestNpcTradePacket(buf.readVarInt(), EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID)));

    public RequestNpcTradePacket {
        offerId = offerId == null ? "" : offerId.trim();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
