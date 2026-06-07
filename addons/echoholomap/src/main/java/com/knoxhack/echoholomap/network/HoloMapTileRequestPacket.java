package com.knoxhack.echoholomap.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record HoloMapTileRequestPacket(
        String dimension,
        int centerChunkX,
        int centerChunkZ,
        int radius) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "tile_request");
    public static final Type<HoloMapTileRequestPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapTileRequestPacket> CODEC =
            StreamCodec.of(HoloMapTileRequestPacket::write, HoloMapTileRequestPacket::read);

    public HoloMapTileRequestPacket {
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        radius = Math.max(0, Math.min(64, radius));
    }

    public static HoloMapTileRequestPacket forPlayer(Player player, double centerX, double centerZ, int radius) {
        String dim = player == null ? "minecraft:overworld" : player.level().dimension().identifier().toString();
        return new HoloMapTileRequestPacket(dim, Math.floorDiv((int) Math.floor(centerX), 16),
                Math.floorDiv((int) Math.floor(centerZ), 16), radius);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int safeRadius() {
        try {
            return Math.max(0, Math.min(Config.TERRAIN_MAX_REQUEST_RADIUS.get(), radius));
        } catch (RuntimeException exception) {
            return Math.max(0, Math.min(32, radius));
        }
    }

    private static void write(RegistryFriendlyByteBuf buffer, HoloMapTileRequestPacket packet) {
        buffer.writeUtf(packet.dimension(), EchoPayloadCodecs.ID);
        buffer.writeVarInt(packet.centerChunkX());
        buffer.writeVarInt(packet.centerChunkZ());
        buffer.writeVarInt(packet.radius());
    }

    private static HoloMapTileRequestPacket read(RegistryFriendlyByteBuf buffer) {
        return new HoloMapTileRequestPacket(
                buffer.readUtf(EchoPayloadCodecs.ID),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }
}
