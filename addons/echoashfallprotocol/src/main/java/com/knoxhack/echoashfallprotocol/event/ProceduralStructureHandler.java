package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.worldgen.ProceduralStructureGenerator;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Event handler for procedural structure generation during chunk loading.
 * Generates unique, randomized structures based on chunk position and biome.
 */
public class ProceduralStructureHandler {
    private static final int TERRAIN_SEARCH_RADIUS = 24;
    private static final int FOOTPRINT_MIN_X = -8;
    private static final int FOOTPRINT_MIN_Z = -12;
    private static final int FOOTPRINT_MAX_MARGIN = 8;
    private static final int FOOTPRINT_SAMPLE_STEP = 4;

    private static final Map<StructureType, Set<String>> VALID_BIOMES = Map.ofEntries(
            Map.entry(StructureType.BIO_LAB, Set.of("toxic_swamp", "ruined_plains")),
            Map.entry(StructureType.DATA_CENTER, Set.of("industrial_ruins", "ruined_cityscape")),
            Map.entry(StructureType.MILITARY_VAULT, Set.of("crash_zone_wasteland", "industrial_ruins", "radiation_zone")),
            Map.entry(StructureType.REACTOR_RUIN, Set.of("radiation_zone")),
            Map.entry(StructureType.SUBWAY_STATION, Set.of("ruined_cityscape", "industrial_ruins")),
            Map.entry(StructureType.SATELLITE_ARRAY, Set.of("crash_zone_wasteland", "radiation_zone")),
            Map.entry(StructureType.RADIO_TOWER, Set.of("ruined_plains", "crash_zone_wasteland", "radiation_zone")),
            Map.entry(StructureType.SEWER_JUNCTION, Set.of("toxic_swamp", "ruined_cityscape", "industrial_ruins")),
            Map.entry(StructureType.TRAIN_YARD, Set.of("industrial_ruins", "ruined_cityscape", "crash_zone_wasteland")),
            Map.entry(StructureType.RADWARDEN_OUTPOST, Set.of("ruined_plains", "crash_zone_wasteland", "radiation_zone")),
            Map.entry(StructureType.CRASHBREAK_SALVAGE_YARD, Set.of("ruined_plains", "crash_zone_wasteland", "ruined_cityscape")),
            Map.entry(StructureType.SPOREBOUND_SANCTUM, Set.of("toxic_swamp")),
            Map.entry(StructureType.CRYOGENIC_RUINS, Set.of("cryogenic_ruins")),
            Map.entry(StructureType.RELAY_STATION, Set.of("ruined_plains", "crash_zone_wasteland")),
            Map.entry(StructureType.DERELICT_WORKSHOP, Set.of("ruined_plains", "industrial_ruins", "crash_zone_wasteland")),
            Map.entry(StructureType.ABANDONED_MINE, Set.of("industrial_ruins", "ruined_cityscape", "crash_zone_wasteland")),
            Map.entry(StructureType.OBSERVATION_POST, Set.of("ruined_plains", "crash_zone_wasteland", "cryogenic_ruins"))
    );

    
    // Structure spawn configuration: spacing, separation, salt
    private static final Map<StructureType, SpawnConfig> SPAWN_CONFIGS = new HashMap<>();
    
