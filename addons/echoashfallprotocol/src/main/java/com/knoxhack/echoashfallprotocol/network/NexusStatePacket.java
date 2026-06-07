package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Network packet to sync Nexus world state from server to clients.
 */
public record NexusStatePacket(
    String worldState,
    long choiceTime,
    String playerName,
    int nexusX,
    int nexusY,
    int nexusZ,
    boolean campaignAwakened,
    int nexusInstability,
    int relaysScanned,
    int relaysResolved,
    int readinessRestore,
    int readinessDestroy,
    int readinessControl,
    boolean siegeComplete,
    boolean warfrontComplete,
    boolean wardenDefeated,
    boolean finaleComplete,
    String relaySummaryPayload
) implements CustomPacketPayload {
    
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "nexus_state");
    
    public static final StreamCodec<FriendlyByteBuf, NexusStatePacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUtf(packet.worldState);
            buf.writeLong(packet.choiceTime);
            buf.writeUtf(packet.playerName);
            buf.writeInt(packet.nexusX);
            buf.writeInt(packet.nexusY);
            buf.writeInt(packet.nexusZ);
            buf.writeBoolean(packet.campaignAwakened);
            buf.writeVarInt(packet.nexusInstability);
            buf.writeVarInt(packet.relaysScanned);
            buf.writeVarInt(packet.relaysResolved);
            buf.writeVarInt(packet.readinessRestore);
            buf.writeVarInt(packet.readinessDestroy);
            buf.writeVarInt(packet.readinessControl);
            buf.writeBoolean(packet.siegeComplete);
            buf.writeBoolean(packet.warfrontComplete);
            buf.writeBoolean(packet.wardenDefeated);
            buf.writeBoolean(packet.finaleComplete);
            buf.writeUtf(packet.relaySummaryPayload);
        },
        buf -> new NexusStatePacket(
            buf.readUtf(),
            buf.readLong(),
            buf.readUtf(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readUtf()
        )
    );
    
    public static final Type<NexusStatePacket> TYPE = new Type<>(ID);
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static NexusStatePacket fromWorldData(NexusWorldData data) {
        return new NexusStatePacket(
            data.getState().name(),
            data.getChoiceTime(),
            data.getPlayerName(),
            data.getNexusPos().getX(),
            data.getNexusPos().getY(),
            data.getNexusPos().getZ(),
            false,
            0,
            0,
            0,
            0,
            0,
            0,
            false,
            false,
            false,
            false,
            ""
        );
    }

    public static NexusStatePacket fromWorldData(NexusWorldData data, NexusCampaignData campaign) {
        NexusCampaignData safeCampaign = campaign == null ? new NexusCampaignData() : campaign;
        return new NexusStatePacket(
            data.getState().name(),
            data.getChoiceTime(),
            data.getPlayerName(),
            data.getNexusPos().getX(),
            data.getNexusPos().getY(),
            data.getNexusPos().getZ(),
            safeCampaign.isAwakened(),
            safeCampaign.getInstability(),
            safeCampaign.getScannedRelayCount(),
            safeCampaign.getResolvedRelayCount(),
            safeCampaign.getReadinessRestore(),
            safeCampaign.getReadinessDestroy(),
            safeCampaign.getReadinessControl(),
            safeCampaign.isSiegeComplete(),
            safeCampaign.isWarfrontComplete(),
            safeCampaign.isWardenDefeated(),
            safeCampaign.isFinaleComplete(),
            safeCampaign.relaySummaryPayload()
        );
    }
    
    public NexusWorldData.WorldState getState() {
        try {
            return NexusWorldData.WorldState.valueOf(worldState);
        } catch (IllegalArgumentException e) {
            return NexusWorldData.WorldState.NORMAL;
        }
    }
}
