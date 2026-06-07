package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestNpcScreenRefreshPacket(int entityId, String tab) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "request_screen_refresh");
    public static final Type<RequestNpcScreenRefreshPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RequestNpcScreenRefreshPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                EchoPayloadCodecs.writeUtf(buf, packet.tab, EchoPayloadCodecs.ID);
            },
            buf -> new RequestNpcScreenRefreshPacket(buf.readVarInt(), EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID)));

    public RequestNpcScreenRefreshPacket {
        tab = tab == null ? "" : tab.trim();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
