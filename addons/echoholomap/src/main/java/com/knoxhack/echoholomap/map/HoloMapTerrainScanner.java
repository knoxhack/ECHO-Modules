package com.knoxhack.echoholomap.map;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.integration.HoloMapMissionHooks;
import com.knoxhack.echoholomap.integration.runtimeguard.HoloMapRuntimeGuardHooks;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class HoloMapTerrainScanner {
    private static final Map<UUID, Integer> SCAN_CURSORS = new HashMap<>();
    private static final Map<UUID, Integer> REQUEST_SCAN_CURSORS = new HashMap<>();
    private static final Map<UUID, ScanState> LAST_SCAN_STATES = new HashMap<>();

    private HoloMapTerrainScanner() {
    }

    public static void onPlayerTick(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
        if (player == null) {
            return;
        }
        int interval = HoloMapRuntimeGuardHooks.refreshIntervalTicks(scanIntervalTicks());
        long now = player.level().getGameTime();
        if (interval <= 0 || Math.floorMod(now + playerScanStagger(player, interval), interval) != 0L) {
            return;
        }
        int centerChunkX = Math.floorDiv(player.blockPosition().getX(), HoloMapTerrainTile.SIZE);
        int centerChunkZ = Math.floorDiv(player.blockPosition().getZ(), HoloMapTerrainTile.SIZE);
        String dimension = player.level().dimension().identifier().toString();
        ScanState previous = LAST_SCAN_STATES.get(player.getUUID());
        if (previous != null && previous.matches(dimension, centerChunkX, centerChunkZ)
                && previous.complete()
                && now - previous.scanTick() < stationaryRescanTicks(interval)) {
            return;
        }
        int sampled = scanAround(player, scanRadiusChunks(), maxSampleChunksPerTick());
        LAST_SCAN_STATES.put(player.getUUID(), new ScanState(dimension, centerChunkX, centerChunkZ, now, sampled == 0));
    }

    public static int scanAround(ServerPlayer player, int radius, int maxChunks) {
        return scanAround(player, radius, maxChunks, false);
    }

    public static int scanAround(ServerPlayer player, int radius, int maxChunks, boolean forceResample) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        int safeRadius = Math.max(0, Math.min(32, radius));
        int safeMax = Math.max(1, Math.min(256, maxChunks));
        int diameter = safeRadius * 2 + 1;
        int total = diameter * diameter;
        int cursor = SCAN_CURSORS.getOrDefault(player.getUUID(), 0);
        int centerChunkX = Math.floorDiv(player.blockPosition().getX(), HoloMapTerrainTile.SIZE);
        int centerChunkZ = Math.floorDiv(player.blockPosition().getZ(), HoloMapTerrainTile.SIZE);
        long now = level.getGameTime();
        long resampleInterval = terrainResampleInterval();
        HoloMapTerrainSavedData data = HoloMapTerrainSavedData.get(level);
        int sampled = 0;
        int visited = 0;
        while (visited < total && sampled < safeMax) {
            int index = Math.floorMod(cursor + visited, total);
            int offsetX = index % diameter - safeRadius;
            int offsetZ = index / diameter - safeRadius;
            int chunkX = centerChunkX + offsetX;
            int chunkZ = centerChunkZ + offsetZ;
            visited++;
            LevelChunk chunk = loadedChunk(level, chunkX, chunkZ);
            if (chunk == null) {
                continue;
            }
            if (!forceResample
                    && !data.needsSample(player.getUUID(), level.dimension(), chunkX, chunkZ, now, resampleInterval)) {
                continue;
            }
            HoloMapTerrainTile tile = sampleChunk(level, chunk, chunkX, chunkZ, now);
            if (tile != null && tile.renderableSurface()
                    && data.saveTile(player.getUUID(), level.dimension(), chunkX, chunkZ, tile)) {
                sampled++;
            }
        }
        SCAN_CURSORS.put(player.getUUID(), Math.floorMod(cursor + visited, Math.max(1, total)));
        if (sampled > 0) {
            HoloMapMissionHooks.recordTerrainDiscovered(player, sampled);
        }
        return sampled;
    }

    public static int scanRequestedViewport(ServerPlayer player, String dimension,
            int centerChunkX, int centerChunkZ, int radius) {
        return scanRequestedViewport(player, dimension, centerChunkX, centerChunkZ, radius,
                requestSampleChunks());
    }

    public static int scanRequestedViewport(ServerPlayer player, String dimension,
            int centerChunkX, int centerChunkZ, int radius, int maxChunks) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        String currentDimension = level.dimension().identifier().toString();
        if (dimension == null || !currentDimension.equals(dimension.strip())) {
            return 0;
        }
        int safeRadius = Math.max(0, Math.min(32, radius));
        int safeMax = Math.max(0, Math.min(128, maxChunks));
        if (safeMax <= 0) {
            return 0;
        }
        int diameter = safeRadius * 2 + 1;
        int total = diameter * diameter;
        int maxVisited = Math.min(total, Math.max(96, safeMax * 24));
        int cursor = REQUEST_SCAN_CURSORS.getOrDefault(player.getUUID(), 0);
        long now = level.getGameTime();
        long resampleInterval = terrainResampleInterval();
        HoloMapTerrainSavedData data = HoloMapTerrainSavedData.get(level);
        int sampled = 0;
        int visited = 0;
        while (visited < maxVisited && sampled < safeMax) {
            int index = Math.floorMod(cursor + visited, total);
            int offsetX = index % diameter - safeRadius;
            int offsetZ = index / diameter - safeRadius;
            int chunkX = centerChunkX + offsetX;
            int chunkZ = centerChunkZ + offsetZ;
            visited++;
            LevelChunk chunk = loadedChunk(level, chunkX, chunkZ);
            if (chunk == null
                    || !data.needsSample(player.getUUID(), level.dimension(), chunkX, chunkZ, now, resampleInterval)) {
                continue;
            }
            HoloMapTerrainTile tile = sampleChunk(level, chunk, chunkX, chunkZ, now);
            if (tile != null && tile.renderableSurface()
                    && data.saveTile(player.getUUID(), level.dimension(), chunkX, chunkZ, tile)) {
                sampled++;
            }
        }
        REQUEST_SCAN_CURSORS.put(player.getUUID(), Math.floorMod(cursor + visited, Math.max(1, total)));
        if (sampled > 0) {
            HoloMapMissionHooks.recordTerrainDiscovered(player, sampled);
        }
        return sampled;
    }

    public static HoloMapTerrainTile sampleChunk(ServerLevel level, int chunkX, int chunkZ, long sampledTime) {
        LevelChunk chunk = loadedChunk(level, chunkX, chunkZ);
        return chunk == null ? null : sampleChunk(level, chunk, chunkX, chunkZ, sampledTime);
    }

    private static HoloMapTerrainTile sampleChunk(ServerLevel level, LevelChunk chunk, int chunkX, int chunkZ,
            long sampledTime) {
        int[] pixels = new int[HoloMapTerrainTile.PIXELS];
        int baseX = chunkX * HoloMapTerrainTile.SIZE;
        int baseZ = chunkZ * HoloMapTerrainTile.SIZE;
        int fallbackPixels = 0;
        int surfacePixels = 0;
        for (int localZ = 0; localZ < HoloMapTerrainTile.SIZE; localZ++) {
            for (int localX = 0; localX < HoloMapTerrainTile.SIZE; localX++) {
                int worldX = baseX + localX;
                int worldZ = baseZ + localZ;
                SurfaceSample surface = findSurface(level, chunk, worldX, worldZ);
                HoloMapTerrainPalette.SurfaceColor color = surface.fallback()
                        ? new HoloMapTerrainPalette.SurfaceColor(
                                HoloMapTerrainTile.FALLBACK_COLOR,
                                HoloMapTerrainTile.DetailMode.BIOME_FALLBACK)
                        : HoloMapTerrainPalette.surfaceColorFor(level, surface.pos(), surface.biome(),
                                surface.state(), surface.shore());
                if (color.detailMode() == HoloMapTerrainTile.DetailMode.BIOME_FALLBACK) {
                    fallbackPixels++;
                } else {
                    surfacePixels++;
                }
                pixels[localZ * HoloMapTerrainTile.SIZE + localX] = color.argb();
            }
        }
        HoloMapTerrainTile.DetailMode detailMode = surfacePixels == 0
                ? HoloMapTerrainTile.DetailMode.BIOME_FALLBACK
                : fallbackPixels > 0
                        ? HoloMapTerrainTile.DetailMode.SURFACE_BLOCK
                        : HoloMapTerrainTile.DetailMode.SURFACE_SHADED;
        return new HoloMapTerrainTile(level.dimension().identifier().toString(), chunkX, chunkZ,
                sampledTime, HoloMapTerrainTile.CURRENT_VERSION, detailMode, pixels);
    }

    public static void clearForTests() {
        SCAN_CURSORS.clear();
        REQUEST_SCAN_CURSORS.clear();
        LAST_SCAN_STATES.clear();
    }

    private static int scanIntervalTicks() {
        try {
            return Math.max(5, Config.TERRAIN_SCAN_INTERVAL.get());
        } catch (RuntimeException exception) {
            return 100;
        }
    }

    private static int scanRadiusChunks() {
        try {
            return Math.max(0, Config.TERRAIN_SCAN_RADIUS.get());
        } catch (RuntimeException exception) {
            return 5;
        }
    }

    private static int maxSampleChunksPerTick() {
        try {
            return Math.max(1, Config.TERRAIN_MAX_SAMPLE_CHUNKS_PER_TICK.get());
        } catch (RuntimeException exception) {
            return 8;
        }
    }

    private static int requestSampleChunks() {
        try {
            return Math.max(0, Config.TERRAIN_REQUEST_SAMPLE_CHUNKS.get());
        } catch (RuntimeException exception) {
            return 24;
        }
    }

    private static long playerScanStagger(ServerPlayer player, int interval) {
        UUID id = player.getUUID();
        return Math.floorMod(id.getMostSignificantBits() ^ id.getLeastSignificantBits(), Math.max(1, interval));
    }

    private static long stationaryRescanTicks(int interval) {
        long resample = terrainResampleInterval();
        return resample <= 0L ? Math.max(20L, interval) : Math.max(interval, resample);
    }

    private static long terrainResampleInterval() {
        try {
            return Math.max(0, Config.TERRAIN_RESAMPLE_INTERVAL.get());
        } catch (RuntimeException exception) {
            return 2400L;
        }
    }

    private static SurfaceSample findSurface(ServerLevel level, LevelChunk chunk, int worldX, int worldZ) {
        int topY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
        int minY = level.getMinY();
        int safeTop = Math.max(minY, topY);
        if (isCeilingFallback(level, safeTop)) {
            return fallbackSample(worldX, safeTop, worldZ);
        }
        int floor = Math.max(minY, safeTop - 48);
        for (int y = safeTop; y >= floor; y--) {
            BlockPos pos = new BlockPos(worldX, y, worldZ);
            BlockState state = chunk.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.is(Blocks.BEDROCK) && y >= level.getMaxY() - 8) {
                continue;
            }
            boolean water = isWater(chunk, pos, state);
            boolean shore = water && hasAdjacentLand(level, pos);
            Holder<Biome> biome = chunk.getNoiseBiome(
                    QuartPos.fromBlock(pos.getX()),
                    QuartPos.fromBlock(pos.getY()),
                    QuartPos.fromBlock(pos.getZ()));
            return new SurfaceSample(pos, state, biome, false, water, shore, y);
        }
        return fallbackSample(worldX, safeTop, worldZ);
    }

    private static boolean isCeilingFallback(ServerLevel level, int topY) {
        return level.dimension().equals(Level.NETHER) && topY >= level.getMaxY() - 16;
    }

    private static SurfaceSample fallbackSample(int worldX, int y, int worldZ) {
        return new SurfaceSample(new BlockPos(worldX, y, worldZ), Blocks.AIR.defaultBlockState(), null,
                true, false, false, y);
    }

    private static boolean isWater(LevelChunk chunk, BlockPos pos, BlockState state) {
        return state.is(Blocks.WATER) || chunk.getFluidState(pos).is(FluidTags.WATER);
    }

    private static boolean hasAdjacentLand(ServerLevel level, BlockPos pos) {
        return isLand(level, pos.north()) || isLand(level, pos.south())
                || isLand(level, pos.east()) || isLand(level, pos.west());
    }

    private static boolean isLand(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = loadedChunkForBlock(level, pos);
        if (chunk == null) {
            return false;
        }
        BlockState state = chunk.getBlockState(pos);
        return !state.isAir() && !isWater(chunk, pos, state);
    }

    private static LevelChunk loadedChunkForBlock(ServerLevel level, BlockPos pos) {
        return loadedChunk(level, Math.floorDiv(pos.getX(), HoloMapTerrainTile.SIZE),
                Math.floorDiv(pos.getZ(), HoloMapTerrainTile.SIZE));
    }

    private static LevelChunk loadedChunk(ServerLevel level, int chunkX, int chunkZ) {
        return level == null ? null : level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    private record SurfaceSample(BlockPos pos, BlockState state, Holder<Biome> biome, boolean fallback,
            boolean water, boolean shore, int height) {
    }

    private record ScanState(String dimension, int chunkX, int chunkZ, long scanTick, boolean complete) {
        private boolean matches(String dimension, int chunkX, int chunkZ) {
            return this.dimension.equals(dimension) && this.chunkX == chunkX && this.chunkZ == chunkZ;
        }
    }
}
