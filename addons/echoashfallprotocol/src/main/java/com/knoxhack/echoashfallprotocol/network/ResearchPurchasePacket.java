package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Network packet to request perk purchase from client to server.
 */
public record ResearchPurchasePacket(String perkId) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "research_purchase");

    public static final StreamCodec<FriendlyByteBuf, ResearchPurchasePacket> CODEC = StreamCodec.of(
        (buf, packet) -> EchoPayloadCodecs.writeUtf(buf, packet.perkId, EchoPayloadCodecs.ID),
        buf -> new ResearchPurchasePacket(EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID))
    );

    public static final Type<ResearchPurchasePacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
