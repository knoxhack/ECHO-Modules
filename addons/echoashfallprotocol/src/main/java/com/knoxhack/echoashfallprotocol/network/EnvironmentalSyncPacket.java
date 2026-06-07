package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Network packet to sync environmental event state from server to clients.
 */
public record EnvironmentalSyncPacket(
    String eventType,
    long eventStartTime,
    int eventDuration,
    long gameTime,
    float intensity,
    float phase,
    long seed,
    int radiationStormsSurvived,
    int toxicStormsSurvived,
    int blackoutsSurvived,
    int ashStormsSurvived,
    int cryoFrontsSurvived,
    int nexusSurgesSurvived
) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "environmental_sync");

    public static final StreamCodec<FriendlyByteBuf, EnvironmentalSyncPacket> CODEC = StreamCodec.of(
        (buf, packet) -> {
            buf.writeUtf(packet.eventType);
            buf.writeLong(packet.eventStartTime);
            buf.writeInt(packet.eventDuration);
            buf.writeLong(packet.gameTime);
            buf.writeFloat(packet.intensity);
            buf.writeFloat(packet.phase);
            buf.writeLong(packet.seed);
            buf.writeInt(packet.radiationStormsSurvived);
            buf.writeInt(packet.toxicStormsSurvived);
            buf.writeInt(packet.blackoutsSurvived);
            buf.writeInt(packet.ashStormsSurvived);
            buf.writeInt(packet.cryoFrontsSurvived);
            buf.writeInt(packet.nexusSurgesSurvived);
        },
        buf -> new EnvironmentalSyncPacket(
            buf.readUtf(),
            buf.readLong(),
            buf.readInt(),
            buf.readLong(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readLong(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        )
    );

    public static final Type<EnvironmentalSyncPacket> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
