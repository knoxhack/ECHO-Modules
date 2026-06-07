package com.knoxhack.echoholomap.map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class HoloMapTerrainPalette {
    private HoloMapTerrainPalette() {
    }

    public static int colorFor(ServerLevel level, BlockPos surfacePos) {
        if (level == null || surfacePos == null) {
            return HoloMapTerrainTile.FALLBACK_COLOR;
        }
        BlockState state = level.getBlockState(surfacePos);
        if (state.isAir() && surfacePos.getY() > level.getMinY()) {
            surfacePos = surfacePos.below();
            state = level.getBlockState(surfacePos);
        }
        Identifier biomeId = biomeId(level.getBiome(surfacePos));
        int base = baseColor(level.dimension(), biomeId, state);
        return shade(base, surfacePos.getY(), state);
    }

    public static SurfaceColor surfaceColorFor(ServerLevel level, BlockPos surfacePos, boolean shore) {
        if (level == null || surfacePos == null) {
            return new SurfaceColor(HoloMapTerrainTile.FALLBACK_COLOR, HoloMapTerrainTile.DetailMode.BIOME_FALLBACK);
        }
        BlockState state = level.getBlockState(surfacePos);
        if (state.isAir() && surfacePos.getY() > level.getMinY()) {
            surfacePos = surfacePos.below();
            state = level.getBlockState(surfacePos);
        }
        return surfaceColorFor(level, surfacePos, level.getBiome(surfacePos), state, shore);
    }

    static SurfaceColor surfaceColorFor(ServerLevel level, BlockPos surfacePos, Holder<Biome> biome,
            BlockState state, boolean shore) {
        if (level == null || surfacePos == null || state == null) {
            return new SurfaceColor(HoloMapTerrainTile.FALLBACK_COLOR, HoloMapTerrainTile.DetailMode.BIOME_FALLBACK);
        }
        Identifier biomeId = biomeId(biome);
        int biomeBase = baseColor(level.dimension(), biomeId, state);
        if (state.isAir()) {
            return new SurfaceColor(shade(biomeBase, surfacePos.getY(), false),
                    HoloMapTerrainTile.DetailMode.BIOME_FALLBACK);
        }
        int blockBase = blockMapColor(level, surfacePos, state, biomeBase);
        int blended = blend(blockBase, biomeBase, biomeBlendWeight(state));
        int highlighted = applySurfaceHighlights(blended, state, shore);
        int shaded = shade(highlighted, surfacePos.getY(), state);
        return new SurfaceColor(shaded, HoloMapTerrainTile.DetailMode.SURFACE_SHADED);
    }

    public static int colorForBiome(String dimension, String biomePath, int height, boolean water) {
        Identifier dimensionId = Identifier.tryParse(dimension == null ? "" : dimension);
        Identifier biomeId = Identifier.tryParse("minecraft:" + (biomePath == null ? "plains" : biomePath));
        int base = water ? 0xFF1E5E7E : baseColor(dimensionId, biomeId, null);
        return shade(base, height, water);
    }

    public static int colorForDescriptor(String dimension, String biomePath, int height, String blockPath,
            boolean water, boolean shore) {
        Identifier dimensionId = Identifier.tryParse(dimension == null ? "" : dimension);
        Identifier biomeId = Identifier.tryParse("minecraft:" + (biomePath == null ? "plains" : biomePath));
        int biomeBase = water ? 0xFF1E5E7E : baseColor(dimensionId, biomeId, null);
        int blockBase = blockDescriptorColor(blockPath, biomeBase);
        int blended = blend(blockBase, biomeBase, 0.18D);
        int highlighted = shore ? blend(blended, 0xFFCABF82, 0.32D) : blended;
        return shade(highlighted, height, water);
    }

    private static Identifier biomeId(Holder<Biome> biome) {
        return biome == null
                ? Identifier.withDefaultNamespace("plains")
                : biome.unwrapKey().map(key -> key.identifier()).orElse(Identifier.withDefaultNamespace("plains"));
    }

    private static int baseColor(net.minecraft.resources.ResourceKey<Level> dimension, Identifier biome, BlockState state) {
        Identifier dimensionId = dimension == null ? Level.OVERWORLD.identifier() : dimension.identifier();
        return baseColor(dimensionId, biome, state);
    }

    private static int baseColor(Identifier dimension, Identifier biome, BlockState state) {
        if (state != null) {
            if (state.is(Blocks.WATER) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                return 0xFF1C6285;
            }
            if (state.is(Blocks.LAVA)) {
                return 0xFFD65F28;
            }
            if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
                return 0xFFE6F7F4;
            }
            if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
                return state.is(Blocks.RED_SAND) ? 0xFFC67948 : 0xFFC8B979;
            }
        }
        String dim = dimension == null ? "" : dimension.toString();
        String path = biome == null ? "plains" : biome.getPath();
        if (dim.contains("the_nether") || path.contains("nether") || path.contains("crimson") || path.contains("warped")) {
            return path.contains("warped") ? 0xFF287A73 : 0xFF7F2D2D;
        }
        if (dim.contains("the_end") || path.contains("end")) {
            return 0xFF6E6687;
        }
        if (path.contains("ocean") || path.contains("river")) {
            return 0xFF1F6E93;
        }
        if (path.contains("beach")) {
            return 0xFFCABF82;
        }
        if (path.contains("snow") || path.contains("ice") || path.contains("frozen")) {
            return 0xFFD7ECEB;
        }
        if (path.contains("desert") || path.contains("badlands")) {
            return path.contains("badlands") ? 0xFFA86143 : 0xFFC7B66C;
        }
        if (path.contains("swamp") || path.contains("mangrove")) {
            return 0xFF4E7049;
        }
        if (path.contains("jungle")) {
            return 0xFF2F8B3C;
        }
        if (path.contains("forest") || path.contains("grove") || path.contains("taiga")) {
            return path.contains("taiga") ? 0xFF3E7661 : 0xFF3B8B4C;
        }
        if (path.contains("savanna")) {
            return 0xFF9EA65B;
        }
        if (path.contains("meadow") || path.contains("plains")) {
            return 0xFF73A950;
        }
        if (path.contains("peak") || path.contains("slope") || path.contains("mountain")) {
            return 0xFF7E8A81;
        }
        if (path.contains("cave") || path.contains("deep")) {
            return 0xFF405159;
        }
        return 0xFF5B8D4C;
    }

    private static int blockMapColor(ServerLevel level, BlockPos pos, BlockState state, int fallback) {
        int special = specialBlockColor(state);
        if (special != 0) {
            return special;
        }
        try {
            MapColor mapColor = state.getMapColor(level, pos);
            int color = mapColor == null ? 0 : mapColor.col;
            if (color != 0) {
                return 0xFF000000 | color;
            }
        } catch (RuntimeException exception) {
            return fallback;
        }
        return fallback;
    }

    private static int blockDescriptorColor(String blockPath, int fallback) {
        if (blockPath == null || blockPath.isBlank()) {
            return fallback;
        }
        String path = blockPath.toLowerCase(java.util.Locale.ROOT);
        if (path.contains("water") || path.contains("ice")) {
            return 0xFF1C6285;
        }
        if (path.contains("lava") || path.contains("magma")) {
            return 0xFFD65F28;
        }
        if (path.contains("snow")) {
            return 0xFFE6F7F4;
        }
        if (path.contains("sand")) {
            return path.contains("red") ? 0xFFC67948 : 0xFFC8B979;
        }
        if (path.contains("stone") || path.contains("deepslate") || path.contains("gravel")) {
            return 0xFF777E7D;
        }
        if (path.contains("grass") || path.contains("moss")) {
            return 0xFF68A94A;
        }
        if (path.contains("dirt") || path.contains("mud")) {
            return 0xFF78593B;
        }
        if (path.contains("log") || path.contains("wood") || path.contains("planks")) {
            return 0xFF866641;
        }
        if (path.contains("leaves")) {
            return 0xFF3C8D45;
        }
        return fallback;
    }

    private static int specialBlockColor(BlockState state) {
        if (state == null) {
            return 0;
        }
        if (state.is(Blocks.WATER) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return 0xFF1C6285;
        }
        if (state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
            return 0xFFD65F28;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            return 0xFFE6F7F4;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE)) {
            return 0xFFC8B979;
        }
        if (state.is(Blocks.RED_SAND) || state.is(Blocks.RED_SANDSTONE)) {
            return 0xFFC67948;
        }
        return 0;
    }

    private static double biomeBlendWeight(BlockState state) {
        if (state == null) {
            return 0.45D;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.MOSS_BLOCK)) {
            return 0.34D;
        }
        if (state.is(Blocks.WATER) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return 0.12D;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.LAVA)) {
            return 0.08D;
        }
        return 0.18D;
    }

    private static int applySurfaceHighlights(int color, BlockState state, boolean shore) {
        int highlighted = color;
        if (state != null) {
            if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                highlighted = blend(highlighted, 0xFFBDEEFF, 0.34D);
            } else if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
                highlighted = blend(highlighted, 0xFFFFFFFF, 0.42D);
            } else if (state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
                highlighted = blend(highlighted, 0xFFFF8A38, 0.44D);
            }
        }
        return shore ? blend(highlighted, 0xFFCABF82, 0.30D) : highlighted;
    }

    private static int shade(int color, int height, BlockState state) {
        boolean water = state != null && (state.is(Blocks.WATER) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE));
        return shade(color, height, water);
    }

    private static int shade(int color, int height, boolean water) {
        int adjustment = Math.max(-28, Math.min(34, (height - 64) / 5));
        if (water) {
            adjustment -= 8;
        }
        return adjust(color, adjustment);
    }

    private static int adjust(int color, int delta) {
        int a = (color >>> 24) & 0xFF;
        int r = clamp(((color >>> 16) & 0xFF) + delta);
        int g = clamp(((color >>> 8) & 0xFF) + delta);
        int b = clamp((color & 0xFF) + delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int blend(int color, int overlay, double weight) {
        double safeWeight = Math.max(0.0D, Math.min(1.0D, weight));
        double baseWeight = 1.0D - safeWeight;
        int a = clamp((int) Math.round(((color >>> 24) & 0xFF) * baseWeight + ((overlay >>> 24) & 0xFF) * safeWeight));
        int r = clamp((int) Math.round(((color >>> 16) & 0xFF) * baseWeight + ((overlay >>> 16) & 0xFF) * safeWeight));
        int g = clamp((int) Math.round(((color >>> 8) & 0xFF) * baseWeight + ((overlay >>> 8) & 0xFF) * safeWeight));
        int b = clamp((int) Math.round((color & 0xFF) * baseWeight + (overlay & 0xFF) * safeWeight));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public record SurfaceColor(int argb, HoloMapTerrainTile.DetailMode detailMode) {
        public SurfaceColor {
            detailMode = detailMode == null ? HoloMapTerrainTile.DetailMode.BIOME_FALLBACK : detailMode;
        }
    }
}
