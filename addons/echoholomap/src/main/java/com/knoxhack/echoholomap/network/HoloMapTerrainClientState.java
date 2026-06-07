package com.knoxhack.echoholomap.network;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HoloMapTerrainClientState {
    private static final Map<TileKey, HoloMapTerrainTile> TILES =
            new LinkedHashMap<>(256, 0.75F, true);
    private static final Map<String, Map<ChunkKey, HoloMapTerrainTile>> TILES_BY_DIMENSION = new LinkedHashMap<>();
    private static String currentDimension = "minecraft:overworld";
    private static int discoveredCount = 0;
    private static long lastGameTime = 0L;
    private static long revision = 0L;
    private static List<HoloMapTerrainTile> cachedVisibleTiles = List.of();
    private static String cachedVisibleDimension = "";
    private static int cachedMinChunkX = Integer.MIN_VALUE;
    private static int cachedMaxChunkX = Integer.MIN_VALUE;
    private static int cachedMinChunkZ = Integer.MIN_VALUE;
    private static int cachedMaxChunkZ = Integer.MIN_VALUE;
    private static final Map<String, DetailStats> DETAIL_STATS = new LinkedHashMap<>();

    private HoloMapTerrainClientState() {
    }

    public static synchronized void apply(HoloMapTileBatchPacket packet) {
        if (packet == null) {
            return;
        }
        currentDimension = normalizeDimension(packet.dimension());
        discoveredCount = packet.discoveredCount();
        lastGameTime = packet.gameTime();
        pruneNonRenderable(currentDimension);
        if (packet.discoveredCount() == 0 && packet.tiles().isEmpty()) {
            TILES.keySet().removeIf(key -> key.dimension().equals(currentDimension));
            TILES_BY_DIMENSION.remove(currentDimension);
        }
        for (HoloMapTerrainTile tile : packet.tiles()) {
            HoloMapTerrainTile copy = tile.copy();
            if (!copy.renderableSurface()) {
                removeTile(new TileKey(copy.dimension(), copy.chunkX(), copy.chunkZ()));
                continue;
            }
            TileKey key = new TileKey(copy.dimension(), copy.chunkX(), copy.chunkZ());
            TILES.put(key, copy);
            TILES_BY_DIMENSION.computeIfAbsent(key.dimension(), ignored -> new LinkedHashMap<>())
                    .put(new ChunkKey(key.chunkX(), key.chunkZ()), copy);
        }
        trim();
        revision++;
        invalidateCaches();
    }

    public static synchronized List<HoloMapTerrainTile> tiles(String dimension,
            int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
        String dim = normalizeDimension(dimension);
        if (dim.equals(cachedVisibleDimension)
                && minChunkX == cachedMinChunkX
                && maxChunkX == cachedMaxChunkX
                && minChunkZ == cachedMinChunkZ
                && maxChunkZ == cachedMaxChunkZ) {
            return cachedVisibleTiles;
        }
        List<HoloMapTerrainTile> visible = new ArrayList<>();
        Map<ChunkKey, HoloMapTerrainTile> dimensionTiles = TILES_BY_DIMENSION.get(dim);
        if (dimensionTiles != null) {
            long columns = (long) maxChunkX - minChunkX + 1L;
            long rows = (long) maxChunkZ - minChunkZ + 1L;
            long gridLookups = Math.max(0L, columns) * Math.max(0L, rows);
            if (gridLookups > dimensionTiles.size() * 2L) {
                for (Map.Entry<ChunkKey, HoloMapTerrainTile> entry : dimensionTiles.entrySet()) {
                    ChunkKey chunk = entry.getKey();
                    if (chunk.chunkX() < minChunkX || chunk.chunkX() > maxChunkX
                            || chunk.chunkZ() < minChunkZ || chunk.chunkZ() > maxChunkZ) {
                        continue;
                    }
                    HoloMapTerrainTile tile = entry.getValue();
                    if (tile != null && tile.renderableSurface()) {
                        TILES.get(new TileKey(dim, chunk.chunkX(), chunk.chunkZ()));
                        visible.add(tile);
                    }
                }
            } else {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        HoloMapTerrainTile tile = dimensionTiles.get(new ChunkKey(chunkX, chunkZ));
                        if (tile != null && tile.renderableSurface()) {
                            TILES.get(new TileKey(dim, chunkX, chunkZ));
                            visible.add(tile);
                        }
                    }
                }
            }
        }
        cachedVisibleDimension = dim;
        cachedMinChunkX = minChunkX;
        cachedMaxChunkX = maxChunkX;
        cachedMinChunkZ = minChunkZ;
        cachedMaxChunkZ = maxChunkZ;
        cachedVisibleTiles = List.copyOf(visible);
        return cachedVisibleTiles;
    }

    public static synchronized int tileCount(String dimension) {
        String dim = normalizeDimension(dimension);
        int count = 0;
        for (TileKey key : TILES.keySet()) {
            if (key.dimension().equals(dim)) {
                count++;
            }
        }
        return count;
    }

    public static synchronized boolean hasRenderableTile(String dimension, int chunkX, int chunkZ) {
        String dim = normalizeDimension(dimension);
        Map<ChunkKey, HoloMapTerrainTile> dimensionTiles = TILES_BY_DIMENSION.get(dim);
        if (dimensionTiles == null) {
            return false;
        }
        HoloMapTerrainTile tile = dimensionTiles.get(new ChunkKey(chunkX, chunkZ));
        return tile != null && tile.renderableSurface();
    }

    public static synchronized DetailStats detailStats(String dimension) {
        String dim = normalizeDimension(dimension);
        DetailStats cached = DETAIL_STATS.get(dim);
        if (cached != null) {
            return cached;
        }
        int total = 0;
        int legacy = 0;
        int biomeFallback = 0;
        int surfaceBlock = 0;
        int surfaceShaded = 0;
        long newestSample = 0L;
        for (HoloMapTerrainTile tile : TILES.values()) {
            if (!tile.dimension().equals(dim)) {
                continue;
            }
            total++;
            if (tile.version() < HoloMapTerrainTile.CURRENT_VERSION) {
                legacy++;
            }
            if (tile.detailMode() == HoloMapTerrainTile.DetailMode.BIOME_FALLBACK) {
                biomeFallback++;
            } else if (tile.detailMode() == HoloMapTerrainTile.DetailMode.SURFACE_BLOCK) {
                surfaceBlock++;
            } else if (tile.detailMode() == HoloMapTerrainTile.DetailMode.SURFACE_SHADED) {
                surfaceShaded++;
            }
            newestSample = Math.max(newestSample, tile.sampledTime());
        }
        DetailStats stats = new DetailStats(total, legacy, biomeFallback, surfaceBlock, surfaceShaded, newestSample);
        DETAIL_STATS.put(dim, stats);
        return stats;
    }

    public static synchronized int discoveredCount() {
        return discoveredCount;
    }

    public static synchronized long lastGameTime() {
        return lastGameTime;
    }

    public static synchronized long revision() {
        return revision;
    }

    public static synchronized void clear() {
        TILES.clear();
        TILES_BY_DIMENSION.clear();
        discoveredCount = 0;
        lastGameTime = 0L;
        revision++;
        invalidateCaches();
    }

    private static void trim() {
        int max = maxCacheSize();
        while (TILES.size() > max) {
            TileKey eldest = TILES.keySet().iterator().next();
            removeTile(eldest);
        }
    }

    private static void pruneNonRenderable(String dimension) {
        String dim = normalizeDimension(dimension);
        List<TileKey> stale = TILES.entrySet().stream()
                .filter(entry -> entry.getKey().dimension().equals(dim))
                .filter(entry -> !entry.getValue().renderableSurface())
                .map(Map.Entry::getKey)
                .toList();
        for (TileKey key : stale) {
            removeTile(key);
        }
    }

    private static void removeTile(TileKey key) {
        TILES.remove(key);
        Map<ChunkKey, HoloMapTerrainTile> dimensionTiles = TILES_BY_DIMENSION.get(key.dimension());
        if (dimensionTiles != null) {
            dimensionTiles.remove(new ChunkKey(key.chunkX(), key.chunkZ()));
            if (dimensionTiles.isEmpty()) {
                TILES_BY_DIMENSION.remove(key.dimension());
            }
        }
    }

    private static int maxCacheSize() {
        try {
            return Math.max(1024, Config.TERRAIN_CLIENT_CACHE_SIZE.get());
        } catch (RuntimeException exception) {
            return 4096;
        }
    }

    private static void invalidateCaches() {
        cachedVisibleTiles = List.of();
        cachedVisibleDimension = "";
        cachedMinChunkX = Integer.MIN_VALUE;
        cachedMaxChunkX = Integer.MIN_VALUE;
        cachedMinChunkZ = Integer.MIN_VALUE;
        cachedMaxChunkZ = Integer.MIN_VALUE;
        DETAIL_STATS.clear();
    }

    private static String normalizeDimension(String dimension) {
        return dimension == null || dimension.isBlank() ? currentDimension : dimension.strip();
    }

    public record TileKey(String dimension, int chunkX, int chunkZ) {
        public TileKey {
            dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        }
    }

    private record ChunkKey(int chunkX, int chunkZ) {
    }

    public record DetailStats(int total, int legacy, int biomeFallback, int surfaceBlock, int surfaceShaded,
            long newestSample) {
        public String label() {
            if (total <= 0) {
                return "pending real chunk scan";
            }
            if (surfaceShaded > 0) {
                return "real shaded " + surfaceShaded + "/" + total;
            }
            if (surfaceBlock > 0) {
                return "real block " + surfaceBlock + "/" + total;
            }
            return "pending real surface";
        }

        public String compactLabel() {
            if (total <= 0) {
                return "REAL PENDING";
            }
            if (surfaceShaded > 0) {
                return "REAL SHD " + surfaceShaded;
            }
            if (surfaceBlock > 0) {
                return "REAL BLK " + surfaceBlock;
            }
            return "REAL PENDING";
        }
    }
}
