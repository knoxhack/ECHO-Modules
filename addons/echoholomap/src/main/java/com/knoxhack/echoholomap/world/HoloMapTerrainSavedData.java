package com.knoxhack.echoholomap.world;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class HoloMapTerrainSavedData extends SavedData {
    private static final Codec<StoredTile> TILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(StoredTile::player),
            Codec.STRING.fieldOf("dimension").forGetter(StoredTile::dimension),
            Codec.INT.fieldOf("chunk_x").forGetter(StoredTile::chunkX),
            Codec.INT.fieldOf("chunk_z").forGetter(StoredTile::chunkZ),
            Codec.LONG.optionalFieldOf("sampled_time", 0L).forGetter(StoredTile::sampledTime),
            Codec.INT.optionalFieldOf("version", HoloMapTerrainTile.LEGACY_VERSION).forGetter(StoredTile::version),
            Codec.STRING.optionalFieldOf("detail_mode", HoloMapTerrainTile.DetailMode.BIOME_FALLBACK.serializedName())
                    .forGetter(StoredTile::detailMode),
            Codec.STRING.fieldOf("pixels").forGetter(StoredTile::pixels)
    ).apply(instance, StoredTile::new));

    public static final Codec<HoloMapTerrainSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TILE_CODEC.listOf().optionalFieldOf("tiles", List.of()).forGetter(HoloMapTerrainSavedData::storedTiles)
    ).apply(instance, HoloMapTerrainSavedData::new));

    public static final SavedDataType<HoloMapTerrainSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "terrain"),
            HoloMapTerrainSavedData::new,
            CODEC);

    private final Map<String, StoredTile> tiles = new LinkedHashMap<>();
    private final Map<String, Map<String, StoredTile>> tilesByOwnerDimension = new LinkedHashMap<>();

    public HoloMapTerrainSavedData() {
    }

    private HoloMapTerrainSavedData(List<StoredTile> storedTiles) {
        for (StoredTile tile : storedTiles) {
            if (tile.valid()) {
                putTile(tile);
            }
        }
    }

    public static HoloMapTerrainSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean saveTile(UUID playerId, ResourceKey<Level> dimension, int chunkX, int chunkZ,
            long sampledTime, int[] pixels) {
        return saveTile(playerId, dimension, chunkX, chunkZ, sampledTime, HoloMapTerrainTile.CURRENT_VERSION,
                HoloMapTerrainTile.DetailMode.SURFACE_SHADED, pixels);
    }

    public boolean saveTile(UUID playerId, ResourceKey<Level> dimension, int chunkX, int chunkZ,
            HoloMapTerrainTile tile) {
        if (tile == null) {
            return false;
        }
        return saveTile(playerId, dimension, chunkX, chunkZ, tile.sampledTime(), tile.version(),
                tile.detailMode(), tile.pixels());
    }

    public boolean saveTile(UUID playerId, ResourceKey<Level> dimension, int chunkX, int chunkZ,
            long sampledTime, int version, HoloMapTerrainTile.DetailMode detailMode, int[] pixels) {
        if (playerId == null || dimension == null || pixels == null) {
            return false;
        }
        String player = playerId.toString();
        String dim = dimension.identifier().toString();
        StoredTile next = StoredTile.from(player, dim, chunkX, chunkZ, sampledTime, version, detailMode, pixels);
        if (!next.renderableSurface()) {
            return false;
        }
        StoredTile previous = putTile(next);
        boolean changed = previous == null || previous.sampledTime() != next.sampledTime()
                || previous.version() != next.version()
                || !previous.detailMode().equals(next.detailMode())
                || !previous.pixels().equals(next.pixels());
        if (changed) {
            evictOldest(player, maxTilesPerPlayer());
            setDirty();
        }
        return changed;
    }

    public boolean needsSample(UUID playerId, ResourceKey<Level> dimension, int chunkX, int chunkZ,
            long now, long resampleInterval) {
        if (playerId == null || dimension == null) {
            return false;
        }
        StoredTile tile = tile(playerId.toString(), dimension.identifier().toString(), chunkX, chunkZ);
        return tile == null
                || !tile.renderableSurface()
                || tile.version() < HoloMapTerrainTile.CURRENT_VERSION
                || resampleInterval <= 0L
                || now - tile.sampledTime() >= resampleInterval;
    }

    public List<HoloMapTerrainTile> tiles(UUID playerId, ResourceKey<Level> dimension,
            int centerChunkX, int centerChunkZ, int radius, int limit) {
        if (playerId == null || dimension == null || limit <= 0) {
            return List.of();
        }
        String player = playerId.toString();
        String dim = dimension.identifier().toString();
        int safeRadius = Math.max(0, Math.min(64, radius));
        int safeLimit = Math.max(1, Math.min(1024, limit));
        List<StoredTile> matching = new ArrayList<>();
        for (StoredTile tile : ownerDimensionTilesOrEmpty(player, dim).values()) {
            if (tile.renderableSurface()
                    && Math.abs(tile.chunkX() - centerChunkX) <= safeRadius
                    && Math.abs(tile.chunkZ() - centerChunkZ) <= safeRadius) {
                matching.add(tile);
            }
        }
        matching.sort(Comparator
                .comparingInt((StoredTile tile) -> distance(tile.chunkX(), tile.chunkZ(), centerChunkX, centerChunkZ))
                .thenComparing(Comparator.comparingLong(StoredTile::sampledTime).reversed()));
        return matching.stream()
                .limit(safeLimit)
                .map(StoredTile::toTile)
                .toList();
    }

    public int discoverableTileCount(UUID playerId, ResourceKey<Level> dimension) {
        if (playerId == null || dimension == null) {
            return 0;
        }
        String player = playerId.toString();
        String dim = dimension.identifier().toString();
        int count = 0;
        for (StoredTile tile : ownerDimensionTilesOrEmpty(player, dim).values()) {
            if (tile.renderableSurface()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasRenderableTile(UUID playerId, ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (playerId == null || dimension == null) {
            return false;
        }
        StoredTile tile = tile(playerId.toString(), dimension.identifier().toString(), chunkX, chunkZ);
        return tile != null && tile.renderableSurface();
    }

    public int clear(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        String player = playerId.toString();
        int before = tiles.size();
        for (StoredTile tile : tiles.values().stream().filter(tile -> tile.player().equals(player)).toList()) {
            removeTile(tile);
        }
        int cleared = before - tiles.size();
        if (cleared > 0) {
            setDirty();
        }
        return cleared;
    }

    public TerrainStats stats(UUID playerId, ResourceKey<Level> dimension) {
        if (playerId == null || dimension == null) {
            return TerrainStats.EMPTY;
        }
        String player = playerId.toString();
        String dim = dimension.identifier().toString();
        int total = 0;
        int legacy = 0;
        int biomeFallback = 0;
        int surfaceBlock = 0;
        int surfaceShaded = 0;
        long newestSample = 0L;
        for (StoredTile tile : ownerDimensionTilesOrEmpty(player, dim).values()) {
            total++;
            if (tile.version() < HoloMapTerrainTile.CURRENT_VERSION) {
                legacy++;
            }
            HoloMapTerrainTile.DetailMode mode = HoloMapTerrainTile.DetailMode.byName(tile.detailMode());
            if (mode == HoloMapTerrainTile.DetailMode.BIOME_FALLBACK) {
                biomeFallback++;
            } else if (mode == HoloMapTerrainTile.DetailMode.SURFACE_BLOCK) {
                surfaceBlock++;
            } else if (mode == HoloMapTerrainTile.DetailMode.SURFACE_SHADED) {
                surfaceShaded++;
            }
            newestSample = Math.max(newestSample, tile.sampledTime());
        }
        return new TerrainStats(total, legacy, biomeFallback, surfaceBlock, surfaceShaded, newestSample);
    }

    public void putForTests(String player, String dimension, int chunkX, int chunkZ, long sampledTime, int[] pixels) {
        putForTests(player, dimension, chunkX, chunkZ, sampledTime, HoloMapTerrainTile.LEGACY_VERSION,
                HoloMapTerrainTile.DetailMode.BIOME_FALLBACK, pixels);
    }

    public void putForTests(String player, String dimension, int chunkX, int chunkZ, long sampledTime,
            int version, HoloMapTerrainTile.DetailMode detailMode, int[] pixels) {
        StoredTile tile = StoredTile.from(player, dimension, chunkX, chunkZ, sampledTime, version, detailMode, pixels);
        putTile(tile);
        setDirty();
    }

    private List<StoredTile> storedTiles() {
        return List.copyOf(tiles.values());
    }

    private void evictOldest(String player, int maxTiles) {
        if (maxTiles <= 0) {
            return;
        }
        List<StoredTile> playerTiles = tiles.values().stream()
                .filter(tile -> tile.player().equals(player))
                .sorted(Comparator.comparingLong(StoredTile::sampledTime))
                .toList();
        int remove = playerTiles.size() - maxTiles;
        for (int i = 0; i < remove; i++) {
            removeTile(playerTiles.get(i));
        }
    }

    private StoredTile tile(String player, String dimension, int chunkX, int chunkZ) {
        return ownerDimensionTilesOrEmpty(player, dimension).get(key(player, dimension, chunkX, chunkZ));
    }

    private StoredTile putTile(StoredTile tile) {
        String key = key(tile.player(), tile.dimension(), tile.chunkX(), tile.chunkZ());
        StoredTile previous = tiles.put(key, tile);
        if (previous != null) {
            removeFromOwnerDimensionIndex(previous);
        }
        ownerDimensionTiles(tile.player(), tile.dimension()).put(key, tile);
        return previous;
    }

    private void removeTile(StoredTile tile) {
        tiles.remove(key(tile.player(), tile.dimension(), tile.chunkX(), tile.chunkZ()));
        removeFromOwnerDimensionIndex(tile);
    }

    private void removeFromOwnerDimensionIndex(StoredTile tile) {
        String ownerDimension = ownerDimensionKey(tile.player(), tile.dimension());
        Map<String, StoredTile> indexed = tilesByOwnerDimension.get(ownerDimension);
        if (indexed == null) {
            return;
        }
        indexed.remove(key(tile.player(), tile.dimension(), tile.chunkX(), tile.chunkZ()));
        if (indexed.isEmpty()) {
            tilesByOwnerDimension.remove(ownerDimension);
        }
    }

    private Map<String, StoredTile> ownerDimensionTiles(String player, String dimension) {
        return tilesByOwnerDimension.computeIfAbsent(ownerDimensionKey(player, dimension), ignored -> new LinkedHashMap<>());
    }

    private Map<String, StoredTile> ownerDimensionTilesOrEmpty(String player, String dimension) {
        Map<String, StoredTile> indexed = tilesByOwnerDimension.get(ownerDimensionKey(player, dimension));
        return indexed == null ? Map.of() : indexed;
    }

    private static int maxTilesPerPlayer() {
        try {
            return Math.max(128, Config.TERRAIN_MAX_TILES_PER_PLAYER.get());
        } catch (RuntimeException exception) {
            return 4096;
        }
    }

    private static int distance(int x, int z, int centerX, int centerZ) {
        int dx = x - centerX;
        int dz = z - centerZ;
        return dx * dx + dz * dz;
    }

    private static String key(String player, String dimension, int chunkX, int chunkZ) {
        return player + "|" + dimension + "|" + chunkX + "|" + chunkZ;
    }

    private static String ownerDimensionKey(String player, String dimension) {
        return player + "|" + dimension;
    }

    public record StoredTile(String player, String dimension, int chunkX, int chunkZ, long sampledTime,
            int version, String detailMode, String pixels) {
        private static StoredTile from(String player, String dimension, int chunkX, int chunkZ,
                long sampledTime, int version, HoloMapTerrainTile.DetailMode detailMode, int[] pixels) {
            return new StoredTile(
                    player == null ? "" : player,
                    dimension == null || dimension.isBlank() ? Level.OVERWORLD.identifier().toString() : dimension,
                    chunkX,
                    chunkZ,
                    Math.max(0L, sampledTime),
                    Math.max(HoloMapTerrainTile.LEGACY_VERSION, version),
                    (detailMode == null ? HoloMapTerrainTile.DetailMode.BIOME_FALLBACK : detailMode).serializedName(),
                    encodePixels(pixels));
        }

        private boolean valid() {
            return !player.isBlank() && !dimension.isBlank() && !pixels.isBlank();
        }

        private HoloMapTerrainTile toTile() {
            return new HoloMapTerrainTile(dimension, chunkX, chunkZ, sampledTime,
                    version, HoloMapTerrainTile.DetailMode.byName(detailMode), decodePixels(pixels));
        }

        private boolean renderableSurface() {
            return version >= HoloMapTerrainTile.CURRENT_VERSION
                    && HoloMapTerrainTile.DetailMode.byName(detailMode) != HoloMapTerrainTile.DetailMode.BIOME_FALLBACK;
        }
    }

    public record TerrainStats(int total, int legacy, int biomeFallback, int surfaceBlock, int surfaceShaded,
            long newestSample) {
        private static final TerrainStats EMPTY = new TerrainStats(0, 0, 0, 0, 0, 0L);

        public String summary() {
            return "v" + HoloMapTerrainTile.CURRENT_VERSION
                    + " total=" + total
                    + " legacy=" + legacy
                    + " bio=" + biomeFallback
                    + " block=" + surfaceBlock
                    + " shaded=" + surfaceShaded
                    + " newest=" + newestSample;
        }
    }

    public static String encodePixels(int[] pixels) {
        int[] safe = new HoloMapTerrainTile("", 0, 0, 0L, pixels).pixels();
        ByteBuffer buffer = ByteBuffer.allocate(HoloMapTerrainTile.PIXELS * Integer.BYTES);
        for (int pixel : safe) {
            buffer.putInt(pixel);
        }
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public static int[] decodePixels(String encoded) {
        int[] pixels = new int[HoloMapTerrainTile.PIXELS];
        java.util.Arrays.fill(pixels, HoloMapTerrainTile.FALLBACK_COLOR);
        if (encoded == null || encoded.isBlank()) {
            return pixels;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            for (int i = 0; i < pixels.length && buffer.remaining() >= Integer.BYTES; i++) {
                pixels[i] = buffer.getInt();
            }
        } catch (IllegalArgumentException exception) {
            return pixels;
        }
        return pixels;
    }
}
