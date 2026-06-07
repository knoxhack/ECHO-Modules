package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Network packet to request mission turn-in from client to server.
 */
public record MissionTurnInPacket(String missionId) implements CustomPacketPayload {
    
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "mission_turn_in");
    
    public static final StreamCodec<FriendlyByteBuf, MissionTurnInPacket> CODEC = StreamCodec.of(
        (buf, packet) -> EchoPayloadCodecs.writeUtf(buf, packet.missionId, EchoPayloadCodecs.ID),
        buf -> new MissionTurnInPacket(EchoPayloadCodecs.readUtf(buf, EchoPayloadCodecs.ID))
    );
    
    public static final Type<MissionTurnInPacket> TYPE = new Type<>(ID);
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
