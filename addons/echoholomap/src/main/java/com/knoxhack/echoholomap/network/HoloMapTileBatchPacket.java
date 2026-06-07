package com.knoxhack.echoholomap.network;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record HoloMapTileBatchPacket(
        String dimension,
        int discoveredCount,
        long gameTime,
        List<HoloMapTerrainTile> tiles) implements CustomPacketPayload {
    private static final int MAX_TILES_PACKET = 1024;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "tile_batch");
    public static final Type<HoloMapTileBatchPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapTileBatchPacket> CODEC =
            StreamCodec.of(HoloMapTileBatchPacket::write, HoloMapTileBatchPacket::read);

    public HoloMapTileBatchPacket {
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        discoveredCount = Math.max(0, discoveredCount);
        gameTime = Math.max(0L, gameTime);
        tiles = copyTiles(tiles);
    }

    public static HoloMapTileBatchPacket from(ServerPlayer player, HoloMapTileRequestPacket request) {
        if (player == null || request == null || !(player.level() instanceof ServerLevel level)) {
            return new HoloMapTileBatchPacket("minecraft:overworld", 0, 0L, List.of());
        }
        String currentDimension = level.dimension().identifier().toString();
        if (!currentDimension.equals(request.dimension())) {
            return new HoloMapTileBatchPacket(currentDimension, 0, level.getGameTime(), List.of());
        }
        HoloMapTerrainSavedData data = HoloMapTerrainSavedData.get(level);
        int limit = maxBatchSize();
        List<HoloMapTerrainTile> tiles = data.tiles(player.getUUID(), level.dimension(),
                request.centerChunkX(), request.centerChunkZ(), request.safeRadius(), limit);
        int discovered = data.discoverableTileCount(player.getUUID(), level.dimension());
        return new HoloMapTileBatchPacket(currentDimension, discovered, level.getGameTime(), tiles);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, HoloMapTileBatchPacket packet) {
        buffer.writeUtf(packet.dimension(), 96);
        buffer.writeVarInt(packet.discoveredCount());
        buffer.writeVarLong(packet.gameTime());
        buffer.writeVarInt(packet.tiles().size());
        for (HoloMapTerrainTile tile : packet.tiles()) {
            buffer.writeVarInt(tile.chunkX());
            buffer.writeVarInt(tile.chunkZ());
            buffer.writeVarLong(tile.sampledTime());
            buffer.writeVarInt(tile.version());
            buffer.writeUtf(tile.detailMode().serializedName(), 32);
            int[] pixels = tile.pixels();
            for (int pixel : pixels) {
                buffer.writeInt(pixel);
            }
        }
    }

    private static HoloMapTileBatchPacket read(RegistryFriendlyByteBuf buffer) {
        String dimension = buffer.readUtf(96);
        int discovered = buffer.readVarInt();
        long gameTime = buffer.readVarLong();
        int count = Math.max(0, Math.min(MAX_TILES_PACKET, buffer.readVarInt()));
        List<HoloMapTerrainTile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int chunkX = buffer.readVarInt();
            int chunkZ = buffer.readVarInt();
            long sampledTime = buffer.readVarLong();
            int version = buffer.readVarInt();
            HoloMapTerrainTile.DetailMode detailMode = HoloMapTerrainTile.DetailMode.byName(buffer.readUtf(32));
            int[] pixels = new int[HoloMapTerrainTile.PIXELS];
            for (int pixel = 0; pixel < pixels.length; pixel++) {
                pixels[pixel] = buffer.readInt();
            }
            tiles.add(new HoloMapTerrainTile(dimension, chunkX, chunkZ, sampledTime, version, detailMode, pixels));
        }
        return new HoloMapTileBatchPacket(dimension, discovered, gameTime, tiles);
    }

    private static List<HoloMapTerrainTile> copyTiles(List<HoloMapTerrainTile> tiles) {
        if (tiles == null || tiles.isEmpty()) {
            return List.of();
        }
        return tiles.stream()
                .filter(tile -> tile != null && tile.renderableSurface())
                .limit(MAX_TILES_PACKET)
                .map(HoloMapTerrainTile::copy)
                .toList();
    }

    public static int maxBatchSize() {
        try {
            return Math.min(MAX_TILES_PACKET, Math.max(256, Config.TERRAIN_MAX_TILE_BATCH_SIZE.get()));
        } catch (RuntimeException exception) {
            return 384;
        }
    }
}