    static {
        // Structure type -> spacing, separation, salt
        // Spacing: chunks between structure attempts
        // Separation: minimum chunks between structures
        // Salt: unique seed modifier per structure type

        // Core structures (v1.0)
        SPAWN_CONFIGS.put(StructureType.BIO_LAB, new SpawnConfig(28, 7, 210415002));
        SPAWN_CONFIGS.put(StructureType.DATA_CENTER, new SpawnConfig(36, 9, 210415003));
        SPAWN_CONFIGS.put(StructureType.MILITARY_VAULT, new SpawnConfig(56, 14, 210415004));
        SPAWN_CONFIGS.put(StructureType.REACTOR_RUIN, new SpawnConfig(72, 18, 210415005));

        // Infrastructure structures
        SPAWN_CONFIGS.put(StructureType.SUBWAY_STATION, new SpawnConfig(32, 8, 210415006));
        SPAWN_CONFIGS.put(StructureType.SEWER_JUNCTION, new SpawnConfig(26, 6, 210415007));
        SPAWN_CONFIGS.put(StructureType.TRAIN_YARD, new SpawnConfig(40, 10, 210415008));

        // Tech/Communication structures
        SPAWN_CONFIGS.put(StructureType.SATELLITE_ARRAY, new SpawnConfig(48, 12, 210415009));
        SPAWN_CONFIGS.put(StructureType.RADIO_TOWER, new SpawnConfig(44, 11, 210415010));
        SPAWN_CONFIGS.put(StructureType.RELAY_STATION, new SpawnConfig(48, 12, 210415011));
        SPAWN_CONFIGS.put(StructureType.OBSERVATION_POST, new SpawnConfig(60, 15, 210415012));

        // Exploration 1.1: faction hubs
        SPAWN_CONFIGS.put(StructureType.RADWARDEN_OUTPOST, new SpawnConfig(50, 12, 210415013));
        SPAWN_CONFIGS.put(StructureType.CRASHBREAK_SALVAGE_YARD, new SpawnConfig(42, 10, 210415014));
        SPAWN_CONFIGS.put(StructureType.SPOREBOUND_SANCTUM, new SpawnConfig(46, 11, 210415015));

        // Exploration 1.1: world POIs
        SPAWN_CONFIGS.put(StructureType.CRYOGENIC_RUINS, new SpawnConfig(54, 13, 210415016));
        SPAWN_CONFIGS.put(StructureType.DERELICT_WORKSHOP, new SpawnConfig(38, 9, 210415017));
        SPAWN_CONFIGS.put(StructureType.ABANDONED_MINE, new SpawnConfig(58, 14, 210415018));
    }
    
    /**
     * Called during chunk generation to potentially spawn procedural structures
     */
    public static void onChunkGenerate(Object event) {
        // Only run on server side during world generation
        if (!(eventValue(event, "getLevel") instanceof LevelAccessor eventLevel) || eventLevel.isClientSide()) return;
        if (!booleanValue(event, "isNewChunk")) return;
        
        if (!(eventLevel instanceof ServerLevel serverLevel)) return;
        
        if (!(eventValue(event, "getChunk") instanceof ChunkAccess chunk)) return;
        BlockPos chunkCenter = chunk.getPos().getWorldPosition().offset(8, 0, 8);
        
        // Get chunk coordinates from world position
        int chunkX = chunkCenter.getX() >> 4;
        int chunkZ = chunkCenter.getZ() >> 4;
        
        // Check each structure type for spawning
        for (StructureType type : StructureType.values()) {
            if (shouldSpawnStructure(chunkX, chunkZ, type, serverLevel, chunkCenter)) {
                BlockPos spawnPos = findTerrainSafeSpawnPosition(serverLevel, chunkCenter, type);
                if (spawnPos != null) {
                    generateStructure(serverLevel, spawnPos, type);
                    break; // Only spawn one structure per chunk
                }
                EchoAshfallProtocol.LOGGER.debug(
                        "Skipped procedural {} near chunk [{}, {}]: no terrain-safe footprint found",
                        type.getName(), chunkX, chunkZ);
            }
        }
    }
    
