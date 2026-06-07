package com.knoxhack.echoashfallprotocol.worldgen;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Legacy POI template spawner - PERMANENTLY DISABLED.
 *
 * Datapack jigsaw structure sets (structure_set/*.json) are the sole authority
 * for POI generation. This legacy spawner remains compiled for emergency debugging
 * only - it must stay disabled to prevent duplicate POI placement.
 * Runtime TOML structure settings only affect ProceduralStructureHandler.
 *
 * All POI spacing/separation is now controlled via datapack:
 * - poi_global.json (spacing 32, separation 12)
 * - poi_ruined_plains.json (spacing 20, separation 8)
 * - poi_crash_zone_wasteland.json (spacing 20, separation 8)
 * - landmark_pois.json (spacing 48, separation 18)
 * - major_pois.json (spacing 56, separation 22)
 */
public class POIStructureSpawner {
    private static final boolean ENABLE_LEGACY_EVENT_SPAWNER = false;

    // POI spawn configurations matching the Sparse Survival structure_set JSONs.
    private static final Map<String, SpawnConfig> POI_CONFIGS = new HashMap<>();

    static {
        // Biome-specific POIs - spacing, separation, salt
        POI_CONFIGS.put("crash_zone_wasteland", new SpawnConfig(WorldgenBalance.CRASH_POI_SPACING, WorldgenBalance.CRASH_POI_SEPARATION, 111111));
        POI_CONFIGS.put("ruined_cityscape", new SpawnConfig(WorldgenBalance.URBAN_POI_SPACING, WorldgenBalance.URBAN_POI_SEPARATION, 222222));
        POI_CONFIGS.put("radiation_zone", new SpawnConfig(WorldgenBalance.MICRO_POI_SPACING, WorldgenBalance.MICRO_POI_SEPARATION, 333333));
        POI_CONFIGS.put("toxic_swamp", new SpawnConfig(WorldgenBalance.MICRO_POI_SPACING, WorldgenBalance.MICRO_POI_SEPARATION, 444444));
        POI_CONFIGS.put("industrial_ruins", new SpawnConfig(WorldgenBalance.URBAN_POI_SPACING, WorldgenBalance.URBAN_POI_SEPARATION, 555555));
        POI_CONFIGS.put("cryogenic_ruins", new SpawnConfig(WorldgenBalance.MICRO_POI_SPACING, WorldgenBalance.MICRO_POI_SEPARATION, 666666));
        POI_CONFIGS.put("ruined_plains", new SpawnConfig(WorldgenBalance.CAMP_SPACING, WorldgenBalance.CAMP_SEPARATION, 777777));
        POI_CONFIGS.put("global", new SpawnConfig(WorldgenBalance.GLOBAL_POI_SPACING, WorldgenBalance.GLOBAL_POI_SEPARATION, 888888));
    }

    // Map biome names to their POI categories
    private static final Map<String, String> BIOME_TO_POI = new HashMap<>();

    static {
        BIOME_TO_POI.put("crash_zone_wasteland", "crash_zone_wasteland");
        BIOME_TO_POI.put("ruined_cityscape", "ruined_cityscape");
        BIOME_TO_POI.put("radiation_zone", "radiation_zone");
        BIOME_TO_POI.put("toxic_swamp", "toxic_swamp");
        BIOME_TO_POI.put("industrial_ruins", "industrial_ruins");
        BIOME_TO_POI.put("cryogenic_ruins", "cryogenic_ruins");
        BIOME_TO_POI.put("ruined_plains", "ruined_plains");
    }

    // Loot table mappings for each POI category
    private static final Map<String, ResourceKey<LootTable>> LOOT_TABLES = new HashMap<>();

    static {
        LOOT_TABLES.put("crash_zone_wasteland", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/crash_zone_wasteland_cache")));
        LOOT_TABLES.put("ruined_cityscape", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/data_center_cache")));
        LOOT_TABLES.put("radiation_zone", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/reactor_ruin_cache")));
        LOOT_TABLES.put("toxic_swamp", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/bio_lab_cache")));
        LOOT_TABLES.put("industrial_ruins", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/industrial_factory_cache")));
        LOOT_TABLES.put("cryogenic_ruins", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/cryogenic_ruins_cache")));
        LOOT_TABLES.put("ruined_plains", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/crashbreak_salvage_yard_cache")));
        LOOT_TABLES.put("global", ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "chests/survivor_cache")));
    }

    public static void onChunkLoad(Object event) {
        if (!ENABLE_LEGACY_EVENT_SPAWNER) {
            return;
        }
        EchoAshfallProtocol.LOGGER.debug("[POI] Legacy event spawner is disabled; native datapack structure sets own POI generation.");
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

    private static void checkAndSpawnPOI(int chunkX, int chunkZ, ServerLevel level,
                                         BlockPos center, String poiCategory) {
        SpawnConfig config = POI_CONFIGS.get(poiCategory);
        if (config == null) return;

        long seed = level.getSeed();
        int spacing = config.spacing;
        int separation = config.separation;
        int salt = config.salt;

        // Calculate region coordinates (same algorithm as vanilla structure sets)
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
            return;
        }
        
        EchoAshfallProtocol.LOGGER.info("[POI] Spawn chunk selected for category '{}' at [{}, {}]", poiCategory, chunkX, chunkZ);

        List<Identifier> templates = getTemplatesForCategory(poiCategory);
        EchoAshfallProtocol.LOGGER.debug("[POI] Category '{}' has {} templates available", poiCategory, templates.size());
        if (templates.isEmpty()) {
            EchoAshfallProtocol.LOGGER.warn("[POI] No templates found for category: {}", poiCategory);
            return;
        }

        Identifier templateId = templates.get(random.nextInt(templates.size()));
        StructureTemplateManager templateManager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = templateManager.get(templateId);
        if (templateOpt.isEmpty()) {
            EchoAshfallProtocol.LOGGER.warn("[POI] Failed to load template: {}", templateId);
            return;
        }

        StructureTemplate template = templateOpt.get();
        Vec3i templateSize = template.getSize();
        if (templateSize.getX() <= 0 || templateSize.getZ() <= 0) {
            EchoAshfallProtocol.LOGGER.warn("[POI] Template {} has invalid size {} and will be skipped", templateId, templateSize);
            return;
        }

        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        Vec3i rotatedSize = getRotatedSize(templateSize, rotation);

        BlockPos spawnPos = findValidSpawnPosition(level, center, rotatedSize);
        if (spawnPos == null) {
            EchoAshfallProtocol.LOGGER.debug("[POI] No valid terrain found for '{}' template {} at [{}, {}]", poiCategory, templateId, chunkX, chunkZ);
            return;
        }

        EchoAshfallProtocol.LOGGER.info("[POI] Valid spawn position found at {} for '{}' using template '{}'", spawnPos, poiCategory, templateId);
        placePOI(level, spawnPos, poiCategory, templateId, template, rotatedSize, rotation, random);
    }

    private static Vec3i getRotatedSize(Vec3i size, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new Vec3i(size.getZ(), size.getY(), size.getX());
            default -> size;
        };
    }

    private static BlockPos findValidSpawnPosition(ServerLevel level, BlockPos center, Vec3i footprintSize) {
        int searchRadius = 12;
        int footprintX = Math.max(3, footprintSize.getX());
        int footprintZ = Math.max(3, footprintSize.getZ());
        PlacementCandidate bestCandidate = null;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                BlockPos checkPos = center.offset(dx, 0, dz);
                int surfaceY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        checkPos.getX(), checkPos.getZ()
                );

                BlockPos surfacePos = checkPos.atY(surfaceY);
                PlacementCandidate candidate = scorePlacement(level, surfacePos, footprintX, footprintZ);
                if (candidate != null && (bestCandidate == null || candidate.score > bestCandidate.score)) {
                    bestCandidate = candidate;
                }
            }
        }

        return bestCandidate != null ? bestCandidate.centerPos : null;
    }

    private static PlacementCandidate scorePlacement(ServerLevel level, BlockPos center, int sizeX, int sizeZ) {
        if (center.getY() < -60) {
            return null;
        }
        BlockPos origin = center.offset(-sizeX / 2, 0, -sizeZ / 2);
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int solidCells = 0;
        int edgeDrops = 0;
        int totalCells = sizeX * sizeZ;

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                int worldX = origin.getX() + x;
                int worldZ = origin.getZ() + z;
                int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                BlockPos surfacePos = new BlockPos(worldX, surfaceY, worldZ);
                BlockPos groundPos = surfacePos.below();

                if (!level.getFluidState(surfacePos).isEmpty()) {
                    return null;
                }
                if (!level.getBlockState(groundPos).canOcclude()) {
                    return null;
                }

                minY = Math.min(minY, surfaceY);
                maxY = Math.max(maxY, surfaceY);
                solidCells++;

                if (x == 0 || z == 0 || x == sizeX - 1 || z == sizeZ - 1) {
                    int east = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX + 1, worldZ);
                    int west = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX - 1, worldZ);
                    int south = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX, worldZ + 1);
                    int north = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, worldX, worldZ - 1);
                    int localDrop = Math.max(
                            Math.max(Math.abs(surfaceY - east), Math.abs(surfaceY - west)),
                            Math.max(Math.abs(surfaceY - south), Math.abs(surfaceY - north))
                    );
                    if (localDrop > 3) {
                        edgeDrops++;
                    }
                }
            }
        }

        int heightSpread = maxY - minY;
        if (heightSpread > 3) {
            return null;
        }
        if (solidCells < Math.max(4, totalCells - 2)) {
            return null;
        }
        if (edgeDrops > Math.max(2, (sizeX + sizeZ) / 4)) {
            return null;
        }

        int score = 100 - (heightSpread * 18) - (edgeDrops * 12) + solidCells;
        BlockPos stabilizedCenter = new BlockPos(center.getX(), minY, center.getZ());
        return new PlacementCandidate(stabilizedCenter, score);
    }

    private static void placePOI(ServerLevel level, BlockPos pos, String category,
                                 Identifier templateId, StructureTemplate template, Vec3i size,
                                 Rotation rotation, java.util.Random random) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false)
                .setFinalizeEntities(true);

        // Calculate position (template spawns from corner, so center it)
        BlockPos placePos = pos.offset(-size.getX() / 2, 0, -size.getZ() / 2);

        // Place the structure
        try {
            template.placeInWorld(level, placePos, placePos, settings, RandomSource.create(random.nextLong()), 2);
            EchoAshfallProtocol.LOGGER.info("Placed POI {} at {} in biome {}",
                    templateId, placePos, category);
        } catch (Exception e) {
            EchoAshfallProtocol.LOGGER.error("Failed to place POI {}: {}", templateId, e.getMessage());
            return;
        }

        try {
            // Spawn loot chests in/around the structure
            spawnLootChests(level, placePos, size, category, random);
        } catch (Exception e) {
            EchoAshfallProtocol.LOGGER.error("Failed to populate POI {} after placement: {}", templateId, e.getMessage());
        }
    }

    private record PlacementCandidate(BlockPos centerPos, int score) {}

    private static List<Identifier> getTemplatesForCategory(String category) {
        List<Identifier> templates = new ArrayList<>();

        switch (category) {
            case "crash_zone_wasteland":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/crash_zone_wasteland/scrap_pile_small"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/crash_zone_wasteland/scrap_pile_medium"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/crash_zone_wasteland/wreckage_cluster"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/crash_zone_wasteland/ash_covered_ruin"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/crash_zone_wasteland/wreckage_command_post"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/crash_zone_wasteland/crashbreak_hut"));
                break;
            case "ruined_cityscape":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_cityscape/collapsed_building_small"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_cityscape/collapsed_building_tall"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_cityscape/street_barricade"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_cityscape/parking_ruin"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_cityscape/data_center_ruin"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_cityscape/subway_station"));
                break;
            case "radiation_zone":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/radiation_zone/containment_breach"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/radiation_zone/waste_barrel_cluster"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/radiation_zone/irradiated_vehicle"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/radiation_zone/radiation_crater"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/radiation_zone/contaminated_lab"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/radiation_zone/fallout_shelter"));
                break;
            case "toxic_swamp":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/toxic_swamp/chemical_spill"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/toxic_swamp/broken_pipeline"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/toxic_swamp/abandoned_shed"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/toxic_swamp/toxic_pool_small"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/toxic_swamp/spore_research_hut"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/toxic_swamp/stilted_outpost"));
                break;
            case "industrial_ruins":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/industrial_ruins/conveyor_ruin"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/industrial_ruins/storage_yard"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/industrial_ruins/crane_wreck"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/industrial_ruins/pipe_cluster"));
                break;
            case "cryogenic_ruins":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/cryogenic_ruins/frozen_vehicle"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/cryogenic_ruins/ice_covered_ruin"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/cryogenic_ruins/broken_tank"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/cryogenic_ruins/frozen_cache"));
                break;
            case "ruined_plains":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/nomad_camp"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/windmill_ruin"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/impact_crater"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/supply_drop"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/scavenger_camp"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/relay_tower"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biomes/ruined_plains/trader_post"));
                break;
            case "global":
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "global/debris_field_small"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "global/debris_field_large"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "global/survivor_cache"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "global/radio_relay_small"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "global/abandoned_camp"));
                templates.add(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "global/road_wreck"));
                break;
            default:
                EchoAshfallProtocol.LOGGER.warn("[POI] Unknown category: {}", category);
        }
        
        EchoAshfallProtocol.LOGGER.debug("[POI] getTemplatesForCategory('{}') returned {} templates", category, templates.size());
        return templates;
    }

    /**
     * Spawn loot chests in and around the placed structure.
     * Attempts to find valid positions on solid ground.
     */
    private static void spawnLootChests(ServerLevel level, BlockPos structurePos, Vec3i size,
                                        String category, java.util.Random random) {
        ResourceKey<LootTable> lootTable = LOOT_TABLES.get(category);
        if (lootTable == null) {
            EchoAshfallProtocol.LOGGER.warn("No loot table found for category: {}", category);
            return;
        }

        int sizeX = Math.max(1, size.getX());
        int sizeY = Math.max(1, size.getY());
        int sizeZ = Math.max(1, size.getZ());

        // Determine number of chests based on structure size
        int chestCount = Math.min(1 + random.nextInt(2), (sizeX * sizeZ) / 20);
        if (chestCount < 1) chestCount = 1;

        int placedChests = 0;
        int attempts = 0;
        int maxAttempts = chestCount * 10; // Try up to 10 times per chest

        while (placedChests < chestCount && attempts < maxAttempts) {
            attempts++;

            // Random position within structure bounds, biased toward edges/corners
            int x = random.nextInt(sizeX);
            int z = random.nextInt(sizeZ);

            // Bias toward edges (corners are more likely to have safe placement)
            if (random.nextFloat() < 0.6f) {
                // Pick an edge
                if (random.nextBoolean()) {
                    x = random.nextBoolean() ? Math.min(1, sizeX - 1) : Math.max(0, sizeX - 2);
                } else {
                    z = random.nextBoolean() ? Math.min(1, sizeZ - 1) : Math.max(0, sizeZ - 2);
                }
            }

            // Place chests at structure Y level (on top of the structure)
            // Try from top down to find a valid position
            for (int yOffset = Math.min(2, sizeY - 1); yOffset >= 0; yOffset--) {
                BlockPos chestPos = structurePos.offset(x, yOffset, z);

                // Validate position - must have solid ground below and air at chest position
                BlockPos groundPos = chestPos.below();
                if (!level.getBlockState(groundPos).canOcclude()) continue;
                if (!level.getBlockState(chestPos).isAir()) continue;
                if (!level.getFluidState(chestPos).isEmpty()) continue;

                // Place Echo cache
                BlockState cacheState = ModBlocks.STRUCTURE_CACHE.get().defaultBlockState();
                level.setBlock(chestPos, cacheState, 2);
                BlockEntity blockEntity = level.getBlockEntity(chestPos);

                if (blockEntity instanceof RandomizableContainerBlockEntity cache) {
                    cache.setLootTable(lootTable, random.nextLong());
                    cache.setChanged();
                    level.sendBlockUpdated(chestPos, cacheState, cacheState, 2);
                    placedChests++;
                    EchoAshfallProtocol.LOGGER.info("Placed loot cache at {} for {} with loot table {}", chestPos, category, lootTable);
                } else {
                    EchoAshfallProtocol.LOGGER.warn("Failed to create loot-capable cache block entity at {}", chestPos);
                }
                break; // Successfully placed or failed, move to next cache
            }
        }

        if (placedChests > 0) {
            EchoAshfallProtocol.LOGGER.info("Placed {} loot cache(s) for {} POI", placedChests, category);
        }
    }

    private record SpawnConfig(int spacing, int separation, int salt) {}
}
