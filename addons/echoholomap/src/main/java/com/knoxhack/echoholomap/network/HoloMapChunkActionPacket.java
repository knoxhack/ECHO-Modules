package com.knoxhack.echoholomap.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoholomap.EchoHoloMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HoloMapChunkActionPacket(
        Identifier providerId,
        Identifier actionId,
        String dimension,
        int chunkX,
        int chunkZ) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "chunk_action");
    public static final Type<HoloMapChunkActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapChunkActionPacket> CODEC =
            StreamCodec.of(HoloMapChunkActionPacket::write, HoloMapChunkActionPacket::read);

    public HoloMapChunkActionPacket {
        providerId = providerId == null ? Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "chunk_action/none") : providerId;
        actionId = actionId == null ? Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "chunk_action/none") : actionId;
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, HoloMapChunkActionPacket packet) {
        EchoPayloadCodecs.writeIdentifier(buffer, packet.providerId());
        EchoPayloadCodecs.writeIdentifier(buffer, packet.actionId());
        buffer.writeUtf(packet.dimension(), EchoPayloadCodecs.ID);
        buffer.writeVarInt(packet.chunkX());
        buffer.writeVarInt(packet.chunkZ());
    }

    private static HoloMapChunkActionPacket read(RegistryFriendlyByteBuf buffer) {
        return new HoloMapChunkActionPacket(
                EchoPayloadCodecs.readIdentifier(buffer),
                EchoPayloadCodecs.readIdentifier(buffer),
                buffer.readUtf(EchoPayloadCodecs.ID),
                buffer.readVarInt(),
                buffer.readVarInt());
    }
}
