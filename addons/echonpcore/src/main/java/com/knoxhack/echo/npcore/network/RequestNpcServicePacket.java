package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestNpcServicePacket(int entityId, String serviceId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "request_service");
    public static final Type<RequestNpcServicePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RequestNpcServicePacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                EchoPayloadCodecs.writeUtf(buf, packet.serviceId, EchoPayloadCodecs.ID);
            },
            buf -> new RequestNpcServicePacket(buf.readVarInt(), EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID)));

    public RequestNpcServicePacket {
        serviceId = serviceId == null ? "" : serviceId.trim();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
