package com.knoxhack.echoholomap.map;

import java.util.Arrays;

public record HoloMapTerrainTile(
        String dimension,
        int chunkX,
        int chunkZ,
        long sampledTime,
        int version,
        DetailMode detailMode,
        int[] pixels) {
    public static final int SIZE = 16;
    public static final int PIXELS = SIZE * SIZE;
    public static final int FALLBACK_COLOR = 0xFF14262A;
    public static final int LEGACY_VERSION = 1;
    public static final int CURRENT_VERSION = 3;

    public HoloMapTerrainTile {
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        sampledTime = Math.max(0L, sampledTime);
        version = Math.max(LEGACY_VERSION, version);
        detailMode = detailMode == null ? DetailMode.BIOME_FALLBACK : detailMode;
        pixels = normalize(pixels);
    }

    public HoloMapTerrainTile(String dimension, int chunkX, int chunkZ, long sampledTime, int[] pixels) {
        this(dimension, chunkX, chunkZ, sampledTime, LEGACY_VERSION, DetailMode.BIOME_FALLBACK, pixels);
    }

    public int pixel(int localX, int localZ) {
        int x = Math.max(0, Math.min(SIZE - 1, localX));
        int z = Math.max(0, Math.min(SIZE - 1, localZ));
        return pixels[z * SIZE + x];
    }

    public int averageColor() {
        long a = 0L;
        long r = 0L;
        long g = 0L;
        long b = 0L;
        for (int pixel : pixels) {
            a += (pixel >>> 24) & 0xFF;
            r += (pixel >>> 16) & 0xFF;
            g += (pixel >>> 8) & 0xFF;
            b += pixel & 0xFF;
        }
        int count = Math.max(1, pixels.length);
        return ((int) (a / count) << 24)
                | ((int) (r / count) << 16)
                | ((int) (g / count) << 8)
                | (int) (b / count);
    }

    public boolean legacy() {
        return version < CURRENT_VERSION || detailMode == DetailMode.BIOME_FALLBACK;
    }

    public boolean renderableSurface() {
        return version >= CURRENT_VERSION && detailMode != DetailMode.BIOME_FALLBACK;
    }

    public HoloMapTerrainTile copy() {
        return new HoloMapTerrainTile(dimension, chunkX, chunkZ, sampledTime, version, detailMode, pixels);
    }

    private static int[] normalize(int[] input) {
        int[] copy = new int[PIXELS];
        Arrays.fill(copy, FALLBACK_COLOR);
        if (input != null) {
            System.arraycopy(input, 0, copy, 0, Math.min(input.length, copy.length));
        }
        return copy;
    }

    public enum DetailMode {
        BIOME_FALLBACK("biome_fallback", "BIO"),
        SURFACE_BLOCK("surface_block", "BLOCK"),
        SURFACE_SHADED("surface_shaded", "SHADED");

        private final String serializedName;
        private final String label;

        DetailMode(String serializedName, String label) {
            this.serializedName = serializedName;
            this.label = label;
        }

        public String serializedName() {
            return serializedName;
        }

        public String label() {
            return label;
        }

        public static DetailMode byName(String name) {
            if (name != null) {
                for (DetailMode mode : values()) {
                    if (mode.serializedName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                        return mode;
                    }
                }
            }
            return BIOME_FALLBACK;
        }
    }
}