    /**
     * Determine if a structure should spawn in this chunk
     */
    private static boolean shouldSpawnStructure(int chunkX, int chunkZ, StructureType type,
                                                 ServerLevel level, BlockPos center) {
        SpawnConfig config = SPAWN_CONFIGS.get(type);
        if (type == StructureType.DROP_POD) return false;
        if (config == null) return false;
        if (!Config.isStructureEnabled(type)) return false;
        
        // Check spacing using salt-based randomization
        long seed = level.getSeed();
        int spacing = Config.getStructureSpacing(type);
        int separation = Config.getStructureSeparation(type);
        int salt = config.salt;
        
        // Calculate region coordinates
        int regionX = Math.floorDiv(chunkX, spacing);
        int regionZ = Math.floorDiv(chunkZ, spacing);
        
        // Create deterministic random for this region
        java.util.Random random = new java.util.Random(
            seed + (long) regionX * 341873128712L + (long) regionZ * 132897987541L + salt
        );
        
        // Pick a random chunk within the region
        int spawnChunkX = regionX * spacing + random.nextInt(spacing - separation);
        int spawnChunkZ = regionZ * spacing + random.nextInt(spacing - separation);
        
        // Check if this is the selected chunk
        if (chunkX != spawnChunkX || chunkZ != spawnChunkZ) {
            return false;
        }
        
        // Check biome compatibility
        return isValidBiome(level, center, type);
    }
    
    /**
     * Check if the biome at this position can spawn this structure type
     */
    private static boolean isValidBiome(ServerLevel level, BlockPos pos, StructureType type) {
        var biome = level.getBiome(pos);
        String biomePath = biome.unwrapKey()
                .map(Object::toString)
                .map(ProceduralStructureHandler::extractBiomePath)
                .orElse("");

        if (!Config.isBiomeContentEnabled(biomePath)) {
            return false;
        }

        if (ProceduralStructureGenerator.isProfileStructureForBiome(biomePath, type)) {
            return true;
        }

        Set<String> validBiomes = VALID_BIOMES.get(type);
        return validBiomes != null && validBiomes.contains(biomePath);
    }

    private static String extractBiomePath(String keyString) {
        int lastSlash = keyString.lastIndexOf('/');
        int lastBracket = keyString.lastIndexOf(']');
        if (lastSlash >= 0 && lastBracket > lastSlash) {
            return keyString.substring(lastSlash + 1, lastBracket);
        }
        int namespaceSep = keyString.indexOf(':');
        if (namespaceSep >= 0 && namespaceSep + 1 < keyString.length()) {
            return keyString.substring(namespaceSep + 1);
        }
        return keyString;
    }
    
    /**
     * Find a valid origin-corner surface position for natural structure spawning.
     */
    public static BlockPos findTerrainSafeSpawnPosition(ServerLevel level, BlockPos center, StructureType type) {
        PlacementCandidate best = null;

        for (int dx = -TERRAIN_SEARCH_RADIUS; dx <= TERRAIN_SEARCH_RADIUS; dx++) {
            for (int dz = -TERRAIN_SEARCH_RADIUS; dz <= TERRAIN_SEARCH_RADIUS; dz++) {
                BlockPos origin = center.offset(dx, 0, dz);
                PlacementCandidate candidate = evaluateTerrainFootprint(level, origin, type, center);
                if (candidate != null && (best == null || candidate.score() > best.score())) {
                    best = candidate;
                }
            }
        }

        return best == null ? null : best.origin();
    }

    public static boolean hasTerrainSafeFootprint(ServerLevel level, BlockPos origin, StructureType type) {
        return evaluateTerrainFootprint(level, origin, type, origin) != null;
    }

