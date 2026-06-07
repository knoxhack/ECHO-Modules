package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client request to analyze one schematic fragment in an open Research Lab.
 */
public record ResearchAnalyzeFragmentPacket(String schematicType) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "research_analyze_fragment");

    public static final StreamCodec<FriendlyByteBuf, ResearchAnalyzeFragmentPacket> CODEC = StreamCodec.of(
        (buf, packet) -> EchoPayloadCodecs.writeUtf(buf, packet.schematicType, EchoPayloadCodecs.ID),
        buf -> new ResearchAnalyzeFragmentPacket(EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID))
    );

    public static final Type<ResearchAnalyzeFragmentPacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
