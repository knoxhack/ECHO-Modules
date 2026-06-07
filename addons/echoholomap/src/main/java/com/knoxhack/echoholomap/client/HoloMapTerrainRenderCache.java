package com.knoxhack.echoholomap.client;

import com.knoxhack.echoholomap.map.HoloMapTerrainTile;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class HoloMapTerrainRenderCache {
    private static final int MAX_ENTRIES = 2048;
    private static final Map<Key, StyledTile> CACHE = new LinkedHashMap<>(256, 0.75F, true);
    private static long builds;

    private HoloMapTerrainRenderCache() {
    }

    static synchronized StyledTile styled(HoloMapTerrainTile tile) {
        Key key = Key.from(tile);
        StyledTile cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        StyledTile built = StyledTile.from(tile);
        CACHE.put(key, built);
        builds++;
        trim();
        return built;
    }

    static synchronized void clearForTests() {
        CACHE.clear();
        builds = 0L;
    }

    static synchronized int entryCountForTests() {
        return CACHE.size();
    }

    static synchronized long buildsForTests() {
        return builds;
    }

    private static void trim() {
        while (CACHE.size() > MAX_ENTRIES) {
            CACHE.remove(CACHE.keySet().iterator().next());
        }
    }

    private static int styleStamp() {
        int hash = Double.hashCode(HoloMapVisualStyle.terrainBrightness());
        return 31 * hash + Double.hashCode(HoloMapVisualStyle.terrainContrast());
    }

    record StyledTile(int averageColor, int[] pixels) {
        private static StyledTile from(HoloMapTerrainTile tile) {
            int[] styledPixels = new int[HoloMapTerrainTile.PIXELS];
            long a = 0L;
            long r = 0L;
            long g = 0L;
            long b = 0L;
            for (int z = 0; z < HoloMapTerrainTile.SIZE; z++) {
                for (int x = 0; x < HoloMapTerrainTile.SIZE; x++) {
                    int color = HoloMapVisualStyle.terrainColor(tile.pixel(x, z));
                    styledPixels[z * HoloMapTerrainTile.SIZE + x] = color;
                    a += (color >>> 24) & 0xFF;
                    r += (color >>> 16) & 0xFF;
                    g += (color >>> 8) & 0xFF;
                    b += color & 0xFF;
                }
            }
            int count = Math.max(1, styledPixels.length);
            int average = ((int) (a / count) << 24)
                    | ((int) (r / count) << 16)
                    | ((int) (g / count) << 8)
                    | (int) (b / count);
            return new StyledTile(average, styledPixels);
        }

        int pixel(int localX, int localZ) {
            int x = Math.max(0, Math.min(HoloMapTerrainTile.SIZE - 1, localX));
            int z = Math.max(0, Math.min(HoloMapTerrainTile.SIZE - 1, localZ));
            return pixels[z * HoloMapTerrainTile.SIZE + x];
        }
    }

    private record Key(String dimension, int chunkX, int chunkZ, long sampledTime, int version,
            HoloMapTerrainTile.DetailMode detailMode, int styleStamp, int pixelHash) {
        private static Key from(HoloMapTerrainTile tile) {
            return new Key(tile.dimension(), tile.chunkX(), tile.chunkZ(), tile.sampledTime(), tile.version(),
                    tile.detailMode(), HoloMapTerrainRenderCache.styleStamp(), Arrays.hashCode(tile.pixels()));
        }
    }
}