    private static PlacementCandidate evaluateTerrainFootprint(ServerLevel level, BlockPos origin,
                                                               StructureType type, BlockPos target) {
        FootprintBounds bounds = footprintBounds(type);
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int sharpEdgeDrops = 0;
        int worstLocalDrop = 0;
        int sampledCells = 0;
        int edgeSamples = 0;
        int heightSpreadLimit = maxHeightSpread(type);
        int edgeDropLimit = heightSpreadLimit <= 4 ? 3 : 4;

        for (int localX = bounds.minX(); localX <= bounds.maxX(); localX += FOOTPRINT_SAMPLE_STEP) {
            for (int localZ = bounds.minZ(); localZ <= bounds.maxZ(); localZ += FOOTPRINT_SAMPLE_STEP) {
                int worldX = origin.getX() + localX;
                int worldZ = origin.getZ() + localZ;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                BlockPos surface = new BlockPos(worldX, surfaceY, worldZ);
                BlockPos ground = surface.below();

                if (!isValidSurface(level, surface, type)) {
                    return null;
                }

                minY = Math.min(minY, surfaceY);
                maxY = Math.max(maxY, surfaceY);
                sampledCells++;

                int localDrop = localHeightDrop(level, surfaceY, worldX, worldZ);
                worstLocalDrop = Math.max(worstLocalDrop, localDrop);
                if (localDrop > edgeDropLimit + 2) {
                    return null;
                }

                if (isFootprintEdge(localX, localZ, bounds)) {
                    edgeSamples++;
                    if (localDrop > edgeDropLimit) {
                        sharpEdgeDrops++;
                    }
                }

                BlockState groundState = level.getBlockState(ground);
                if (!groundState.canOcclude()) {
                    return null;
                }
            }
        }

        if (sampledCells == 0) {
            return null;
        }
        int heightSpread = maxY - minY;
        if (heightSpread > heightSpreadLimit) {
            return null;
        }
        if (sharpEdgeDrops > Math.max(2, edgeSamples / 10)) {
            return null;
        }

        int distance = (int) Math.sqrt(origin.distSqr(target));
        int score = 2000 - distance - heightSpread * 100 - sharpEdgeDrops * 80 - worstLocalDrop * 30;
        return new PlacementCandidate(origin.atY(maxY), score);
    }

    private static boolean isValidSurface(ServerLevel level, BlockPos pos, StructureType type) {
        if (pos.getY() < level.getMinY() + 5) return false;
        if (pos.getY() > level.getMaxY() - 20) return false;
        if (!level.getFluidState(pos).isEmpty()) return false;

        return switch (type) {
            case MILITARY_VAULT -> pos.getY() > 60;
            default -> true;
        };
    }

    private static int localHeightDrop(ServerLevel level, int surfaceY, int worldX, int worldZ) {
        int east = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX + 1, worldZ);
        int west = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX - 1, worldZ);
        int south = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ + 1);
        int north = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ - 1);
        return Math.max(
                Math.max(Math.abs(surfaceY - east), Math.abs(surfaceY - west)),
                Math.max(Math.abs(surfaceY - south), Math.abs(surfaceY - north))
        );
    }

    private static boolean isFootprintEdge(int localX, int localZ, FootprintBounds bounds) {
        return localX == bounds.minX()
                || localZ == bounds.minZ()
                || localX + FOOTPRINT_SAMPLE_STEP > bounds.maxX()
                || localZ + FOOTPRINT_SAMPLE_STEP > bounds.maxZ();
    }

    private static int maxHeightSpread(StructureType type) {
        return switch (type) {
            case RADIO_TOWER, RELAY_STATION, OBSERVATION_POST, SATELLITE_ARRAY -> 6;
            default -> 4;
        };
    }

    private static FootprintBounds footprintBounds(StructureType type) {
        int max = type.getMaxSize() + FOOTPRINT_MAX_MARGIN;
        return new FootprintBounds(FOOTPRINT_MIN_X, FOOTPRINT_MIN_Z, max, max);
    }
    
    /**
     * Generate the structure at the given position
     */
    private static void generateStructure(ServerLevel level, BlockPos pos, StructureType type) {
        try {
            // Use the world's random source for consistency
            net.minecraft.util.RandomSource random = level.getRandom();
            
            // Log generation
            EchoAshfallProtocol.LOGGER.info("Generating procedural {} at {}", 
                    type.getName(), pos);
            
            // Generate the structure using LevelAccessor
            ProceduralStructureGenerator.generateStructure(level, pos, type, random);
            
        } catch (Exception e) {
            EchoAshfallProtocol.LOGGER.error("Failed to generate procedural structure {} at {}: {}",
                    type.getName(), pos, e.getMessage());
        }
    }
    
    /**
     * Configuration for structure spawning
     */
    private record SpawnConfig(int spacing, int separation, int salt) {}

    private record FootprintBounds(int minX, int minZ, int maxX, int maxZ) {}

    private record PlacementCandidate(BlockPos origin, int score) {}

    private static boolean booleanValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof Boolean result && result;
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
