package com.knoxhack.echoashfallprotocol.worldgen;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.gameplay.AshfallInteractionRules;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.BiomeGuardianSiteData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Main generator for procedural structures using BSP tree algorithm.
 * Creates unique, randomized layouts for each structure instance.
 */
public class ProceduralStructureGenerator {
    
    private static final int MIN_ROOM_SIZE = 5;
    private static final int MAX_SPLIT_DEPTH = 4;
    private static final int WALL_THICKNESS = 1;
    private static final int STARTING_POD_SPAWN_X = 8;
    private static final int STARTING_POD_SPAWN_Y = 3;
    private static final int STARTING_POD_SPAWN_Z = 10;
    private static final int STARTING_POD_CLEAR_MARGIN_XZ = 2;
    private static final int STARTING_POD_CLEAR_MARGIN_Y = 2;
    private static final int GUARDIAN_MIN_BOSS_ROOM_SIZE = 17;
    private static final int GUARDIAN_BOSS_ROOM_HEIGHT = 6;
    private static final int GUARDIAN_SPAWN_RESERVE_RADIUS = 2;
    private static final int GUARDIAN_SPAWN_CLEARANCE = 5;
    
    // Loot table mappings for room types
    private static final Map<Room.RoomType, ResourceKey<LootTable>> ROOM_LOOT_TABLES = new HashMap<>();
    private static final Map<StructureType, ResourceKey<LootTable>> STRUCTURE_LOOT_TABLES = new HashMap<>();
    
    // Mob spawner danger levels for room types
    private static final Map<Room.RoomType, Float> ROOM_DANGER_LEVELS = new HashMap<>();
    private static final Map<String, BiomeStructureProfile> BIOME_PROFILES = Map.of(
            "crash_zone_wasteland", new BiomeStructureProfile(StructureType.MILITARY_VAULT,
                    Set.of(StructureType.DROP_POD, StructureType.TRAIN_YARD, StructureType.SATELLITE_ARRAY),
                    ModEntities.CRASH_ZONE_COLOSSUS::get),
            "cryogenic_ruins", new BiomeStructureProfile(StructureType.CRYOGENIC_RUINS,
                    Set.of(StructureType.OBSERVATION_POST, StructureType.RELAY_STATION),
                    ModEntities.CRYOGENIC_OVERSEER::get),
            "industrial_ruins", new BiomeStructureProfile(StructureType.DATA_CENTER,
                    Set.of(StructureType.TRAIN_YARD, StructureType.ABANDONED_MINE, StructureType.DERELICT_WORKSHOP, StructureType.SUBWAY_STATION),
                    ModEntities.INDUSTRIAL_JUGGERNAUT::get),
            "nexus_scar", new BiomeStructureProfile(StructureType.REACTOR_RUIN,
                    Set.of(StructureType.BIO_LAB, StructureType.MILITARY_VAULT, StructureType.SATELLITE_ARRAY),
                    ModEntities.NEXUS_SCAR_AVATAR::get),
            "radiation_zone", new BiomeStructureProfile(StructureType.REACTOR_RUIN,
                    Set.of(StructureType.MILITARY_VAULT, StructureType.RADIO_TOWER, StructureType.SATELLITE_ARRAY),
                    ModEntities.RADIATION_BEHEMOTH::get),
            "ruined_cityscape", new BiomeStructureProfile(StructureType.DATA_CENTER,
                    Set.of(StructureType.SUBWAY_STATION, StructureType.SEWER_JUNCTION, StructureType.TRAIN_YARD, StructureType.ABANDONED_MINE),
                    ModEntities.CITY_RUIN_STALKER::get),
            "ruined_plains", new BiomeStructureProfile(StructureType.RADWARDEN_OUTPOST,
                    Set.of(StructureType.BIO_LAB, StructureType.RADIO_TOWER, StructureType.CRASHBREAK_SALVAGE_YARD, StructureType.DROP_POD),
                    ModEntities.PLAINS_WARLORD::get),
            "toxic_swamp", new BiomeStructureProfile(StructureType.SPOREBOUND_SANCTUM,
                    Set.of(StructureType.BIO_LAB, StructureType.SEWER_JUNCTION, StructureType.REACTOR_RUIN),
                    ModEntities.TOXIC_HIVE_MATRIARCH::get)
    );
    private static final Map<String, GuardianSiteTheme> GUARDIAN_THEMES = Map.ofEntries(
            Map.entry("plains_warlord", new GuardianSiteTheme(
                    GuardianEntranceForm.BUNKER_DOOR, GuardianDescentForm.STAIR_TUNNEL, 6, 0.30f,
                    ModBlocks.CONCRETE_RUBBLE::get, ModBlocks.RUSTED_METAL_SHEET::get,
                    ModBlocks.ASH_STONE::get, ModBlocks.SIGNAL_SCANNER::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.WEAPON_RACK::get, ModBlocks.SUPPLY_CRATE::get)),
            Map.entry("city_ruin_stalker", new GuardianSiteTheme(
                    GuardianEntranceForm.SUBWAY_STAIR, GuardianDescentForm.STAIR_TUNNEL, 6, 0.34f,
                    ModBlocks.CRACKED_ASPHALT::get, ModBlocks.CONCRETE_RUBBLE::get,
                    ModBlocks.RIFTSTONE::get, ModBlocks.CABLE_BUNDLE::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.SIGNAL_SCANNER::get, ModBlocks.REBAR_BLOCK::get)),
            Map.entry("industrial_juggernaut", new GuardianSiteTheme(
                    GuardianEntranceForm.FREIGHT_LIFT, GuardianDescentForm.LIFT_SHAFT, 6, 0.38f,
                    ModBlocks.OIL_STAINED_CONCRETE::get, ModBlocks.RUSTED_METAL_SHEET::get,
                    ModBlocks.INDUSTRIAL_AGGREGATE::get, ModBlocks.CORRODED_PIPE::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.FACTORY_CONTROLLER::get, ModBlocks.REBAR_BLOCK::get)),
            Map.entry("toxic_hive_matriarch", new GuardianSiteTheme(
                    GuardianEntranceForm.TOXIC_SINKHOLE, GuardianDescentForm.SINKHOLE, 7, 0.48f,
                    ModBlocks.TOXIC_SLAGSTONE::get, ModBlocks.TOXIC_MOSS::get,
                    ModBlocks.TOXIC_MOSS::get, ModBlocks.ACIDIC_SLUDGE::get, ModBlocks.OOZE_CRYSTAL::get,
                    ModBlocks.CONTAMINANT_CONDENSER::get, ModBlocks.TOXIC_WASTE_BARREL::get)),
            Map.entry("crash_zone_colossus", new GuardianSiteTheme(
                    GuardianEntranceForm.IMPACT_BREACH, GuardianDescentForm.BREACH_SHAFT, 7, 0.46f,
                    ModBlocks.CRASH_SLAG::get, ModBlocks.RUSTED_METAL_SHEET::get,
                    ModBlocks.DROP_POD_HULL::get, ModBlocks.RUSTED_METAL_DEBRIS::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.POWER_NODE::get, ModBlocks.DROP_POD_GLASS::get)),
            Map.entry("radiation_behemoth", new GuardianSiteTheme(
                    GuardianEntranceForm.REACTOR_HATCH, GuardianDescentForm.LADDER_SHAFT, 6, 0.36f,
                    ModBlocks.IRRADIATED_SHALE::get, ModBlocks.RIFTSTONE::get,
                    ModBlocks.IRRADIATED_CRUST::get, ModBlocks.RADIATION_BLOCK::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.ISOTOPE_REFINER::get, ModBlocks.REBAR_BLOCK::get)),
            Map.entry("cryogenic_overseer", new GuardianSiteTheme(
                    GuardianEntranceForm.FROZEN_SHAFT, GuardianDescentForm.LIFT_SHAFT, 6, 0.40f,
                    ModBlocks.CRYOGENIC_FRACTURED_STONE::get, ModBlocks.BLUE_ICE_CRYSTAL::get,
                    ModBlocks.FROZEN_CONDUIT::get, ModBlocks.PERMAFROST::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.ATMOSPHERIC_SCRUBBER::get, ModBlocks.BLUE_ICE_CRYSTAL::get)),
            Map.entry("nexus_scar_avatar", new GuardianSiteTheme(
                    GuardianEntranceForm.NEXUS_BREACH, GuardianDescentForm.BREACH_SHAFT, 7, 0.44f,
                    ModBlocks.NEXUS_CRACKED_SOIL::get, ModBlocks.RIFTSTONE::get,
                    ModBlocks.ENERGIZED_FISSURE::get, ModBlocks.ECHO_CRYSTAL::get, ModBlocks.POWER_NODE::get,
                    ModBlocks.NEXUS_CORE::get, ModBlocks.CRYSTALLINE_SYNTHESIZER::get))
    );
    
    static {
        // Room-specific loot tables
        ROOM_LOOT_TABLES.put(Room.RoomType.LABORATORY, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/bio_lab_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.MEDBAY, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/bio_lab_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.SERVER_ROOM, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/data_center_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.CONTROL_ROOM, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/data_center_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.ARMORY, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/military_vault_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.BARRACKS, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/military_vault_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.REACTOR_CORE, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/reactor_ruin_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.CONTAINMENT, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/reactor_ruin_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.STORAGE, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/industrial_factory_cache")));
        ROOM_LOOT_TABLES.put(Room.RoomType.ENTRANCE, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/crashed_satellite_cache")));
        
        // Structure fallback loot tables
        STRUCTURE_LOOT_TABLES.put(StructureType.BIO_LAB, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/bio_lab_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.DATA_CENTER, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/data_center_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.MILITARY_VAULT, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/military_vault_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.REACTOR_RUIN, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/reactor_ruin_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.DROP_POD, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/crashed_satellite_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.SUBWAY_STATION, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/subway_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.SATELLITE_ARRAY, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/satellite_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.RADIO_TOWER, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/radio_tower_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.SEWER_JUNCTION, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/sewer_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.TRAIN_YARD, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/train_yard_cache")));

        // Exploration 1.1: faction hubs
        STRUCTURE_LOOT_TABLES.put(StructureType.RADWARDEN_OUTPOST, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/radwarden_outpost_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.CRASHBREAK_SALVAGE_YARD, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/crashbreak_salvage_yard_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.SPOREBOUND_SANCTUM, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/sporebound_sanctum_cache")));

        // Exploration 1.1: world POIs
        STRUCTURE_LOOT_TABLES.put(StructureType.CRYOGENIC_RUINS, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/cryogenic_ruins_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.RELAY_STATION, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/relay_station_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.DERELICT_WORKSHOP, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/derelict_workshop_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.ABANDONED_MINE, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/abandoned_mine_cache")));
        STRUCTURE_LOOT_TABLES.put(StructureType.OBSERVATION_POST, ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/observation_post_cache")));

        // Danger levels for mob spawners
        ROOM_DANGER_LEVELS.put(Room.RoomType.REACTOR_CORE, 0.8f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.CONTAINMENT, 0.7f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.CONTROL_ROOM, 0.5f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.LABORATORY, 0.4f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.ARMORY, 0.3f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.SERVER_ROOM, 0.3f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.STORAGE, 0.15f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.BARRACKS, 0.2f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.ENTRANCE, 0.05f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.MAIN_HALL, 0.1f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.MEDBAY, 0.1f);
        ROOM_DANGER_LEVELS.put(Room.RoomType.HALLWAY, 0.1f);
    }

    public static boolean isProfileStructureForBiome(String biomePath, StructureType type) {
        return BiomeGuardianProfiles.isProfileStructureForBiome(biomePath, type);
    }

    public static boolean isKnownStructureBiome(String biomePath) {
        return BiomeGuardianProfiles.byBiome(biomePath).isPresent();
    }

    public static Set<String> getProfileBiomes() {
        return BiomeGuardianProfiles.biomePaths();
    }

    public static StructureType getMainStructureForBiome(String biomePath) {
        return BiomeGuardianProfiles.getMainStructureForBiome(biomePath);
    }

    public static boolean hasGuardianSiteTheme(BiomeGuardianProfile profile) {
        return profile != null && GUARDIAN_THEMES.containsKey(profile.bossPath());
    }

    public static boolean guardianSiteLayoutContractValid(BiomeGuardianProfile profile) {
        GuardianSiteTheme theme = profile == null ? null : GUARDIAN_THEMES.get(profile.bossPath());
        return theme != null
                && theme.surfaceRadius() >= 5
                && theme.scatterDensity() > 0.0F
                && profile.polish() != null
                && !profile.polish().arenaSetPiece().isBlank()
                && !profile.polish().counterplayObject().isBlank()
                && GUARDIAN_MIN_BOSS_ROOM_SIZE >= GUARDIAN_SPAWN_RESERVE_RADIUS * 2 + 9
                && GUARDIAN_BOSS_ROOM_HEIGHT >= GUARDIAN_SPAWN_CLEARANCE;
    }
    
    /**
     * Generate a complete procedural structure at the given position
     */
    public static void generateStructure(ServerLevel level, BlockPos origin,
                                         StructureType type, RandomSource random) {
        generateStructure(level, origin, type, random, null);
    }

    public static void generateStructure(ServerLevel level, BlockPos origin,
                                         StructureType type, RandomSource random,
                                         String bossBiomeOverride) {
        // DROP_POD is a special case - use premade cylindrical design instead of BSP
        if (type == StructureType.DROP_POD) {
            generatePremadeDropPod(level, origin, random);
            return;
        }

        String biomePath = bossBiomeOverride != null ? bossBiomeOverride : getBiomePath(level, origin);
        BiomeGuardianProfile guardianProfile = BiomeGuardianProfiles.byBiome(biomePath)
                .filter(profile -> profile.mainStructure() == type)
                .orElse(null);

        int size = type.getRandomSize(random);
        int roomCount = type.getRandomRoomCount(random);
        if (guardianProfile != null) {
            size = Math.max(size, 34);
            roomCount = Math.max(roomCount, 7);
        }

        // Create BSP tree root
        BSPNode root = new BSPNode(0, 0, 0, size, 12, size);

        // Split into rooms
        int actualRooms = root.split(MIN_ROOM_SIZE, MAX_SPLIT_DEPTH, 0, random);

        // Collect all rooms
        List<Room> rooms = new ArrayList<>();
        root.getAllRooms(rooms);

        // Limit room count if we got too many
        if (rooms.size() > roomCount) {
            rooms = new ArrayList<>(rooms.subList(0, roomCount));
        }

        // Assign room types based on structure
        assignRoomTypes(rooms, type, random);

        // Create corridors
        List<Corridor> corridors = new ArrayList<>();
        root.createCorridors(corridors, random);

        // Multi-floor expansion (Pass 1). Gated: only types whose maxFloors() > 1
        // get upper floors. Adds clones of ~60% of ground rooms at Y += 6,
        // tagged with floorIndex=1+.
        if (type.maxFloors() > 1 && guardianProfile == null) {
            rooms = expandMultipleFloors(rooms, type, random);
        }

        GuardianSiteLayout guardianLayout = guardianProfile != null
                ? ensureGuardianSiteLayout(rooms, size, guardianProfile)
                : null;

        BlockPos buildOrigin = guardianProfile != null
                ? getUndergroundGuardianOrigin(level, origin, size, random)
                : origin;

        // Build the structure
        buildStructure(level, buildOrigin, rooms, corridors, type, random,
                bossBiomeOverride, guardianProfile, guardianLayout, origin);
    }

    private static BlockPos getUndergroundGuardianOrigin(ServerLevel level, BlockPos surfaceOrigin, int size, RandomSource random) {
        BlockPos stableSurface = findGuardianSurfaceOrigin(level, surfaceOrigin);
        int depth = 18 + random.nextInt(19);
        int arenaY = Math.max(level.getMinY() + 8, stableSurface.getY() - depth);
        return new BlockPos(stableSurface.getX() - size / 2, arenaY, stableSurface.getZ() - size / 2);
    }

    private static BlockPos findGuardianSurfaceOrigin(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                BlockPos sample = origin.offset(dx, 0, dz);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        sample.getX(), sample.getZ());
                BlockPos surface = sample.atY(y);
                if (!level.getFluidState(surface).isEmpty()) {
                    continue;
                }
                if (!level.getBlockState(surface.below()).canOcclude()) {
                    continue;
                }
                int score = scoreGuardianSurfaceCandidate(level, surface, origin);
                if (score > bestScore) {
                    best = surface;
                    bestScore = score;
                }
            }
        }
        int fallbackY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                origin.getX(), origin.getZ());
        return best != null ? best : origin.atY(fallbackY);
    }

    private static int scoreGuardianSurfaceCandidate(ServerLevel level, BlockPos surface, BlockPos target) {
        int score = 1000 - (int) Math.sqrt(surface.distSqr(target));
        if (level.getFluidState(surface).isEmpty()) {
            score += 120;
        }
        if (level.getBlockState(surface.below()).canOcclude()) {
            score += 180;
        }
        int y = surface.getY();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = surface.relative(direction, 3);
            int sideY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    side.getX(), side.getZ());
            score -= Math.abs(sideY - y) * 35;
        }
        return score;
    }

    /**
     * Pass 1 - clone ~60% of ground-floor rooms upward to form additional
     * floors. Each upper-floor clone copies the ground room's footprint with
     * Y offset = 6 * floorIndex. Skipped rooms become "open atriums" (the
     * absence of a clone above means the ceiling stays in place).
     *
     * Loot rooms are preferentially cloned upward so loot is distributed
     * vertically. Type assignment carries to clones unchanged except entrance,
     * which always stays on the ground floor.
     */
    private static List<Room> expandMultipleFloors(List<Room> groundRooms, StructureType type,
                                                     RandomSource random) {
        List<Room> all = new ArrayList<>(groundRooms);
        int floors = Math.min(type.maxFloors(), 3); // safety clamp
        int floorHeight = 6;

        for (int floor = 1; floor < floors; floor++) {
            for (Room ground : groundRooms) {
                if (ground.isEntrance()) continue;            // entrance stays ground-only
                if (random.nextFloat() > 0.60f) continue;     // 40% atrium gap
                if (ground.getWidth() < 4 || ground.getDepth() < 4) continue;

                int newY = ground.getY() + floorHeight * floor;
                Room clone = new Room(
                        ground.getX(), newY, ground.getZ(),
                        ground.getWidth(), ground.getHeight(), ground.getDepth(),
                        ground.getType());
                clone.setFloorIndex(floor);
                // 30% of upper rooms become loot rooms even if ground wasn't.
                if (ground.isMainLootRoom() || random.nextFloat() < 0.30f) {
                    clone.setMainLootRoom(true);
                }
                all.add(clone);
            }
        }
        return all;
    }

    private static GuardianSiteLayout ensureGuardianSiteLayout(List<Room> rooms, int structureSize,
                                                               BiomeGuardianProfile profile) {
        if (rooms.isEmpty()) {
            Room entrance = new Room(3, 0, 3, 7, 4, 7, Room.RoomType.ENTRANCE);
            entrance.setEntrance(true);
            rooms.add(entrance);
        }

        Room entranceRoom = rooms.stream().filter(Room::isEntrance).findFirst().orElse(rooms.get(0));
        entranceRoom.setEntrance(true);

        Room bossRoom = selectGuardianBossRoom(rooms, entranceRoom);
        boolean dedicated = false;
        if (bossRoom == null) {
            bossRoom = createDedicatedGuardianBossRoom(rooms, entranceRoom, structureSize);
            rooms.add(bossRoom);
            dedicated = true;
        }
        bossRoom.setType(Room.RoomType.REACTOR_CORE);
        bossRoom.setMainLootRoom(true);
        bossRoom.setHeight(Math.max(bossRoom.getHeight(), GUARDIAN_BOSS_ROOM_HEIGHT));

        return new GuardianSiteLayout(entranceRoom, bossRoom, dedicated, profile.bossPath());
    }

    private static Room selectGuardianBossRoom(List<Room> rooms, Room entranceRoom) {
        return rooms.stream()
                .filter(room -> room != entranceRoom)
                .filter(room -> room.getFloorIndex() == 0)
                .filter(room -> room.getWidth() >= GUARDIAN_MIN_BOSS_ROOM_SIZE)
                .filter(room -> room.getDepth() >= GUARDIAN_MIN_BOSS_ROOM_SIZE)
                .max(Comparator.comparingInt(room -> room.getWidth() * room.getDepth()))
                .orElse(null);
    }

    private static Room createDedicatedGuardianBossRoom(List<Room> rooms, Room entranceRoom, int structureSize) {
        int maxX = rooms.stream()
                .mapToInt(room -> room.getX() + room.getWidth())
                .max()
                .orElse(structureSize);
        int roomZ = clampInt(entranceRoom.getCenterZ() - GUARDIAN_MIN_BOSS_ROOM_SIZE / 2,
                2, Math.max(2, structureSize - GUARDIAN_MIN_BOSS_ROOM_SIZE - 2));
        Room bossRoom = new Room(maxX + 6, entranceRoom.getY(), roomZ,
                GUARDIAN_MIN_BOSS_ROOM_SIZE, GUARDIAN_BOSS_ROOM_HEIGHT,
                GUARDIAN_MIN_BOSS_ROOM_SIZE, Room.RoomType.REACTOR_CORE);
        bossRoom.setMainLootRoom(true);
        return bossRoom;
    }

    /**
     * Generate a premade drop pod - uses NBT if available, otherwise builds procedurally.
     * Auto-generates structure at game start with interior, damage effects, and amenities.
     */
    private static void generatePremadeDropPod(ServerLevel level, BlockPos origin, RandomSource random) {
        StructureTemplateManager templateManager = level.getStructureManager();
        Identifier templateId = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drop_pod");
        Optional<StructureTemplate> templateOpt = templateManager.get(templateId);

        if (templateOpt.isPresent()) {
            placeNBTDropPod(level, origin, random, templateOpt.get());
        } else {
            EchoAshfallProtocol.LOGGER.warn(
                    "Drop pod NBT template not found at {} - using procedural fallback at {}.",
                    templateId, origin);
            placeProceduralStartingDropPod(level, origin, random);
        }
    }

    /**
     * Places drop pod from NBT template.
     *
     * The authored template ships its own 32x32 crater (scorch blocks, lava leaks,
     * hidden smoke columns, debris, buried deepslate courses) - we place it as-is
     * without pre-clearing or adding external impact effects, both of which would
     * stomp on the authored dressing.
     */
    /**
     * Public entry point for placing the starting drop pod at an arbitrary surface
     * origin - used by the first-login handler to drop the pod on top of the
     * player's world spawn. Returns the interior tile a player should stand on
     * (block the player's feet occupy), or {@code null} if placement failed.
     */
    public static BlockPos placeStartingDropPod(ServerLevel level, BlockPos origin, RandomSource random) {
        StructureTemplateManager templateManager = level.getStructureManager();
        Identifier templateId = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drop_pod");
        Optional<StructureTemplate> templateOpt = templateManager.get(templateId);
        if (templateOpt.isEmpty()) {
            EchoAshfallProtocol.LOGGER.warn(
                    "Drop pod NBT template not found at {} - using procedural starting pod fallback at {}.",
                    templateId, origin);
            return placeProceduralStartingDropPod(level, origin, random);
        }
        StructureTemplate template = templateOpt.get();
        BlockPos placePos = getDropPodPlacePos(origin, template.getSize());
        clearStartingDropPodVolume(level, placePos, template.getSize());
        if (!placeNBTDropPod(level, origin, random, template)) {
            return null;
        }

        clearStartingPodProtectedPathDebris(level, placePos, template.getSize());
        BlockPos spawn = findStartingPodSpawn(level, placePos);
        if (spawn != null) {
            polishStartingDropPod(level, spawn);
            clearStartingPodProtectedPathDebris(level, placePos, template.getSize());
        }
        return spawn;
    }

    private static BlockPos placeProceduralStartingDropPod(ServerLevel level, BlockPos origin, RandomSource random) {
        Vec3i fallbackSize = new Vec3i(7, 5, 7);
        BlockPos placePos = getDropPodPlacePos(origin, fallbackSize);
        clearStartingDropPodVolume(level, placePos, fallbackSize);
        buildProceduralDropPod(level, origin, random);

        BlockPos spawn = origin;
        if (!prepareStartingPodSpawn(level, spawn, true)) {
            spawn = findStartingPodSpawn(level, placePos);
        }
        polishStartingDropPod(level, spawn);
        return spawn;
    }

    private static boolean placeNBTDropPod(ServerLevel level, BlockPos origin, RandomSource random, StructureTemplate template) {
        Vec3i size = template.getSize();

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false)
                .setFinalizeEntities(true);

        // The template's internal crater floor sits 2 blocks above its own
        // min-Y (two buried deepslate courses below it). Offset by -2 so that
        // crater floor lands on the world surface.
        BlockPos placePos = getDropPodPlacePos(origin, size);

        try {
            template.placeInWorld(level, placePos, placePos, settings, random, 2);
            removeInvalidBlockEntities(level, placePos, size);
            EchoAshfallProtocol.LOGGER.info("Placed drop pod from NBT at {}", placePos);
            return true;
        } catch (Exception e) {
            EchoAshfallProtocol.LOGGER.error("Failed to place drop pod: {}", e.getMessage());
            return false;
        }
    }

    private static BlockPos getDropPodPlacePos(BlockPos origin, Vec3i size) {
        // The template includes two buried deepslate courses below the crater
        // floor. Offset by -2 so the crater floor lands on the world surface.
        return origin.offset(-size.getX() / 2, -2, -size.getZ() / 2);
    }

    private static BlockPos findStartingPodSpawn(ServerLevel level, BlockPos placePos) {
        int[][] candidates = {
                {STARTING_POD_SPAWN_X, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z},
                {STARTING_POD_SPAWN_X, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z - 1},
                {STARTING_POD_SPAWN_X - 1, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z},
                {STARTING_POD_SPAWN_X + 1, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z},
                {STARTING_POD_SPAWN_X, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z + 1},
                {STARTING_POD_SPAWN_X - 1, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z - 1},
                {STARTING_POD_SPAWN_X + 1, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z - 1},
        };

        for (int[] candidate : candidates) {
            BlockPos pos = placePos.offset(candidate[0], candidate[1], candidate[2]);
            if (prepareStartingPodSpawn(level, pos, false)) {
                return pos;
            }
        }

        BlockPos fallback = placePos.offset(STARTING_POD_SPAWN_X, STARTING_POD_SPAWN_Y, STARTING_POD_SPAWN_Z);
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    for (int dy = 0; dy <= STARTING_POD_CLEAR_MARGIN_Y + 4; dy++) {
                        BlockPos repairedFallback = fallback.offset(dx, dy, dz);
                        if (prepareStartingPodSpawn(level, repairedFallback, true)) {
                            EchoAshfallProtocol.LOGGER.warn(
                                    "Relocated fallback starting pod spawn to {} because authored spawn candidates were blocked.",
                                    repairedFallback);
                            return repairedFallback;
                        }
                    }
                }
            }
        }

        for (int dy = 0; dy <= STARTING_POD_CLEAR_MARGIN_Y + 8; dy++) {
            BlockPos raisedFallback = fallback.above(dy);
            if (prepareStartingPodSpawn(level, raisedFallback, true)) {
                EchoAshfallProtocol.LOGGER.warn(
                        "Cleared fallback starting pod spawn at {} because all authored spawn candidates were blocked.",
                        raisedFallback);
                return raisedFallback;
            }
        }

        EchoAshfallProtocol.LOGGER.warn(
                "Could not fully validate fallback starting pod spawn at {}. Returning authored fallback after clearing non-bedrock headroom.",
                fallback);
        clearBlockForStartingSpawn(level, fallback);
        clearBlockForStartingSpawn(level, fallback.above());
        return fallback;
    }

    private static boolean isSafePlayerSpawn(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && AshfallInteractionRules.supportsPlacement(level, pos.below());
    }

    private static void clearStartingDropPodVolume(ServerLevel level, BlockPos placePos, Vec3i size) {
        int minX = -STARTING_POD_CLEAR_MARGIN_XZ;
        int maxX = size.getX() + STARTING_POD_CLEAR_MARGIN_XZ;
        int minY = 0;
        int maxY = size.getY() + STARTING_POD_CLEAR_MARGIN_Y;
        int minZ = -STARTING_POD_CLEAR_MARGIN_XZ;
        int maxZ = size.getZ() + STARTING_POD_CLEAR_MARGIN_XZ;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (!isStartingDropPodClearColumn(x, z, size)) {
                        continue;
                    }
                    BlockPos pos = placePos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                        clearBlockEntityAndSetAir(level, pos);
                    }
                }
            }
        }
    }

    private static boolean isStartingDropPodClearColumn(int localX, int localZ, Vec3i size) {
        double centerX = (size.getX() - 1) / 2.0;
        double centerZ = (size.getZ() - 1) / 2.0;
        double normalizedX = (localX - centerX) / (size.getX() * 0.50);
        double normalizedZ = (localZ - centerZ) / (size.getZ() * 0.43);
        if (normalizedX * normalizedX + normalizedZ * normalizedZ <= 1.12) {
            return true;
        }

        double rearDistance = centerZ - localZ;
        if (rearDistance <= 0) {
            return false;
        }

        double trailHalfWidth = Math.max(2.0, 7.0 - rearDistance * 0.65);
        return localZ <= centerZ - 4.0 && Math.abs(localX - centerX) <= trailHalfWidth;
    }

    private static void clearStartingPodProtectedPathDebris(ServerLevel level, BlockPos placePos, Vec3i size) {
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                if (!isStartingPodProtectedPathCell(x, z)) {
                    continue;
                }
                BlockPos pos = placePos.offset(x, STARTING_POD_SPAWN_Y, z);
                BlockState state = level.getBlockState(pos);
                if (isStartingPodPathClutter(state)) {
                    clearBlockEntityAndSetAir(level, pos);
                }
            }
        }
    }

    private static boolean isStartingPodProtectedPathCell(int localX, int localZ) {
        if (Math.abs(localX - STARTING_POD_SPAWN_X) <= 1 && localZ >= 8 && localZ <= STARTING_POD_SPAWN_Z) {
            return true;
        }
        if ((localX == 5 || localX == 6 || localX == 10 || localX == 11) && localZ >= 4 && localZ <= 5) {
            return true;
        }
        if (localX >= 4 && localX <= STARTING_POD_SPAWN_X && localZ >= 7 && localZ <= STARTING_POD_SPAWN_Z) {
            return true;
        }
        return Math.abs(localX - STARTING_POD_SPAWN_X) <= 2 && localZ >= 12 && localZ <= 15;
    }

    private static boolean isStartingPodPathClutter(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return state.is(ModBlocks.RUSTED_METAL_DEBRIS.get())
                || state.is(ModBlocks.CABLE_BUNDLE.get())
                || state.is(ModBlocks.TWISTED_METAL.get())
                || Identifier.fromNamespaceAndPath("echoblockworks", "rubble_pile").equals(id)
                || Identifier.fromNamespaceAndPath("echoblockworks", "scattered_debris").equals(id)
                || Identifier.fromNamespaceAndPath("echoblockworks", "steam_vent").equals(id);
    }

    private static boolean prepareStartingPodSpawn(ServerLevel level, BlockPos pos, boolean repairFloor) {
        clearBlockForStartingSpawn(level, pos);
        clearBlockForStartingSpawn(level, pos.above());

        BlockState floor = level.getBlockState(pos.below());
        if ((floor.isAir() || !AshfallInteractionRules.supportsPlacement(floor)) && repairFloor && !floor.is(Blocks.BEDROCK)) {
            setStructureBlock(level, pos.below(), ModBlocks.DROP_POD_HULL.get().defaultBlockState(), 2);
        }

        return isSafePlayerSpawn(level, pos);
    }

    private static void polishStartingDropPod(ServerLevel level, BlockPos spawn) {
        clearBlockForStartingSpawn(level, spawn);
        clearBlockForStartingSpawn(level, spawn.above());
        placeStarterCache(level, spawn);
    }

    private static void placeStarterCache(ServerLevel level, BlockPos spawn) {
        int[][] offsets = {
                {2, 1}, {-2, 1}, {2, -1}, {-2, -1},
                {3, 0}, {-3, 0}, {0, 3}, {0, -3}
        };
        for (int[] offset : offsets) {
            BlockPos pos = spawn.offset(offset[0], 0, offset[1]);
            if (!level.getBlockState(pos).isAir() || !AshfallInteractionRules.supportsPlacement(level, pos.below())) {
                continue;
            }

            BlockState cacheState = ModBlocks.ECHO_CACHE.get().defaultBlockState();
            setStructureBlock(level, pos, cacheState, 2);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof net.minecraft.world.Container cache) {
                cache.setItem(0, new ItemStack(Items.STICK, 8));
                cache.setItem(1, new ItemStack(ModItems.PLANT_FIBER.get(), 8));
                cache.setItem(2, new ItemStack(ModItems.ASH.get(), 6));
                cache.setItem(3, new ItemStack(Items.GLASS_BOTTLE, 3));
                cache.setItem(4, new ItemStack(ModItems.SCRAP_METAL.get(), 6));
                cache.setItem(5, new ItemStack(ModItems.SCRAP_WIRE.get(), 4));
                cache.setItem(6, new ItemStack(Items.TORCH, 8));
                cache.setItem(7, com.knoxhack.echoashfallprotocol.item.BatteryItem.withEnergy(
                        ModItems.BASIC_BATTERY.get(), 1_000));
                be.setChanged();
            }
            return;
        }
    }

    private static void clearBlockForStartingSpawn(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
            clearBlockEntityAndSetAir(level, pos);
        }
    }

    private static void clearBlockEntityAndSetAir(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
            return;
        }
        if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
        }
        setStructureBlock(level, pos, Blocks.AIR.defaultBlockState(), 2);
    }

    public static int removeInvalidBlockEntities(ServerLevel level, BlockPos placePos, Vec3i size) {
        int removed = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = placePos.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.hasBlockEntity()) {
                        level.removeBlockEntity(pos);
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            EchoAshfallProtocol.LOGGER.warn(
                    "Removed {} invalid block entities after drop pod placement at {}.",
                    removed, placePos);
        }
        return removed;
    }

    /**
     * Builds a procedural drop pod directly when NBT is unavailable.
     * Creates a 7x5x7 pod with interior amenities and crash damage.
     */
    private static void buildProceduralDropPod(ServerLevel level, BlockPos origin, RandomSource random) {
        // Pod dimensions: 7x5x7 with center offset
        int sizeX = 7, sizeY = 5, sizeZ = 7;
        int cx = sizeX / 2, cz = sizeZ / 2;

        // Build the pod block by block centered on origin
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockState state = getDropPodBlock(x, y, z, sizeX, sizeY, sizeZ, cx, cz, random);
                    if (state != null) {
                        BlockPos placePos = origin.offset(x - cx, y - 1, z - cz);
                        setStructureBlock(level, placePos, state, 2);
                        if (state.is(Blocks.CHEST)) {
                            assignLootTable(level, placePos, STRUCTURE_LOOT_TABLES.get(StructureType.DROP_POD), random);
                        }
                    }
                }
            }
        }

        // Add impact effects around the pod
        addImpactEffects(level, origin, sizeX, sizeZ, random);

        EchoAshfallProtocol.LOGGER.info("Built procedural drop pod at {}", origin);
    }

    /**
     * Adds impact crater effects around the pod.
     */
    private static void addImpactEffects(ServerLevel level, BlockPos origin, int sizeX, int sizeZ, RandomSource random) {
        int radius = Math.max(sizeX, sizeZ) / 2 + 2;

        // Scattered debris blocks

        // Scattered debris blocks
        for (int i = 0; i < 12; i++) {
            int ox = random.nextInt(radius * 2 + 2) - radius - 1;
            int oz = random.nextInt(radius * 2 + 2) - radius - 1;

            // Skip if inside the pod footprint
            if (Math.abs(ox) < sizeX / 2 && Math.abs(oz) < sizeZ / 2) continue;

            BlockPos craterPos = origin.offset(ox, -1, oz);
            Block debris = switch (random.nextInt(4)) {
                case 0 -> Blocks.COBBLESTONE;
                case 1 -> Blocks.DIRT;
                case 2 -> Blocks.GRAVEL;
                default -> Blocks.IRON_BARS;
            };
            setStructureBlock(level, craterPos, debris.defaultBlockState(), 2);

            // Some debris at y-2 for depth
            if (random.nextFloat() < 0.3f) {
                setStructureBlock(level, craterPos.below(), debris.defaultBlockState(), 2);
            }
        }

        // Scorched earth: replace nearby grass with coarse dirt
        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int z = -radius - 2; z <= radius + 2; z++) {
                // Skip pod interior
                if (Math.abs(x) < sizeX / 2 && Math.abs(z) < sizeZ / 2) continue;

                BlockPos groundPos = origin.offset(x, -1, z);
                if (random.nextFloat() < 0.2f) {
                    setStructureBlock(level, groundPos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }
            }
        }

        // Fire at crash site
        int fireCount = 2 + random.nextInt(2);
        for (int i = 0; i < fireCount; i++) {
            int fx = random.nextInt(radius + 2) - radius / 2;
            int fz = random.nextInt(radius + 2) - radius / 2;

            // Place outside pod
            if (Math.abs(fx) >= sizeX / 2 || Math.abs(fz) >= sizeZ / 2) {
                BlockPos firePos = origin.offset(fx, 0, fz);
                setStructureBlock(level, firePos, Blocks.FIRE.defaultBlockState(), 2);
            }
        }
    }

    /**
     * Creates a drop pod structure by building it directly in a temporary location,
     * then capturing it as a template and saving to NBT for future use.
     * DISABLED - procedural building is now done directly at spawn location.
     */
    private static StructureTemplate createAndSaveDropPodTemplate(ServerLevel level, RandomSource random) {
        // Disabled - use buildProceduralDropPod() instead for in-place generation
        return null;
    }

    /**
     * Determines block type for each position in the drop pod.
     */
    private static BlockState getDropPodBlock(int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                                                int cx, int cz, RandomSource random) {
        double distFromCenter = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));

        // === FLOOR (y=0) ===
        if (y == 0) {
            if (distFromCenter < 2.5) {
                // Hatch at north side (z=0, center x)
                if (z == 0 && x == cx) {
                    return Blocks.IRON_TRAPDOOR.defaultBlockState();
                }
                return Blocks.IRON_BLOCK.defaultBlockState();
            }
            if (distFromCenter < 3.5) {
                return Blocks.COBBLESTONE.defaultBlockState();
            }
            return null;
        }

        // === CEILING (y=SIZE_Y-1) ===
        if (y == sizeY - 1) {
            if (distFromCenter < 2.5) {
                // Center light
                if (x == cx && z == cz) {
                    return Blocks.GLOWSTONE.defaultBlockState();
                }
                return Blocks.IRON_BLOCK.defaultBlockState();
            }
            return null;
        }

        // === WALLS ===
        boolean isWall = distFromCenter >= 2.0 && distFromCenter < 3.0;
        boolean isCorner = (x == 0 || x == sizeX - 1) && (z == 0 || z == sizeZ - 1);

        if (isWall && !isCorner) {
            // Viewport on east/west sides at eye level (y=2)
            if (y == 2 && (x == 0 || x == sizeX - 1) && (z >= 2 && z <= 4)) {
                // One viewport broken on southeast
                if (z == 4 && x == sizeX - 1) {
                    return Blocks.IRON_BARS.defaultBlockState();
                }
                return Blocks.GLASS.defaultBlockState();
            }

            // Scorch marks on south side (impact damage)
            if (z == sizeZ - 1 && y < 3) {
                if (y == 1 && x == cx) {
                    return Blocks.COARSE_DIRT.defaultBlockState(); // Burnt hole
                }
                return Blocks.BLACK_WOOL.defaultBlockState(); // Scorch mark
            }

            // Exposed framework on west side
            if (x == 0 && z == 2 && y == 1) {
                return Blocks.IRON_BARS.defaultBlockState();
            }

            return Blocks.IRON_BLOCK.defaultBlockState();
        }

        // === INTERIOR (y=1 to y=3) ===
        if (distFromCenter < 2.5 && y > 0 && y < sizeY - 1) {
            if (y == 1) {
                // Bed head (north side)
                if (z == 1 && x == cx - 1) {
                    return Blocks.WHITE_WOOL.defaultBlockState();
                }
                // Crafting table (center-south)
                if (z == 4 && x == cx) {
                    return Blocks.CRAFTING_TABLE.defaultBlockState();
                }
                // Furnace (west side)
                if (x == 1 && z == 2) {
                    return Blocks.FURNACE.defaultBlockState();
                }
                // Chest (east side)
                if (x == sizeX - 2 && z == 3) {
                    return Blocks.CHEST.defaultBlockState();
                }
                // Cobwebs in corners
                if ((x == 1 && z == 1) || (x == sizeX - 2 && z == 1)) {
                    if (random.nextFloat() < 0.5f) {
                        return Blocks.COBWEB.defaultBlockState();
                    }
                }
            }
            return null; // Air for interior space
        }

        return null; // Air
    }

    /**
     * Saves the drop pod template to the generated structure folder.
     */
    private static void saveDropPodTemplateToFile(ServerLevel level, StructureTemplate template) throws Exception {
        java.nio.file.Path worldPath = java.nio.file.Path.of(
            level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toString(),
            "generated", "echoashfallprotocol", "structure");
        java.nio.file.Files.createDirectories(worldPath);

        java.nio.file.Path filePath = worldPath.resolve("drop_pod.nbt");
        net.minecraft.nbt.CompoundTag nbt = template.save(new net.minecraft.nbt.CompoundTag());
        net.minecraft.nbt.NbtIo.writeCompressed(nbt, filePath);

        EchoAshfallProtocol.LOGGER.info("Saved drop pod to: {}", filePath);
    }

    /**
     * Assign specific room types based on structure type
     */
    private static void assignRoomTypes(List<Room> rooms, StructureType type, RandomSource random) {
        if (rooms.isEmpty()) return;
        
        // First room is always entrance
        rooms.get(0).setEntrance(true);
        rooms.get(0).setType(Room.RoomType.ENTRANCE);
        
        // Assign types based on structure
        switch (type) {
            case BIO_LAB -> assignBioLabTypes(rooms, random);
            case DATA_CENTER -> assignDataCenterTypes(rooms, random);
            case MILITARY_VAULT -> assignMilitaryTypes(rooms, random);
            case REACTOR_RUIN -> assignReactorTypes(rooms, random);
            case DROP_POD -> assignDropPodTypes(rooms);
            case SUBWAY_STATION -> assignSubwayTypes(rooms, random);
            case SATELLITE_ARRAY -> assignSatelliteTypes(rooms, random);
            case RADIO_TOWER -> assignRadioTowerTypes(rooms, random);
            case SEWER_JUNCTION -> assignSewerTypes(rooms, random);
            case TRAIN_YARD -> assignTrainYardTypes(rooms, random);
            // === EXPLORATION 1.1: FACTION HUBS ===
            case RADWARDEN_OUTPOST -> assignRadwardenOutpostTypes(rooms, random);
            case CRASHBREAK_SALVAGE_YARD -> assignCrashbreakSalvageYardTypes(rooms, random);
            case SPOREBOUND_SANCTUM -> assignSporeboundSanctumTypes(rooms, random);
            // === EXPLORATION 1.1: WORLD POIs ===
            case CRYOGENIC_RUINS -> assignCryogenicRuinsTypes(rooms, random);
            case RELAY_STATION -> assignRelayStationTypes(rooms, random);
            case DERELICT_WORKSHOP -> assignWorkshopTypes(rooms, random);
            case ABANDONED_MINE -> assignMineTypes(rooms, random);
            case OBSERVATION_POST -> assignObservationTypes(rooms, random);
        }
        
        // After types are assigned, adjust heights
        for (Room room : rooms) {
            adjustRoomHeight(room, random);
        }
    }
    
    /**
     * Adjust room height based on its type for architectural variety
     */
    private static void adjustRoomHeight(Room room, RandomSource random) {
        int height = switch (room.getType()) {
            case MAIN_HALL -> 6 + random.nextInt(3);
            case REACTOR_CORE -> 7 + random.nextInt(3);
            case LABORATORY, ARMORY, BARRACKS -> 5 + random.nextInt(2);
            case SERVER_ROOM, CONTROL_ROOM -> 4 + random.nextInt(2);
            case ENTRANCE -> 5;
            case STORAGE, HALLWAY -> 3 + random.nextInt(2);
            // Vanilla houses need specific heights
            case VANILLA_HOUSE_SMALL -> 5;
            case VANILLA_HOUSE_MEDIUM -> 6;
            case VANILLA_HOUSE_LARGE -> 7;
            default -> room.getHeight();
        };
        room.setHeight(height);
    }
    
    private static void assignBioLabTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.LABORATORY, Room.RoomType.CONTAINMENT, 
            Room.RoomType.STORAGE, Room.RoomType.MEDBAY
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            // Large rooms might be main labs
            if (room.getWidth() * room.getDepth() > 80 && random.nextFloat() < 0.5f) {
                room.setType(Room.RoomType.MAIN_HALL);
            }
            
            // Mark one room as main loot room
            if (i == rooms.size() / 2) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignDataCenterTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.SERVER_ROOM, Room.RoomType.CONTROL_ROOM, 
            Room.RoomType.STORAGE, Room.RoomType.HALLWAY
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (i == rooms.size() / 2) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignMilitaryTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.ARMORY, Room.RoomType.BARRACKS, 
            Room.RoomType.MEDBAY, Room.RoomType.STORAGE
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            // Secure room for main loot
            if (i == rooms.size() - 1) {
                room.setType(Room.RoomType.ARMORY);
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignReactorTypes(List<Room> rooms, RandomSource random) {
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            
            // Center room is reactor core
            if (i == rooms.size() / 2) {
                room.setType(Room.RoomType.REACTOR_CORE);
                room.setMainLootRoom(true);
            } else if (random.nextFloat() < 0.4f) {
                room.setType(Room.RoomType.CONTROL_ROOM);
            } else {
                room.setType(Room.RoomType.CONTAINMENT);
            }
        }
    }
    
    private static void assignDropPodTypes(List<Room> rooms) {
        if (rooms.size() > 1) {
            rooms.get(1).setType(Room.RoomType.STORAGE);
            rooms.get(1).setMainLootRoom(true);
        }
    }
    
    private static void assignSubwayTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.HALLWAY, Room.RoomType.STORAGE,
            Room.RoomType.CONTROL_ROOM, Room.RoomType.MAIN_HALL
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 50) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignSatelliteTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.CONTROL_ROOM, Room.RoomType.STORAGE,
            Room.RoomType.SERVER_ROOM
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (i == 1) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignRadioTowerTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.CONTROL_ROOM, Room.RoomType.STORAGE,
            Room.RoomType.ARMORY
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (i == rooms.size() - 1) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignSewerTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.HALLWAY, Room.RoomType.CONTAINMENT,
            Room.RoomType.STORAGE
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 40) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignTrainYardTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.STORAGE, Room.RoomType.ARMORY,
            Room.RoomType.CONTROL_ROOM, Room.RoomType.HALLWAY
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 60) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    // === EXPLORATION 1.1: FACTION HUB ROOM TYPES ===
    
    private static void assignRadwardenOutpostTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.ARMORY, Room.RoomType.CONTROL_ROOM,
            Room.RoomType.BARRACKS, Room.RoomType.STORAGE
        };

        int vanillaCount = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            // 35% chance for a vanilla-style house (militarized village building)
            if (random.nextFloat() < 0.35f) {
                if (room.getWidth() >= 7 && room.getDepth() >= 7) {
                    room.setType(Room.RoomType.VANILLA_HOUSE_MEDIUM);
                } else {
                    room.setType(Room.RoomType.VANILLA_HOUSE_SMALL);
                }
                vanillaCount++;
            } else {
                room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            }

            if (room.getWidth() * room.getDepth() > 35) {
                room.setMainLootRoom(true);
            }
        }
        EchoAshfallProtocol.LOGGER.debug("Radwarden Outpost: assigned {} vanilla houses out of {} rooms", vanillaCount, rooms.size() - 1);
    }
    
    private static void assignCrashbreakSalvageYardTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.STORAGE, Room.RoomType.MARKET,
            Room.RoomType.OFFICE, Room.RoomType.RECEPTION
        };

        int vanillaCount = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            // 40% chance for a vanilla-style house (trading post village building)
            if (random.nextFloat() < 0.40f) {
                if (room.getWidth() >= 9 && room.getDepth() >= 9) {
                    room.setType(Room.RoomType.VANILLA_HOUSE_LARGE);
                } else if (room.getWidth() >= 7 && room.getDepth() >= 7) {
                    room.setType(Room.RoomType.VANILLA_HOUSE_MEDIUM);
                } else {
                    room.setType(Room.RoomType.VANILLA_HOUSE_SMALL);
                }
                vanillaCount++;
            } else {
                room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            }

            if (room.getWidth() * room.getDepth() > 30) {
                room.setMainLootRoom(true);
            }
        }
        EchoAshfallProtocol.LOGGER.debug("Crashbreak Salvage Yard: assigned {} vanilla houses out of {} rooms", vanillaCount, rooms.size() - 1);
    }
    
    private static void assignSporeboundSanctumTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.MEDBAY, Room.RoomType.GREENHOUSE,
            Room.RoomType.LABORATORY, Room.RoomType.CONTAINMENT
        };

        int vanillaCount = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            // 35% chance for a vanilla-style house (bio-adapted village building)
            if (random.nextFloat() < 0.35f) {
                if (room.getWidth() >= 7 && room.getDepth() >= 7) {
                    room.setType(Room.RoomType.VANILLA_HOUSE_MEDIUM);
                } else {
                    room.setType(Room.RoomType.VANILLA_HOUSE_SMALL);
                }
                vanillaCount++;
            } else {
                room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            }

            if (room.getWidth() * room.getDepth() > 32) {
                room.setMainLootRoom(true);
            }
        }
        EchoAshfallProtocol.LOGGER.debug("Sporebound Sanctum: assigned {} vanilla houses out of {} rooms", vanillaCount, rooms.size() - 1);
    }
    
    // === EXPLORATION 1.1: WORLD POI ROOM TYPES ===
    
    private static void assignCryogenicRuinsTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.CONTAINMENT, Room.RoomType.LABORATORY,
            Room.RoomType.STORAGE, Room.RoomType.REACTOR_CORE
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 28) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignRelayStationTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.CONTROL_ROOM, Room.RoomType.STORAGE,
            Room.RoomType.RECEPTION
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 25) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignWorkshopTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.WORKSHOP, Room.RoomType.STORAGE,
            Room.RoomType.OFFICE
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 26) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignMineTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.STORAGE, Room.RoomType.SHAFT,
            Room.RoomType.ORE_VEIN, Room.RoomType.WORKSHOP
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 50) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    private static void assignObservationTypes(List<Room> rooms, RandomSource random) {
        Room.RoomType[] possibleTypes = {
            Room.RoomType.CONTROL_ROOM, Room.RoomType.OBSERVATORY,
            Room.RoomType.STORAGE
        };
        
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            room.setType(possibleTypes[random.nextInt(possibleTypes.length)]);
            
            if (room.getWidth() * room.getDepth() > 22) {
                room.setMainLootRoom(true);
            }
        }
    }
    
    /**
     * Build the complete structure in the world
     */
    private static void buildStructure(ServerLevel level, BlockPos origin,
                                       List<Room> rooms, List<Corridor> corridors,
                                       StructureType type, RandomSource random,
                                       String bossBiomeOverride,
                                       BiomeGuardianProfile guardianProfile,
                                       GuardianSiteLayout guardianLayout,
                                       BlockPos surfaceOrigin) {
        boolean guardianSite = guardianProfile != null;
        GuardianSiteLayout layout = guardianSite && guardianLayout == null
                ? ensureGuardianSiteLayout(rooms, Math.max(34, type.getMaxSize()), guardianProfile)
                : guardianLayout;
        // Pick an ArchStyle for this instance (Pass 5). Seeded by origin so
        // regenerating the same chunk yields the same style. Falls back to
        // the type's default palette when no styles are registered.
        StructureType.ArchStyle style = type.pickStyle(origin.asLong());
        StructureType.BlockPalette palette = style != null ? style.getPalette() : type.getPalette();
        
        // Phase 1: Build terrain foundations to prevent floating structures.
        // Underground guardian arenas are already buried into solid terrain.
        if (!guardianSite) {
            buildTerrainFoundations(level, origin, rooms, corridors, palette, random);
        }
        
        // Clear area first
        clearArea(level, origin, rooms, corridors);
        
        // Build entrance
        buildEntrance(level, origin, rooms, palette, random);
        
        // Build floors
        for (Room room : rooms) {
            buildRoomFloor(level, origin, room, palette, random);
        }
        
        // Build corridor floors
        for (Corridor corridor : corridors) {
            buildCorridorFloor(level, origin, corridor, palette, random);
        }
        
        // Build walls
        for (Room room : rooms) {
            buildRoomWalls(level, origin, room, palette, random);
        }
        
        // Build ceilings - but skip any room with a stacked room directly above
        // (Pass 1 multi-floor). The upper room's floor-build pass writes the
        // shared slab at Y-1, so there's no need for a ceiling here.
        java.util.Set<Room> ceilingSkip = computeStackedBelow(rooms);
        for (Room room : rooms) {
            if (ceilingSkip.contains(room)) continue;
            buildRoomCeiling(level, origin, room, palette, random);
        }

        // Carve ladder columns wherever a room is stacked above another, so
        // the upper floor is reachable.
        for (Room lower : ceilingSkip) {
            carveLadderColumn(level, origin, lower, rooms);
        }

        // Build doorways
        for (Corridor corridor : corridors) {
            buildDoorway(level, origin, corridor, rooms, palette, random);
        }

        // Facade pass: punch windows on exterior walls, hang a structure sign (Pass 2)
        if (!guardianSite) {
            applyFacadePass(level, origin, rooms, type, palette, random);
        }

        // Add room contents
        for (Room room : rooms) {
            populateRoom(level, origin, room, type, random);
        }

        // Furniture density pass (Pass 3) - adds reusable furniture pieces on top
        // of the type-specific populate* output above. Runs before lighting so
        // the lighting pass can place fixtures above newly-placed furniture.
        for (Room room : rooms) {
            populateRoomFurniture(level, origin, room, random);
        }

        // Place interior lighting (Pass 4)
        for (Room room : rooms) {
            placeRoomLighting(level, origin, room, type, random);
        }

        if (!guardianSite) {
            // Add exterior features (Towers, Vents)
            addExteriorFeatures(level, origin, rooms, type, random);

            // Add exterior debris
            addExteriorDebris(level, origin, rooms, type, random);
        }

        // Approach path + outbuilding + ruin halo (Pass 6). Skip for DROP_POD;
        // its NBT ships its own crater/debris and we don't want to stomp it.
        if (type != StructureType.DROP_POD && !guardianSite) {
            buildExteriorApproach(level, origin, rooms, type, palette, random);
        }
        
        // Place mob spawners in dangerous rooms
        for (Room room : rooms) {
            placeMobSpawners(level, origin, room, type, random);
        }

        // Phase 3: Apply decay and damage for post-apocalyptic aesthetic
        applyDecayAndDamage(level, origin, rooms, corridors, type, random);

        if (!guardianSite) {
            // === VANILLA VILLAGE HOUSE INTEGRATION ===
            // Place faction-themed vanilla village houses in designated rooms
            int vanillaHouseCount = 0;
            for (Room room : rooms) {
                if (isVanillaHouse(room.getType())) {
                    vanillaHouseCount++;
                    EchoAshfallProtocol.LOGGER.info("Placing vanilla house type {} in faction {} at room {}x{}",
                            room.getType(), type.getName(), room.getX(), room.getZ());
                    // Clear the room area before placing vanilla house
                    clearRoomArea(level, origin, room);
                    // Place the faction-themed vanilla house
                    VanillaVillageHouseBuilder.placeVanillaHouse(level, origin, room, type, random);
                }
            }
            if (vanillaHouseCount > 0) {
                EchoAshfallProtocol.LOGGER.info("Placed {} vanilla houses in {}", vanillaHouseCount, type.getName());
            }
        }

        GuardianRouteReport guardianRoute = null;
        if (guardianSite) {
            GuardianSiteTheme theme = guardianTheme(guardianProfile);
            repairGuardianBossChamber(level, origin, layout, theme);
            carveGuardianLayoutConnector(level, origin, layout, theme);
            guardianRoute = buildGuardianEntranceAndAccess(level, surfaceOrigin, origin, layout, type, guardianProfile, random);
            decorateGuardianArena(level, origin, layout, guardianProfile, random);
        }

        placeBiomeBoss(level, origin, rooms, layout, type, random, bossBiomeOverride, guardianProfile,
                guardianRoute == null ? null : guardianRoute.entrance());
    }

    private static GuardianRouteReport buildGuardianEntranceAndAccess(ServerLevel level, BlockPos surfaceOrigin,
                                                            BlockPos arenaOrigin, GuardianSiteLayout layout,
                                                            StructureType type, BiomeGuardianProfile profile,
                                                            RandomSource random) {
        GuardianSiteTheme theme = guardianTheme(profile);
        List<GuardianAccessCandidate> candidates = guardianAccessCandidates(level, arenaOrigin, layout.entranceRoom(), surfaceOrigin);
        GuardianAccessCandidate selected = null;
        boolean usedFallback = false;
        boolean reached = false;

        for (GuardianAccessCandidate candidate : candidates) {
            if (buildGuardianAccessRoute(level, candidate.entrance(), candidate.roomEntry(), theme, random)) {
                selected = candidate;
                reached = true;
                break;
            }
        }

        if (selected == null) {
            selected = candidates.get(0);
            usedFallback = true;
            reached = buildGuardianFallbackRoute(level, selected.entrance(), selected.roomEntry(), theme);
            if (!reached) {
                EchoAshfallProtocol.LOGGER.warn(
                        "Guardian route failed all offset candidates for {}; fallback shaft still hit protected terrain at entrance {} roomEntry {}",
                        profile.title(), selected.entrance(), selected.roomEntry());
            }
        }

        prepareGuardianSurfacePad(level, selected.entrance(), theme);
        buildGuardianSurfaceMarker(level, selected.entrance(), theme, random);

        EchoAshfallProtocol.LOGGER.info(
                "GuardianSiteReport guardian={} type={} entrance={} roomEntry={} arena={} dedicatedBossChamber={} routeReached={} fallbackRoute={} form={} descent={}",
                profile.bossPath(), type.getName(), selected.entrance(), selected.roomEntry(),
                arenaOrigin.offset(layout.bossRoom().getCenterX(), layout.bossRoom().getY() + 1, layout.bossRoom().getCenterZ()),
                layout.dedicatedBossChamber(), reached, usedFallback, theme.entranceForm(), theme.descentForm());
        return new GuardianRouteReport(selected.entrance(), selected.roomEntry(), reached, usedFallback);
    }

    private static List<GuardianAccessCandidate> guardianAccessCandidates(ServerLevel level, BlockPos arenaOrigin,
                                                                          Room entranceRoom, BlockPos preferredSurface) {
        int[][] offsets = {
                {0, 0}, {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {3, 3}, {-3, 3}, {3, -3}, {-3, -3}
        };
        List<GuardianAccessCandidate> candidates = new ArrayList<>();
        for (int[] offset : offsets) {
            int localX = entranceRoom.getCenterX() + offset[0];
            int localZ = entranceRoom.getCenterZ() + offset[1];
            if (!isWithinGuardianRoomInterior(entranceRoom, localX, localZ, 2)) {
                continue;
            }
            BlockPos roomEntry = arenaOrigin.offset(localX, entranceRoom.getY() + 1, localZ);
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    roomEntry.getX(), roomEntry.getZ());
            BlockPos entrance = new BlockPos(roomEntry.getX(), surfaceY, roomEntry.getZ());
            int score = scoreGuardianSurfaceCandidate(level, entrance, preferredSurface)
                    - Math.abs(offset[0]) * 12 - Math.abs(offset[1]) * 12;
            candidates.add(new GuardianAccessCandidate(entrance, roomEntry, score));
        }
        if (candidates.isEmpty()) {
            BlockPos roomEntry = arenaOrigin.offset(entranceRoom.getCenterX(), entranceRoom.getY() + 1, entranceRoom.getCenterZ());
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    roomEntry.getX(), roomEntry.getZ());
            candidates.add(new GuardianAccessCandidate(new BlockPos(roomEntry.getX(), surfaceY, roomEntry.getZ()), roomEntry, 0));
        }
        candidates.sort(Comparator.comparingInt(GuardianAccessCandidate::score).reversed());
        return candidates;
    }

    private static boolean isWithinGuardianRoomInterior(Room room, int x, int z, int padding) {
        return x >= room.getX() + padding
                && x < room.getX() + room.getWidth() - padding
                && z >= room.getZ() + padding
                && z < room.getZ() + room.getDepth() - padding;
    }

    private static void prepareGuardianSurfacePad(ServerLevel level, BlockPos entrance, GuardianSiteTheme theme) {
        int radius = Math.max(3, theme.surfaceRadius() - 1);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos floor = entrance.offset(dx, -1, dz);
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                Block block = distance >= radius - 1 ? theme.trim() : theme.floor();
                setStructureBlock(level, floor, block.defaultBlockState(), 2);
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos clear = entrance.offset(dx, dy, dz);
                    if (distance <= radius - 2 || isGuardianOpening(dx, dz)) {
                        clearNonBedrock(level, clear);
                    }
                }
            }
        }
    }

    private static void buildGuardianSurfaceMarker(ServerLevel level, BlockPos entrance,
                                                   GuardianSiteTheme theme, RandomSource random) {
        int radius = theme.surfaceRadius();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos floor = entrance.offset(dx, -1, dz);
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                if (distance <= radius) {
                    Block floorBlock = distance >= radius - 1 ? theme.trim() : theme.floor();
                    if (theme.entranceForm() == GuardianEntranceForm.IMPACT_BREACH && random.nextFloat() < 0.30f) {
                        floorBlock = theme.accent();
                    } else if (theme.entranceForm() == GuardianEntranceForm.TOXIC_SINKHOLE && random.nextFloat() < 0.26f) {
                        floorBlock = theme.accent();
                    } else if (theme.entranceForm() == GuardianEntranceForm.FROZEN_SHAFT && random.nextFloat() < 0.24f) {
                        floorBlock = theme.accent();
                    }
                    setStructureBlock(level, floor, floorBlock.defaultBlockState(), 2);
                }

                if (isGuardianOpening(dx, dz)) {
                    for (int dy = 0; dy <= 5; dy++) {
                        clearNonBedrock(level, entrance.offset(dx, dy, dz));
                    }
                    continue;
                }

                if (distance == radius && random.nextFloat() < theme.scatterDensity()) {
                    placeGuardianBlock(level, entrance.offset(dx, 0, dz),
                            random.nextBoolean() ? theme.prop() : theme.hazard());
                }
            }
        }

        buildGuardianSurfaceLandmark(level, entrance, theme, random);
        clearGuardianSurfaceOpening(level, entrance);
    }

    private static void buildGuardianSurfaceLandmark(ServerLevel level, BlockPos entrance,
                                                     GuardianSiteTheme theme, RandomSource random) {
        switch (theme.entranceForm()) {
            case RESCUE_HATCH -> {
                placeGuardianFrame(level, entrance, theme, 2, 1);
                placeGuardianBlock(level, entrance.offset(0, 0, 3), theme.light());
                placeGuardianBlock(level, entrance.offset(0, 0, -3), theme.core());
                placeGuardianBlock(level, entrance.offset(-3, 0, 0), theme.prop());
                placeGuardianBlock(level, entrance.offset(3, 0, 0), theme.prop());
            }
            case BUNKER_DOOR -> {
                for (int x = -3; x <= 3; x++) {
                    for (int y = 0; y <= 2; y++) {
                        if (Math.abs(x) <= 1 && y <= 1) continue;
                        placeGuardianBlock(level, entrance.offset(x, y, -3),
                                Math.abs(x) == 3 || y == 2 ? theme.trim() : theme.accent());
                    }
                }
                placeGuardianBlock(level, entrance.offset(-4, 0, -2), theme.prop());
                placeGuardianBlock(level, entrance.offset(4, 0, -2), theme.prop());
            }
            case SUBWAY_STAIR -> {
                for (int z = -theme.surfaceRadius(); z <= theme.surfaceRadius(); z++) {
                    if (Math.abs(z) <= 1) continue;
                    placeGuardianBlock(level, entrance.offset(-1, 0, z), Blocks.RAIL);
                    placeGuardianBlock(level, entrance.offset(1, 0, z), Blocks.RAIL);
                }
                placeGuardianBlock(level, entrance.offset(-3, 0, -3), theme.light());
                placeGuardianBlock(level, entrance.offset(3, 0, -3), theme.light());
                placeGuardianBlock(level, entrance.offset(0, 0, 4), theme.accent());
            }
            case FREIGHT_LIFT -> {
                placeGuardianFrame(level, entrance, theme, 3, 2);
                for (int y = 1; y <= 4; y++) {
                    placeGuardianBlock(level, entrance.offset(-3, y, -3), Blocks.IRON_BARS);
                    placeGuardianBlock(level, entrance.offset(3, y, -3), Blocks.IRON_BARS);
                    placeGuardianBlock(level, entrance.offset(-3, y, 3), Blocks.IRON_BARS);
                    placeGuardianBlock(level, entrance.offset(3, y, 3), Blocks.IRON_BARS);
                }
            }
            case TOXIC_SINKHOLE -> {
                for (int i = 0; i < 18; i++) {
                    int x = random.nextInt(theme.surfaceRadius() * 2 + 1) - theme.surfaceRadius();
                    int z = random.nextInt(theme.surfaceRadius() * 2 + 1) - theme.surfaceRadius();
                    if (isGuardianOpening(x, z) || Math.max(Math.abs(x), Math.abs(z)) < 3) continue;
                    placeGuardianBlock(level, entrance.offset(x, 0, z),
                            random.nextFloat() < 0.45f ? theme.hazard() : theme.accent());
                }
                placeGuardianBlock(level, entrance.offset(0, 0, 4), theme.light());
            }
            case IMPACT_BREACH -> {
                for (int i = -theme.surfaceRadius(); i <= theme.surfaceRadius(); i++) {
                    if (Math.abs(i) <= 1) continue;
                    placeGuardianBlock(level, entrance.offset(i, 0, 0), random.nextBoolean() ? theme.accent() : theme.prop());
                    if (i % 2 == 0) {
                        placeGuardianBlock(level, entrance.offset(0, 0, i), theme.hazard());
                    }
                }
                placeGuardianBlock(level, entrance.offset(-4, 1, -2), theme.prop());
                placeGuardianBlock(level, entrance.offset(4, 1, 2), theme.prop());
            }
            case REACTOR_HATCH -> {
                placeGuardianFrame(level, entrance, theme, 3, 1);
                for (int x = -4; x <= 4; x += 2) {
                    placeGuardianBlock(level, entrance.offset(x, 0, -4), Blocks.IRON_BARS);
                    placeGuardianBlock(level, entrance.offset(x, 0, 4), Blocks.IRON_BARS);
                }
                placeGuardianBlock(level, entrance.offset(-3, 0, 0), theme.light());
                placeGuardianBlock(level, entrance.offset(3, 0, 0), theme.light());
            }
            case FROZEN_SHAFT -> {
                placeGuardianFrame(level, entrance, theme, 3, 2);
                for (int[] offset : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                    for (int y = 0; y <= 2; y++) {
                        placeGuardianBlock(level, entrance.offset(offset[0], y, offset[1]),
                                y == 2 ? theme.light() : theme.accent());
                    }
                }
            }
            case NEXUS_BREACH -> {
                for (int[] offset : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                    for (int y = 0; y <= 3; y++) {
                        placeGuardianBlock(level, entrance.offset(offset[0], y, offset[1]),
                                y == 3 ? theme.light() : theme.trim());
                    }
                }
                placeGuardianBlock(level, entrance.offset(0, 0, 4), theme.accent());
                placeGuardianBlock(level, entrance.offset(4, 0, 0), theme.prop());
            }
        }
    }

    private static void placeGuardianFrame(ServerLevel level, BlockPos entrance,
                                           GuardianSiteTheme theme, int radius, int height) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius || isGuardianOpening(dx, dz)) {
                    continue;
                }
                for (int y = 0; y <= height; y++) {
                    placeGuardianBlock(level, entrance.offset(dx, y, dz), y == height ? theme.accent() : theme.trim());
                }
            }
        }
    }

    private static boolean buildGuardianAccessRoute(ServerLevel level, BlockPos entrance, BlockPos roomEntry,
                                                    GuardianSiteTheme theme, RandomSource random) {
        return switch (theme.descentForm()) {
            case LADDER_SHAFT -> buildGuardianAccessShaft(level, entrance, roomEntry, theme);
            case STAIR_TUNNEL -> buildGuardianStairStyledAccess(level, entrance, roomEntry, theme);
            case LIFT_SHAFT -> buildGuardianLiftAccess(level, entrance, roomEntry, theme);
            case SINKHOLE -> buildGuardianSinkholeAccess(level, entrance, roomEntry, theme, random);
            case BREACH_SHAFT -> buildGuardianBreachAccess(level, entrance, roomEntry, theme, random);
        };
    }

    private static boolean buildGuardianFallbackRoute(ServerLevel level, BlockPos entrance, BlockPos roomEntry,
                                                      GuardianSiteTheme theme) {
        return buildGuardianVerticalAccess(level, entrance, roomEntry, theme, 3, false, true);
    }

    private static boolean buildGuardianAccessShaft(ServerLevel level, BlockPos entrance,
                                                    BlockPos roomEntry, GuardianSiteTheme theme) {
        return buildGuardianVerticalAccess(level, entrance, roomEntry, theme, 2, false, false);
    }

    private static boolean buildGuardianStairStyledAccess(ServerLevel level, BlockPos entrance,
                                                          BlockPos roomEntry, GuardianSiteTheme theme) {
        boolean reached = buildGuardianVerticalAccess(level, entrance, roomEntry, theme, 2, false, false);
        int topY = entrance.getY();
        int bottomY = Math.min(roomEntry.getY(), topY - 4);
        for (int y = topY - 2; y >= bottomY + 2; y -= 3) {
            int phase = Math.floorMod((topY - y) / 3, 4);
            int[][] ledges = switch (phase) {
                case 0 -> new int[][]{{-2, 1}, {-2, 0}, {-2, -1}};
                case 1 -> new int[][]{{-1, 2}, {0, 2}, {1, 2}};
                case 2 -> new int[][]{{2, -1}, {2, 0}, {2, 1}};
                default -> new int[][]{{-1, -2}, {0, -2}, {1, -2}};
            };
            for (int[] ledge : ledges) {
                BlockPos pos = new BlockPos(entrance.getX() + ledge[0], y, entrance.getZ() + ledge[1]);
                if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
                    setStructureBlock(level, pos, theme.accent().defaultBlockState(), 2);
                }
            }
        }
        return reached;
    }

    private static boolean buildGuardianLiftAccess(ServerLevel level, BlockPos entrance,
                                                   BlockPos roomEntry, GuardianSiteTheme theme) {
        boolean reached = buildGuardianVerticalAccess(level, entrance, roomEntry, theme, 3, false, true);
        int topY = entrance.getY();
        int bottomY = Math.min(roomEntry.getY(), topY - 4);
        for (int y = topY; y >= bottomY; y--) {
            if (y % 5 == 0) {
                placeGuardianBlock(level, new BlockPos(entrance.getX() - 2, y, entrance.getZ()), theme.light());
                placeGuardianBlock(level, new BlockPos(entrance.getX() + 2, y, entrance.getZ()), theme.light());
            }
        }
        return reached;
    }

    private static boolean buildGuardianSinkholeAccess(ServerLevel level, BlockPos entrance,
                                                       BlockPos roomEntry, GuardianSiteTheme theme, RandomSource random) {
        for (int y = entrance.getY(); y >= entrance.getY() - 5; y--) {
            int radius = Math.max(2, 5 - (entrance.getY() - y) / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distance = Math.max(Math.abs(dx), Math.abs(dz));
                    BlockPos pos = new BlockPos(entrance.getX() + dx, y, entrance.getZ() + dz);
                    if (distance <= radius - 2) {
                        clearNonBedrock(level, pos);
                    } else if (random.nextFloat() < 0.45f) {
                        placeGuardianBlock(level, pos, random.nextBoolean() ? theme.accent() : theme.hazard());
                    }
                }
            }
        }
        return buildGuardianVerticalAccess(level, entrance, roomEntry, theme, 3, true, false);
    }

    private static boolean buildGuardianBreachAccess(ServerLevel level, BlockPos entrance,
                                                     BlockPos roomEntry, GuardianSiteTheme theme, RandomSource random) {
        boolean reached = buildGuardianVerticalAccess(level, entrance, roomEntry, theme, 3, true, false);
        int topY = entrance.getY();
        int bottomY = Math.min(roomEntry.getY(), topY - 4);
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int y = topY - 1; y >= bottomY; y -= 4) {
            Direction direction = directions[random.nextInt(directions.length)];
            for (int step = 2; step <= 4; step++) {
                BlockPos crack = new BlockPos(
                        entrance.getX() + direction.getStepX() * step,
                        y,
                        entrance.getZ() + direction.getStepZ() * step);
                placeGuardianBlock(level, crack, random.nextBoolean() ? theme.accent() : theme.hazard());
            }
        }
        return reached;
    }

    private static boolean buildGuardianVerticalAccess(ServerLevel level, BlockPos entrance, BlockPos roomEntry,
                                                       GuardianSiteTheme theme, int wallRadius,
                                                       boolean roughWalls, boolean liftCables) {
        int topY = entrance.getY();
        int bottomY = Math.min(roomEntry.getY(), topY - 4);
        boolean reached = true;

        for (int y = topY; y >= bottomY; y--) {
            for (int dx = -wallRadius; dx <= wallRadius; dx++) {
                for (int dz = -wallRadius; dz <= wallRadius; dz++) {
                    BlockPos pos = new BlockPos(entrance.getX() + dx, y, entrance.getZ() + dz);
                    int distance = Math.max(Math.abs(dx), Math.abs(dz));
                    if (isGuardianOpening(dx, dz)) {
                        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                            reached = false;
                        }
                        clearNonBedrock(level, pos);
                    } else if (distance == 2 || (wallRadius > 2 && distance == wallRadius && (roughWalls || liftCables))) {
                        Block wallBlock = roughWalls && Math.floorMod(y + dx * 7 + dz * 11, 5) == 0
                                ? theme.accent()
                                : theme.trim();
                        placeGuardianBlock(level, pos, wallBlock);
                    }
                }
            }

            BlockPos ladderBack = new BlockPos(entrance.getX(), y, entrance.getZ() - 2);
            placeGuardianBlock(level, ladderBack, theme.trim());
            BlockPos ladder = new BlockPos(entrance.getX(), y, entrance.getZ() - 1);
            if (!level.getBlockState(ladder).is(Blocks.BEDROCK)) {
                setStructureBlock(level, ladder, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), 2);
            } else {
                reached = false;
            }

            if (liftCables) {
                placeGuardianBlock(level, new BlockPos(entrance.getX() - 3, y, entrance.getZ() - 3), Blocks.IRON_BARS);
                placeGuardianBlock(level, new BlockPos(entrance.getX() + 3, y, entrance.getZ() - 3), Blocks.IRON_BARS);
                placeGuardianBlock(level, new BlockPos(entrance.getX() - 3, y, entrance.getZ() + 3), Blocks.IRON_BARS);
                placeGuardianBlock(level, new BlockPos(entrance.getX() + 3, y, entrance.getZ() + 3), Blocks.IRON_BARS);
            }

            if (Math.floorMod(topY - y, 7) == 3) {
                placeGuardianBlock(level, new BlockPos(entrance.getX() + 1, y, entrance.getZ() + 2), theme.light());
            }
        }

        return carveGuardianLanding(level, roomEntry, theme) && reached;
    }

    private static boolean carveGuardianLanding(ServerLevel level, BlockPos roomEntry, GuardianSiteTheme theme) {
        boolean clear = true;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                setStructureBlock(level, roomEntry.offset(dx, -1, dz),
                        (Math.max(Math.abs(dx), Math.abs(dz)) == 2 ? theme.trim() : theme.floor()).defaultBlockState(), 2);
                for (int dy = 0; dy <= 4; dy++) {
                    BlockPos pos = roomEntry.offset(dx, dy, dz);
                    if (isGuardianOpening(dx, dz) && level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        clear = false;
                    }
                    clearNonBedrock(level, pos);
                }
            }
        }
        return clear;
    }

    private static void clearGuardianSurfaceOpening(ServerLevel level, BlockPos entrance) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    clearNonBedrock(level, entrance.offset(dx, dy, dz));
                }
            }
        }
    }

    private static void repairGuardianBossChamber(ServerLevel level, BlockPos origin,
                                                  GuardianSiteLayout layout, GuardianSiteTheme theme) {
        Room bossRoom = layout.bossRoom();
        for (int x = bossRoom.getX() - 1; x <= bossRoom.getX() + bossRoom.getWidth(); x++) {
            for (int z = bossRoom.getZ() - 1; z <= bossRoom.getZ() + bossRoom.getDepth(); z++) {
                boolean wall = x == bossRoom.getX() - 1 || x == bossRoom.getX() + bossRoom.getWidth()
                        || z == bossRoom.getZ() - 1 || z == bossRoom.getZ() + bossRoom.getDepth();
                BlockPos floor = origin.offset(x, bossRoom.getY() - 1, z);
                setStructureBlock(level, floor, (wall ? theme.trim() : theme.floor()).defaultBlockState(), 2);
                for (int dy = 0; dy <= GUARDIAN_SPAWN_CLEARANCE; dy++) {
                    BlockPos pos = origin.offset(x, bossRoom.getY() + dy, z);
                    if (wall && dy < GUARDIAN_SPAWN_CLEARANCE) {
                        placeGuardianBlock(level, pos, dy == GUARDIAN_SPAWN_CLEARANCE - 1 ? theme.accent() : theme.trim());
                    } else {
                        clearNonBedrock(level, pos);
                    }
                }
            }
        }
        clearGuardianArenaSpawnReserve(level, origin, bossRoom, theme);
    }

    private static void carveGuardianLayoutConnector(ServerLevel level, BlockPos origin,
                                                     GuardianSiteLayout layout, GuardianSiteTheme theme) {
        Room start = layout.entranceRoom();
        Room end = layout.bossRoom();
        if (start == end) {
            return;
        }

        int y = start.getY();
        int x = start.getCenterX();
        int z = start.getCenterZ();
        int endX = end.getCenterX();
        int endZ = end.getCenterZ();
        int step = 0;

        while (x != endX) {
            carveGuardianPathCell(level, origin, x, y, z, theme, step++);
            x += Integer.compare(endX, x);
        }
        while (z != endZ) {
            carveGuardianPathCell(level, origin, x, y, z, theme, step++);
            z += Integer.compare(endZ, z);
        }
        carveGuardianPathCell(level, origin, endX, y, endZ, theme, step);
    }

    private static void carveGuardianPathCell(ServerLevel level, BlockPos origin, int localX, int localY,
                                              int localZ, GuardianSiteTheme theme, int step) {
        BlockPos floor = origin.offset(localX, localY - 1, localZ);
        setStructureBlock(level, floor, (step % 5 == 0 ? theme.accent() : theme.floor()).defaultBlockState(), 2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pathFloor = origin.offset(localX + dx, localY - 1, localZ + dz);
                setStructureBlock(level, pathFloor, (Math.abs(dx) + Math.abs(dz) > 1
                        ? theme.trim() : theme.floor()).defaultBlockState(), 2);
                for (int dy = 0; dy <= 3; dy++) {
                    clearNonBedrock(level, origin.offset(localX + dx, localY + dy, localZ + dz));
                }
            }
        }
        if (step % 7 == 0) {
            placeGuardianBlock(level, origin.offset(localX, localY + 3, localZ), theme.light());
        }
    }

    private static void decorateGuardianArena(ServerLevel level, BlockPos origin, GuardianSiteLayout layout,
                                              BiomeGuardianProfile profile, RandomSource random) {
        GuardianSiteTheme theme = guardianTheme(profile);
        Room bossRoom = layout.bossRoom();
        int cx = bossRoom.getCenterX();
        int cz = bossRoom.getCenterZ();

        for (int x = bossRoom.getX(); x < bossRoom.getX() + bossRoom.getWidth(); x++) {
            for (int z = bossRoom.getZ(); z < bossRoom.getZ() + bossRoom.getDepth(); z++) {
                Block floor = theme.floor();
                int distance = Math.max(Math.abs(x - cx), Math.abs(z - cz));
                if (distance == 3 || distance == 5) {
                    floor = theme.accent();
                } else if (!isGuardianBossSpawnReserve(bossRoom, x, z) && random.nextFloat() < 0.10f) {
                    floor = theme.trim();
                }
                setStructureBlock(level, origin.offset(x, bossRoom.getY() - 1, z), floor.defaultBlockState(), 2);
            }
        }

        clearGuardianArenaSpawnReserve(level, origin, bossRoom, theme);
        placeGuardianArenaPillars(level, origin, bossRoom, theme);
        placeGuardianArenaCore(level, origin, bossRoom, theme);
        placeGuardianPrepAlcove(level, origin, bossRoom, theme);
        scatterGuardianArenaDetails(level, origin, bossRoom, theme, random);
        applyGuardianArenaMotif(level, origin, bossRoom, profile, theme, random);
    }

    private static void clearGuardianArenaSpawnReserve(ServerLevel level, BlockPos origin, Room bossRoom,
                                                       GuardianSiteTheme theme) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = origin.offset(bossRoom.getCenterX() + dx, bossRoom.getY() - 1, bossRoom.getCenterZ() + dz);
                setStructureBlock(level, floor, (Math.max(Math.abs(dx), Math.abs(dz)) == 2
                        ? theme.trim()
                        : theme.floor()).defaultBlockState(), 2);
                for (int dy = 0; dy <= 4; dy++) {
                    clearNonBedrock(level, floor.above(dy + 1));
                }
            }
        }
    }

    private static void placeGuardianArenaPillars(ServerLevel level, BlockPos origin, Room bossRoom,
                                                  GuardianSiteTheme theme) {
        int cx = bossRoom.getCenterX();
        int cz = bossRoom.getCenterZ();
        int[][] pillars = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] offset : pillars) {
            int px = clampInt(cx + offset[0], bossRoom.getX() + 1, bossRoom.getX() + bossRoom.getWidth() - 2);
            int pz = clampInt(cz + offset[1], bossRoom.getZ() + 1, bossRoom.getZ() + bossRoom.getDepth() - 2);
            if (isGuardianBossSpawnReserve(bossRoom, px, pz)) {
                continue;
            }
            for (int y = 1; y <= Math.min(4, bossRoom.getHeight() - 1); y++) {
                placeGuardianBlock(level, origin.offset(px, bossRoom.getY() + y, pz),
                        y == Math.min(4, bossRoom.getHeight() - 1) ? theme.light() : theme.trim());
            }
        }
    }

    private static void placeGuardianArenaCore(ServerLevel level, BlockPos origin, Room bossRoom,
                                               GuardianSiteTheme theme) {
        BlockPos local = findGuardianArenaDisplayLocal(bossRoom);
        if (local == null) {
            return;
        }
        BlockPos world = origin.offset(local.getX(), bossRoom.getY(), local.getZ());
        setStructureBlock(level, world.below(), theme.trim().defaultBlockState(), 2);
        clearNonBedrock(level, world);
        clearNonBedrock(level, world.above());
        placeGuardianBlock(level, world, theme.core());
        placeGuardianBlock(level, world.above(), theme.light());
    }

    private static void placeGuardianPrepAlcove(ServerLevel level, BlockPos origin, Room bossRoom,
                                                GuardianSiteTheme theme) {
        int x = bossRoom.getX() + 2;
        int z = bossRoom.getZ() + 2;
        if (isGuardianBossSpawnReserve(bossRoom, x, z)) {
            z = bossRoom.getZ() + bossRoom.getDepth() - 3;
        }
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                BlockPos floor = origin.offset(x + dx, bossRoom.getY() - 1, z + dz);
                setStructureBlock(level, floor, theme.trim().defaultBlockState(), 2);
                clearNonBedrock(level, floor.above());
                clearNonBedrock(level, floor.above(2));
            }
        }
        placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), z), theme.prop());
        placeGuardianBlock(level, origin.offset(x + 2, bossRoom.getY(), z), Blocks.CRAFTING_TABLE);
        placeGuardianBlock(level, origin.offset(x + 1, bossRoom.getY() + 2, z + 2), theme.light());
    }

    private static BlockPos findGuardianArenaDisplayLocal(Room bossRoom) {
        int cx = bossRoom.getCenterX();
        int cz = bossRoom.getCenterZ();
        int[][] candidates = {
                {cx, bossRoom.getZ() + 2},
                {cx, bossRoom.getZ() + bossRoom.getDepth() - 3},
                {bossRoom.getX() + 2, cz},
                {bossRoom.getX() + bossRoom.getWidth() - 3, cz}
        };
        for (int[] candidate : candidates) {
            int x = clampInt(candidate[0], bossRoom.getX() + 1, bossRoom.getX() + bossRoom.getWidth() - 2);
            int z = clampInt(candidate[1], bossRoom.getZ() + 1, bossRoom.getZ() + bossRoom.getDepth() - 2);
            if (!isGuardianBossSpawnReserve(bossRoom, x, z)) {
                return new BlockPos(x, bossRoom.getY(), z);
            }
        }
        return null;
    }

    private static void scatterGuardianArenaDetails(ServerLevel level, BlockPos origin, Room bossRoom,
                                                    GuardianSiteTheme theme, RandomSource random) {
        int count = Math.max(10, (int) (bossRoom.getWidth() * bossRoom.getDepth() * theme.scatterDensity() * 0.35f));
        for (int i = 0; i < count; i++) {
            BlockPos local = bossRoom.getRandomPosition(random, 2);
            if (isGuardianBossSpawnReserve(bossRoom, local.getX(), local.getZ())) {
                continue;
            }
            BlockPos world = origin.offset(local.getX(), bossRoom.getY(), local.getZ());
            Block block = random.nextFloat() < 0.36f ? theme.hazard() : random.nextBoolean() ? theme.accent() : theme.prop();
            if (level.getBlockState(world).isAir()) {
                placeGuardianBlock(level, world, block);
            }
        }
    }

    private static void applyGuardianArenaMotif(ServerLevel level, BlockPos origin, Room bossRoom,
                                                BiomeGuardianProfile profile, GuardianSiteTheme theme, RandomSource random) {
        int cx = bossRoom.getCenterX();
        int cz = bossRoom.getCenterZ();
        switch (theme.entranceForm()) {
            case RESCUE_HATCH -> {
                for (int i = -6; i <= 6; i++) {
                    if (isGuardianBossSpawnReserve(bossRoom, cx + i, cz)
                            || isGuardianBossSpawnReserve(bossRoom, cx, cz + i)) continue;
                    if (i % 2 == 0) {
                        placeGuardianBlock(level, origin.offset(cx + i, bossRoom.getY(), cz), theme.light());
                        placeGuardianBlock(level, origin.offset(cx, bossRoom.getY(), cz + i), theme.accent());
                    }
                }
            }
            case BUNKER_DOOR -> {
                for (int x = bossRoom.getX() + 2; x < bossRoom.getX() + bossRoom.getWidth() - 2; x += 3) {
                    int northZ = bossRoom.getZ() + 3;
                    int southZ = bossRoom.getZ() + bossRoom.getDepth() - 4;
                    if (!isGuardianBossSpawnReserve(bossRoom, x, northZ)) {
                        placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), northZ), Blocks.IRON_BARS);
                    }
                    if (!isGuardianBossSpawnReserve(bossRoom, x, southZ)) {
                        placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), southZ), theme.prop());
                    }
                }
            }
            case SUBWAY_STAIR, FREIGHT_LIFT -> {
                for (int x = bossRoom.getX() + 2; x < bossRoom.getX() + bossRoom.getWidth() - 2; x++) {
                    if (isGuardianBossSpawnReserve(bossRoom, x, cz - 3) || isGuardianBossSpawnReserve(bossRoom, x, cz + 3)) continue;
                    placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), cz - 3), Blocks.RAIL);
                    placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), cz + 3), Blocks.RAIL);
                }
            }
            case TOXIC_SINKHOLE -> {
                for (int i = 0; i < 10; i++) {
                    BlockPos local = bossRoom.getRandomPosition(random, 2);
                    if (!isGuardianBossSpawnReserve(bossRoom, local.getX(), local.getZ())) {
                        placeGuardianBlock(level, origin.offset(local.getX(), bossRoom.getY(), local.getZ()), theme.hazard());
                    }
                }
            }
            case IMPACT_BREACH -> {
                for (int i = -6; i <= 6; i++) {
                    int x = clampInt(cx + i, bossRoom.getX() + 1, bossRoom.getX() + bossRoom.getWidth() - 2);
                    int z = clampInt(cz + i / 2, bossRoom.getZ() + 1, bossRoom.getZ() + bossRoom.getDepth() - 2);
                    if (!isGuardianBossSpawnReserve(bossRoom, x, z)) {
                        placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), z),
                                i % 3 == 0 ? theme.hazard() : theme.prop());
                    }
                }
            }
            case REACTOR_HATCH -> {
                for (int z = bossRoom.getZ() + 2; z < bossRoom.getZ() + bossRoom.getDepth() - 2; z += 3) {
                    if (isGuardianBossSpawnReserve(bossRoom, cx - 4, z) || isGuardianBossSpawnReserve(bossRoom, cx + 4, z)) continue;
                    placeGuardianBlock(level, origin.offset(cx - 4, bossRoom.getY(), z), Blocks.IRON_BARS);
                    placeGuardianBlock(level, origin.offset(cx + 4, bossRoom.getY(), z), Blocks.IRON_BARS);
                }
            }
            case FROZEN_SHAFT -> {
                for (int[] offset : new int[][]{{-5, 0}, {5, 0}, {0, -5}, {0, 5}}) {
                    int x = clampInt(cx + offset[0], bossRoom.getX() + 1, bossRoom.getX() + bossRoom.getWidth() - 2);
                    int z = clampInt(cz + offset[1], bossRoom.getZ() + 1, bossRoom.getZ() + bossRoom.getDepth() - 2);
                    if (!isGuardianBossSpawnReserve(bossRoom, x, z)) {
                        placeGuardianBlock(level, origin.offset(x, bossRoom.getY(), z), theme.accent());
                        placeGuardianBlock(level, origin.offset(x, bossRoom.getY() + 1, z), theme.accent());
                    }
                }
            }
            case NEXUS_BREACH -> {
                for (int[] offset : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
                    int x = clampInt(cx + offset[0], bossRoom.getX() + 1, bossRoom.getX() + bossRoom.getWidth() - 2);
                    int z = clampInt(cz + offset[1], bossRoom.getZ() + 1, bossRoom.getZ() + bossRoom.getDepth() - 2);
                    if (!isGuardianBossSpawnReserve(bossRoom, x, z)) {
                        for (int y = 0; y <= 2; y++) {
                            placeGuardianBlock(level, origin.offset(x, bossRoom.getY() + y, z),
                                    y == 2 ? theme.light() : theme.trim());
                        }
                    }
                }
            }
            default -> {
                // Other themes are covered by the shared ring, pillar, and scatter passes.
            }
        }
        placeGuardianArenaHazardCues(level, origin, bossRoom, profile, theme, random);
    }

    private static void placeGuardianArenaHazardCues(ServerLevel level, BlockPos origin, Room bossRoom,
                                                     BiomeGuardianProfile profile, GuardianSiteTheme theme,
                                                     RandomSource random) {
        int cx = bossRoom.getCenterX();
        int cz = bossRoom.getCenterZ();
        int[][] anchors = {{-5, 0}, {5, 0}, {0, -5}, {0, 5}};
        for (int[] offset : anchors) {
            int x = clampInt(cx + offset[0], bossRoom.getX() + 1, bossRoom.getX() + bossRoom.getWidth() - 2);
            int z = clampInt(cz + offset[1], bossRoom.getZ() + 1, bossRoom.getZ() + bossRoom.getDepth() - 2);
            if (isGuardianBossSpawnReserve(bossRoom, x, z)) {
                continue;
            }
            BlockPos pos = origin.offset(x, bossRoom.getY(), z);
            Block marker = switch (profile.arenaHazard()) {
                case RECOVERY_BEACON -> theme.light();
                case COMMAND_BUNKER -> random.nextBoolean() ? Blocks.DISPENSER : theme.prop();
                case SHADOW_CORRIDORS -> random.nextBoolean() ? Blocks.SCULK : Blocks.COBWEB;
                case HEAT_VENTS -> random.nextBoolean() ? Blocks.MAGMA_BLOCK : theme.light();
                case HIVE_PODS -> theme.hazard();
                case DEBRIS_FIELD -> random.nextBoolean() ? Blocks.ANVIL : theme.hazard();
                case REACTOR_HOT_ZONE -> theme.hazard();
                case CRYO_VENTS -> random.nextBoolean() ? Blocks.BLUE_ICE : Blocks.PACKED_ICE;
                case ANOMALY_RIFTS -> random.nextBoolean() ? Blocks.CRYING_OBSIDIAN : Blocks.END_ROD;
            };
            placeGuardianBlock(level, pos, marker);
            if (profile.arenaHazard() == BiomeGuardianProfile.ArenaHazard.RECOVERY_BEACON
                    || profile.arenaHazard() == BiomeGuardianProfile.ArenaHazard.ANOMALY_RIFTS) {
                placeGuardianBlock(level, pos.above(), theme.light());
            }
        }
        placeGuardianCounterplayFeature(level, origin, bossRoom, profile, theme);
    }

    private static void placeGuardianCounterplayFeature(ServerLevel level, BlockPos origin, Room bossRoom,
                                                        BiomeGuardianProfile profile, GuardianSiteTheme theme) {
        int cx = bossRoom.getCenterX();
        int cz = bossRoom.getCenterZ();
        int minX = bossRoom.getX() + 2;
        int maxX = bossRoom.getX() + bossRoom.getWidth() - 3;
        int minZ = bossRoom.getZ() + 2;
        int maxZ = bossRoom.getZ() + bossRoom.getDepth() - 3;
        int[][] lanes = {{-7, 0}, {7, 0}, {0, -7}, {0, 7}};
        for (int[] lane : lanes) {
            int x = clampInt(cx + lane[0], minX, maxX);
            int z = clampInt(cz + lane[1], minZ, maxZ);
            if (isGuardianBossSpawnReserve(bossRoom, x, z)) {
                continue;
            }
            BlockPos p = origin.offset(x, bossRoom.getY(), z);
            switch (profile.arenaHazard()) {
                case RECOVERY_BEACON -> {
                    placeGuardianBlock(level, p, Blocks.SEA_LANTERN);
                    placeGuardianBlock(level, p.above(), Blocks.IRON_BARS);
                }
                case COMMAND_BUNKER -> {
                    placeGuardianBlock(level, p, Blocks.RED_BANNER);
                    placeGuardianBlock(level, p.below(), Blocks.IRON_BLOCK);
                }
                case SHADOW_CORRIDORS -> {
                    placeGuardianBlock(level, p, Blocks.SOUL_LANTERN);
                    placeGuardianBlock(level, p.above(), Blocks.IRON_BARS);
                }
                case HEAT_VENTS -> {
                    placeGuardianBlock(level, p, Blocks.MAGMA_BLOCK);
                    placeGuardianBlock(level, p.above(), Blocks.REDSTONE_LAMP);
                }
                case HIVE_PODS -> {
                    placeGuardianBlock(level, p, theme.hazard());
                    placeGuardianBlock(level, p.above(), Blocks.SHROOMLIGHT);
                }
                case DEBRIS_FIELD -> {
                    placeGuardianBlock(level, p, Blocks.ANVIL);
                    placeGuardianBlock(level, p.relative(lane[0] == 0 ? Direction.EAST : Direction.SOUTH), theme.hazard());
                }
                case REACTOR_HOT_ZONE -> {
                    placeGuardianBlock(level, p, ModBlocks.RADIATION_CLEANSER.get());
                    placeGuardianBlock(level, p.above(), Blocks.GLOWSTONE);
                }
                case CRYO_VENTS -> {
                    placeGuardianBlock(level, p, ModBlocks.THERMAL_ARRAY.get());
                    placeGuardianBlock(level, p.above(), Blocks.SEA_LANTERN);
                }
                case ANOMALY_RIFTS -> {
                    placeGuardianBlock(level, p, Blocks.CRYING_OBSIDIAN);
                    placeGuardianBlock(level, p.above(), Blocks.END_ROD);
                }
            }
        }
    }

    private static boolean isGuardianOpening(int dx, int dz) {
        return Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
    }

    private static boolean isGuardianBossSpawnReserve(Room bossRoom, int x, int z) {
        return Math.abs(x - bossRoom.getCenterX()) <= 2 && Math.abs(z - bossRoom.getCenterZ()) <= 2;
    }

    private static void placeGuardianBlock(ServerLevel level, BlockPos pos, Block block) {
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
            setStructureBlock(level, pos, block.defaultBlockState(), 2);
        }
    }

    private static void clearNonBedrock(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
            setStructureBlock(level, pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static boolean setStructureBlock(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
        }
        return level.setBlock(pos, echoStructureState(state), flags);
    }

    private static BlockState echoStructureState(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR || block == Blocks.BEDROCK) {
            return state;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !"minecraft".equals(id.getNamespace())) {
            return state;
        }
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) {
            return ModBlocks.STRUCTURE_CACHE.get().defaultBlockState();
        }
        if (block == Blocks.GLASS || block == Blocks.GLASS_PANE || block == Blocks.TINTED_GLASS) {
            return ModBlocks.SHATTERED_GLASS.get().defaultBlockState();
        }
        if (block == Blocks.WATER || block == Blocks.SLIME_BLOCK || block == Blocks.LILY_PAD) {
            return ModBlocks.ACIDIC_SLUDGE.get().defaultBlockState();
        }
        if (block == Blocks.LAVA || block == Blocks.MAGMA_BLOCK || block == Blocks.FIRE || block == Blocks.NETHERRACK) {
            return ModBlocks.ENERGIZED_FISSURE.get().defaultBlockState();
        }
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW) {
            return ModBlocks.PERMAFROST.get().defaultBlockState();
        }
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH
                || block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN || block == Blocks.REDSTONE_TORCH
                || block == Blocks.REDSTONE_LAMP || block == Blocks.GLOWSTONE || block == Blocks.SEA_LANTERN
                || block == Blocks.SHROOMLIGHT || block == Blocks.END_ROD) {
            return ModBlocks.POWER_NODE.get().defaultBlockState();
        }
        if (block == Blocks.RAIL || block == Blocks.LADDER || block == Blocks.IRON_BARS || "chain".equals(id.getPath())
                || block == Blocks.IRON_TRAPDOOR || block == Blocks.IRON_DOOR) {
            return ModBlocks.REBAR_BLOCK.get().defaultBlockState();
        }
        if (block == Blocks.CRAFTING_TABLE) {
            return ModBlocks.WORKSHOP_BLOCK.get().defaultBlockState();
        }
        if (block == Blocks.FURNACE) {
            return ModBlocks.THERMAL_BURNER.get().defaultBlockState();
        }
        if (block == Blocks.CAULDRON || block == Blocks.BREWING_STAND) {
            return ModBlocks.CONTAMINANT_CONDENSER.get().defaultBlockState();
        }
        if (block == Blocks.OBSERVER || block == Blocks.COMPARATOR || block == Blocks.DAYLIGHT_DETECTOR || block == Blocks.BOOKSHELF) {
            return ModBlocks.SIGNAL_SCANNER.get().defaultBlockState();
        }
        if (block == Blocks.ANVIL || block == Blocks.HOPPER || block == Blocks.DISPENSER) {
            return ModBlocks.HAND_RECYCLER.get().defaultBlockState();
        }
        if (block == Blocks.LECTERN) {
            return ModBlocks.MAP_TABLE.get().defaultBlockState();
        }
        if (block == Blocks.WHITE_BED || block == Blocks.RED_BED || block == Blocks.BLUE_BED || block == Blocks.GREEN_BED
                || block == Blocks.OAK_STAIRS || block == Blocks.OAK_TRAPDOOR) {
            return ModBlocks.EMERGENCY_BUNK.get().defaultBlockState();
        }
        if (block == Blocks.COAL_ORE || block == Blocks.IRON_ORE) {
            return ModBlocks.SCRAP_ORE.get().defaultBlockState();
        }
        if (block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.DEAD_BUSH) {
            return ModBlocks.DRY_GRASS.get().defaultBlockState();
        }
        if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.GRASS_BLOCK) {
            return ModBlocks.WASTELAND_DIRT.get().defaultBlockState();
        }
        if (block == Blocks.GRAVEL || block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE
                || block == Blocks.STONE || block == Blocks.STONE_BRICKS || block == Blocks.CRACKED_STONE_BRICKS
                || block == Blocks.MOSSY_STONE_BRICKS || block == Blocks.SMOOTH_STONE) {
            return ModBlocks.CONCRETE_RUBBLE.get().defaultBlockState();
        }
        if (block == Blocks.IRON_BLOCK || block == Blocks.COPPER_BLOCK || block == Blocks.EXPOSED_COPPER
                || block == Blocks.CUT_COPPER || block == Blocks.OXIDIZED_COPPER) {
            return ModBlocks.RUSTED_METAL_SHEET.get().defaultBlockState();
        }
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.AMETHYST_BLOCK) {
            return ModBlocks.RIFTSTONE.get().defaultBlockState();
        }
        return ModBlocks.ASH_STONE.get().defaultBlockState();
    }

    private static Block entranceBlock(BiomeGuardianProfile profile) {
        return guardianTheme(profile).floor();
    }

    private static Block entranceTrimBlock(BiomeGuardianProfile profile) {
        return guardianTheme(profile).trim();
    }

    private static Block arenaAccentBlock(BiomeGuardianProfile profile) {
        return guardianTheme(profile).accent();
    }

    private static Block arenaCoreBlock(BiomeGuardianProfile profile) {
        return guardianTheme(profile).core();
    }

    private static void placeBiomeBoss(ServerLevel level, BlockPos origin, List<Room> rooms,
                                       GuardianSiteLayout guardianLayout,
                                       StructureType type, RandomSource random, String bossBiomeOverride,
                                       BiomeGuardianProfile guardianProfile, BlockPos guardianEntrance) {
        if (rooms.isEmpty()) return;

        String biomePath = bossBiomeOverride != null ? bossBiomeOverride : getBiomePath(level, origin);
        BiomeGuardianProfile profile = guardianProfile != null
                ? guardianProfile
                : BiomeGuardianProfiles.byBiome(biomePath).orElse(null);
        if (profile == null || profile.bossType() == null || profile.mainStructure() != type) {
            return;
        }

        EntityType<? extends BiomeBossEntity> bossType = profile.bossType().get();
        Optional<BiomeBossEntity> existing = findExistingBiomeBoss(level, origin, bossType);
        if (existing.isPresent()) {
            if (guardianEntrance != null) {
                BiomeGuardianSiteData.get(level).addOrUpdate(profile, guardianEntrance, existing.get().blockPosition());
            }
            EchoAshfallProtocol.LOGGER.info(
                    "GuardianSiteReport guardian={} reusedExistingBoss=true entrance={} arena={} duplicateSpawnSkipped=true",
                    profile.bossPath(), guardianEntrance, existing.get().blockPosition());
            return;
        }

        Room bossRoom = guardianLayout != null ? guardianLayout.bossRoom() : findBossRoom(rooms);
        BlockPos spawnPos = findBossSpawnPosition(level, origin, bossRoom, random, profile);
        if (spawnPos == null) {
            spawnPos = findBossFallbackSpawnPosition(level, origin, bossRoom, guardianEntrance, profile);
            if (spawnPos == null) {
                EchoAshfallProtocol.LOGGER.warn("Could not place biome boss for {} at {}: no valid room position",
                        biomePath, origin);
                return;
            }
            EchoAshfallProtocol.LOGGER.warn("Used repaired fallback biome boss spawn for {} at {}",
                    biomePath, spawnPos);
        }

        BiomeBossEntity boss = bossType.create(level, EntitySpawnReason.EVENT);
        if (boss == null) {
            EchoAshfallProtocol.LOGGER.warn("Could not create biome boss {} for {} at {}", bossType, biomePath, origin);
            return;
        }

        boss.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        boss.setYRot(random.nextFloat() * 360.0f);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
        if (guardianEntrance != null) {
            BiomeGuardianSiteData.get(level).addOrUpdate(profile, guardianEntrance, spawnPos);
        }
        EchoAshfallProtocol.LOGGER.info(
                "GuardianSiteReport guardian={} bossType={} biome={} entrance={} arena={} dedicatedBossChamber={} bossSpawned=true",
                profile.bossPath(), bossType, biomePath, guardianEntrance, spawnPos,
                guardianLayout != null && guardianLayout.dedicatedBossChamber());
    }

    private static Optional<BiomeBossEntity> findExistingBiomeBoss(ServerLevel level, BlockPos origin,
                                                                   EntityType<? extends BiomeBossEntity> bossType) {
        AABB searchArea = new AABB(origin).inflate(96.0, 48.0, 96.0);
        return level.getEntitiesOfClass(BiomeBossEntity.class, searchArea,
                boss -> boss.isAlive() && boss.getType() == bossType).stream().findFirst();
    }

    private static Room findBossRoom(List<Room> rooms) {
        Room best = null;
        for (Room room : rooms) {
            if (room.isEntrance()) continue;
            if (room.isMainLootRoom()) {
                return room;
            }
            if (best == null || room.getWidth() * room.getDepth() > best.getWidth() * best.getDepth()) {
                best = room;
            }
        }
        return best != null ? best : rooms.get(0);
    }

    private static BlockPos findBossSpawnPosition(ServerLevel level, BlockPos origin, Room room,
                                                  RandomSource random, BiomeGuardianProfile profile) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(origin.offset(room.getCenterX(), room.getY() + 1, room.getCenterZ()));
        for (int i = 0; i < 12; i++) {
            BlockPos local = room.getRandomPosition(random, 2);
            candidates.add(origin.offset(local.getX(), local.getY(), local.getZ()));
        }

        Block floorRepairBlock = guardianTheme(profile).floor();
        for (BlockPos candidate : candidates) {
            if (prepareBossSpawnPosition(level, candidate, true, floorRepairBlock)) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockPos findBossFallbackSpawnPosition(ServerLevel level, BlockPos origin, Room room,
                                                          BlockPos guardianEntrance, BiomeGuardianProfile profile) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(origin.offset(room.getCenterX(), room.getY() + 1, room.getCenterZ()));
        candidates.add(origin.offset(room.getCenterX(), room.getY() + 2, room.getCenterZ()));
        if (guardianEntrance != null) {
            candidates.add(guardianEntrance.offset(0, 1, 4));
            candidates.add(guardianEntrance.offset(0, 2, 4));
        }

        Block floorRepairBlock = arenaAccentBlock(profile);
        for (BlockPos candidate : candidates) {
            if (prepareBossSpawnPosition(level, candidate, true, floorRepairBlock)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean prepareBossSpawnPosition(ServerLevel level, BlockPos candidate,
                                                    boolean repairFloor, Block floorRepairBlock) {
        if (level.getBlockState(candidate).is(Blocks.BEDROCK)
                || level.getBlockState(candidate.above()).is(Blocks.BEDROCK)
                || level.getBlockState(candidate.above(2)).is(Blocks.BEDROCK)) {
            return false;
        }

        BlockPos floorPos = candidate.below();
        BlockState floor = level.getBlockState(floorPos);
        if ((floor.isAir() || !AshfallInteractionRules.supportsPlacement(floor)) && repairFloor && !floor.is(Blocks.BEDROCK)) {
            setStructureBlock(level, floorPos, floorRepairBlock.defaultBlockState(), 2);
            floor = level.getBlockState(floorPos);
        }
        if (floor.isAir() || !AshfallInteractionRules.supportsPlacement(floor)) {
            return false;
        }
        if (!repairFloor && (!level.getFluidState(candidate).isEmpty()
                || !level.getFluidState(candidate.above()).isEmpty()
                || !level.getFluidState(candidate.above(2)).isEmpty())) {
            return false;
        }

        setStructureBlock(level, candidate, Blocks.AIR.defaultBlockState(), 2);
        setStructureBlock(level, candidate.above(), Blocks.AIR.defaultBlockState(), 2);
        setStructureBlock(level, candidate.above(2), Blocks.AIR.defaultBlockState(), 2);
        return true;
    }

    private static String getBiomePath(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(Object::toString)
                .map(ProceduralStructureGenerator::extractBiomePath)
                .orElse("");
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
     * Check if a room type is a vanilla house variant.
     */
    private static boolean isVanillaHouse(Room.RoomType type) {
        return type == Room.RoomType.VANILLA_HOUSE_SMALL ||
               type == Room.RoomType.VANILLA_HOUSE_MEDIUM ||
               type == Room.RoomType.VANILLA_HOUSE_LARGE;
    }

    /**
     * Clear a single room area before placing vanilla house.
     */
    private static void clearRoomArea(ServerLevel level, BlockPos origin, Room room) {
        for (int x = room.getX() - 1; x < room.getX() + room.getWidth() + 1; x++) {
            for (int y = room.getY() - 1; y < room.getY() + room.getHeight() + 2; y++) {
                for (int z = room.getZ() - 1; z < room.getZ() + room.getDepth() + 1; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    setStructureBlock(level, pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void clearArea(ServerLevel level, BlockPos origin, 
                                   List<Room> rooms, List<Corridor> corridors) {
        // Clear rooms
        for (Room room : rooms) {
            for (int x = room.getX() - 1; x < room.getX() + room.getWidth() + 1; x++) {
                for (int y = room.getY(); y < room.getY() + room.getHeight(); y++) {
                    for (int z = room.getZ() - 1; z < room.getZ() + room.getDepth() + 1; z++) {
                        BlockPos pos = origin.offset(x, y, z);
                        setStructureBlock(level, pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        // Clear corridors
        for (Corridor corridor : corridors) {
            for (BlockPos pathPos : corridor.getExpandedPath()) {
                for (int y = 0; y < corridor.getHeight(); y++) {
                    BlockPos pos = origin.offset(pathPos.getX(), pathPos.getY() + y, pathPos.getZ());
                    setStructureBlock(level, pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }
    
    /**
     * Build stepped foundations that follow terrain to prevent floating structures.
     * Analyzes ground level at structure perimeter and creates support pillars/foundations.
     */
    private static void buildTerrainFoundations(ServerLevel level, BlockPos origin,
                                                  List<Room> rooms, List<Corridor> corridors,
                                                  StructureType.BlockPalette palette, RandomSource random) {
        if (rooms.isEmpty()) return;
        
        // Get structure bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int structureY = rooms.get(0).getY();
        
        for (Room room : rooms) {
            minX = Math.min(minX, room.getX() - 2);
            maxX = Math.max(maxX, room.getX() + room.getWidth() + 2);
            minZ = Math.min(minZ, room.getZ() - 2);
            maxZ = Math.max(maxZ, room.getZ() + room.getDepth() + 2);
        }
        
        // Sample terrain heights at grid points
        Map<BlockPos, Integer> terrainHeights = new HashMap<>();
        int sampleSpacing = 3; // Check every 3 blocks
        
        for (int x = minX; x <= maxX; x += sampleSpacing) {
            for (int z = minZ; z <= maxZ; z += sampleSpacing) {
                BlockPos samplePos = origin.offset(x, structureY, z);
                int groundY = findGroundLevel(level, samplePos);
                terrainHeights.put(samplePos, groundY);
            }
        }
        
        // Build foundations at sample points
        for (Map.Entry<BlockPos, Integer> entry : terrainHeights.entrySet()) {
            BlockPos samplePos = entry.getKey();
            int groundY = entry.getValue();
            int foundationDepth = structureY - groundY;
            
            // Only build foundation if needed (ground is below structure floor)
            if (foundationDepth > 0) {
                // Build support column from ground up to structure floor
                for (int dy = 0; dy < foundationDepth; dy++) {
                    BlockPos foundationPos = samplePos.offset(0, -dy - 1, 0);
                    
                    // Use stone/concrete for underground, palette blocks near surface
                    Block foundationBlock;
                    if (dy < foundationDepth - 2) {
                        foundationBlock = Blocks.STONE;
                    } else if (dy < foundationDepth - 1) {
                        foundationBlock = Blocks.COBBLESTONE;
                    } else {
                        foundationBlock = palette.secondary();
                    }
                    
                    // Only replace air/non-solid blocks
                    if (level.getBlockState(foundationPos).isAir() ||
                        !AshfallInteractionRules.supportsPlacement(level, foundationPos)) {
                        setStructureBlock(level, foundationPos, foundationBlock.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        // Build perimeter footing (1 block wider than structure)
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                BlockPos perimeterPos = origin.offset(x, structureY - 1, z);
                
                // Check if this is on the perimeter edge
                boolean isEdge = (x == minX - 1 || x == maxX + 1 || z == minZ - 1 || z == maxZ + 1);
                
                if (isEdge && level.getBlockState(perimeterPos).isAir()) {
                    // Find ground and build down
                    int groundY = findGroundLevel(level, perimeterPos);
                    for (int y = structureY - 2; y >= groundY; y--) {
                        BlockPos downPos = origin.offset(x, y, z);
                        if (level.getBlockState(downPos).isAir()) {
                            setStructureBlock(level, downPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        
        // Clear vegetation/floating blocks above ground within structure bounds
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos checkPos = origin.offset(x, structureY, z);
                int groundY = findGroundLevel(level, checkPos);
                
                // Clear blocks between ground and structure floor
                for (int y = groundY + 1; y < structureY; y++) {
                    BlockPos clearPos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(clearPos);
                    // Clear vegetation, snow, etc.
                    if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || 
                        state.is(Blocks.SNOW) || state.is(Blocks.FERN)) {
                        setStructureBlock(level, clearPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
    
    /**
     * Find the ground level (first solid block) at a position going downward
     */
    private static int findGroundLevel(ServerLevel level, BlockPos startPos) {
        int y = startPos.getY();
        
        // Search downward up to 20 blocks
        for (int i = 0; i < 20; i++) {
            BlockPos checkPos = startPos.offset(0, -i, 0);
            BlockState state = level.getBlockState(checkPos);
            
            // Stop when we find a solid block that's not vegetation
            if (AshfallInteractionRules.supportsPlacement(state) && !state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.TALL_GRASS) &&
                !state.is(Blocks.FERN) && !state.is(Blocks.DEAD_BUSH)) {
                return checkPos.getY();
            }
        }
        
        return startPos.getY() - 5; // Default fallback
    }
    
    private static void buildRoomFloor(ServerLevel level, BlockPos origin, 
                                        Room room, StructureType.BlockPalette palette, 
                                        RandomSource random) {
        boolean isIndustrial = room.getType() == Room.RoomType.STORAGE || room.getType() == Room.RoomType.REACTOR_CORE;
        boolean hasConduit = (room.getWidth() > 5 && room.getDepth() > 5) && random.nextFloat() < 0.3f;
        
        for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
            for (int z = room.getZ(); z < room.getZ() + room.getDepth(); z++) {
                BlockPos pos = origin.offset(x, room.getY() - 1, z);
                
                // Determine block based on patterns
                Block block;
                if (hasConduit && (x == room.getCenterX() || z == room.getCenterZ())) {
                    // Power conduit under floor grating
                    setStructureBlock(level, pos.below(), Blocks.GLOWSTONE.defaultBlockState(), 2);
                    block = Blocks.IRON_BARS;
                } else if (isIndustrial && (x + z) % 4 == 0) {
                    // Hazard stripe pattern
                    block = Blocks.YELLOW_CONCRETE;
                } else if (isIndustrial && (x + z) % 4 == 1) {
                    block = Blocks.BLACK_CONCRETE;
                } else {
                    float decay = room.getType() == Room.RoomType.REACTOR_CORE ? 0.3f : 0.05f;
                    block = palette.getRandomBlock(random, decay);
                }
                
                setStructureBlock(level, pos, block.defaultBlockState(), 2);
            }
        }
    }
    
    private static void buildCorridorFloor(ServerLevel level, BlockPos origin,
                                            Corridor corridor, StructureType.BlockPalette palette,
                                            RandomSource random) {
        for (BlockPos pathPos : corridor.getExpandedPath()) {
            BlockPos pos = origin.offset(pathPos.getX(), pathPos.getY() - 1, pathPos.getZ());
            setStructureBlock(level, pos, palette.primary().defaultBlockState(), 2);
        }
    }
    
    private static void addExteriorDebris(ServerLevel level, BlockPos origin, List<Room> rooms, 
                                          StructureType type, RandomSource random) {
        if (rooms.isEmpty()) return;
        Room root = rooms.get(0);
        
        // Scatter some debris around the entrance and bounding box
        for (int i = 0; i < 15; i++) {
            int rx = random.nextInt(30) - 15;
            int rz = random.nextInt(30) - 15;
            BlockPos pos = origin.offset(root.getCenterX() + rx, root.getY() - 1, root.getZ() + rz);
            
            if (level.getBlockState(pos.above()).isAir()) {
                Block debris = random.nextBoolean() ? Blocks.COBBLESTONE : Blocks.GRAVEL;
                setStructureBlock(level, pos.above(), debris.defaultBlockState(), 2);
            }
        }
        
        // Add some vegetation if themed
        if (type == StructureType.BIO_LAB || type == StructureType.SEWER_JUNCTION) {
            for (int i = 0; i < 10; i++) {
                int rx = random.nextInt(20) - 10;
                int rz = random.nextInt(20) - 10;
                BlockPos pos = origin.offset(root.getCenterX() + rx, root.getY() - 1, root.getZ() + rz);
                if (level.getBlockState(pos.above()).isAir()) {
                    setStructureBlock(level, pos.above(), Blocks.SHORT_GRASS.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void buildEntrance(ServerLevel level, BlockPos origin, List<Room> rooms,
                                      StructureType.BlockPalette palette, RandomSource random) {
        if (rooms.isEmpty()) return;
        Room entranceRoom = rooms.get(0);
        
        int z = entranceRoom.getZ() - 1;
        int x = entranceRoom.getCenterX();
        BlockPos entrancePos = origin.offset(x, entranceRoom.getY(), z);
        
        // Build Airlock Module (3x3x3)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -3; dz <= 0; dz++) {
                    BlockPos p = entrancePos.offset(dx, dy, dz);
                    boolean isWall = dx == -2 || dx == 2 || dy == -1 || dy == 3 || dz == -3;
                    boolean isHole = (dx >= -1 && dx <= 1) && (dy >= 0 && dy <= 2);
                    
                    if (isWall) {
                        if (!isHole || dz != -3) {
                            setStructureBlock(level, p, palette.secondary().defaultBlockState(), 2);
                        }
                    } else if (dy < 3) {
                        setStructureBlock(level, p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        // Add lighting to entrance
        setStructureBlock(level, entrancePos.offset(-1, 2, -3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        setStructureBlock(level, entrancePos.offset(1, 2, -3), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        
        // Clear passage into room
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                setStructureBlock(level, entrancePos.offset(dx, dy, 0), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void addExteriorFeatures(ServerLevel level, BlockPos origin, List<Room> rooms, 
                                            StructureType type, RandomSource random) {
        for (Room room : rooms) {
            // Only add features to roofs of larger rooms
            if (room.getWidth() * room.getDepth() < 30) continue;
            
            BlockPos roofPos = origin.offset(room.getCenterX(), room.getY() + room.getHeight() + 1, room.getCenterZ());
            
            if (random.nextFloat() < 0.4f) {
                // Ventilation Tower
                for (int y = 0; y < 2; y++) {
                    setStructureBlock(level, roofPos.above(y), Blocks.IRON_BLOCK.defaultBlockState(), 2);
                }
                setStructureBlock(level, roofPos.above(2), Blocks.IRON_TRAPDOOR.defaultBlockState(), 2);
            } else if (random.nextFloat() < 0.3f) {
                // Antenna
                for (int y = 0; y < 4; y++) {
                    setStructureBlock(level, roofPos.above(y), Blocks.IRON_BARS.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void buildRoomWalls(ServerLevel level, BlockPos origin,
                                        Room room, StructureType.BlockPalette palette,
                                        RandomSource random) {
        int minX = room.getX() - WALL_THICKNESS;
        int maxX = room.getX() + room.getWidth() + WALL_THICKNESS;
        int minZ = room.getZ() - WALL_THICKNESS;
        int maxZ = room.getZ() + room.getDepth() + WALL_THICKNESS;
        int minY = room.getY();
        int maxY = room.getY() + room.getHeight();
        
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                // Determine if this is a wall position
                boolean isWallX = x < room.getX() || x >= room.getX() + room.getWidth();
                boolean isWallZ = z < room.getZ() || z >= room.getZ() + room.getDepth();
                
                if (isWallX || isWallZ) {
                    // Corner or Pillar position
                    boolean isCorner = (x == minX || x == maxX - 1) && (z == minZ || z == maxZ - 1);
                    boolean isPillarX = (x - room.getX()) % 4 == 0;
                    boolean isPillarZ = (z - room.getZ()) % 4 == 0;
                    boolean isPillar = isCorner || (isWallX && isPillarZ) || (isWallZ && isPillarX);
                    
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = origin.offset(x, y, z);
                        Block block;
                        
                        if (isPillar) {
                            block = palette.secondary(); // Vertical support
                        } else if (y == minY || y == maxY - 1) {
                            block = palette.secondary(); // Horizontal trim
                        } else if (y == minY + 2 && (x + z) % 3 == 0) {
                            // Patterned wall detail (Screens/Vents)
                            block = palette.accent();
                        } else {
                            block = palette.primary();
                        }
                        
                        setStructureBlock(level, pos, block.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
    
    private static void buildRoomCeiling(ServerLevel level, BlockPos origin,
                                          Room room, StructureType.BlockPalette palette,
                                          RandomSource random) {
        int baseCeilingY = room.getY() + room.getHeight();
        RoofStyle roofStyle = selectRoofStyle(room, random);
        
        // Build roof based on style
        switch (roofStyle) {
            case GABLED -> buildGabledRoof(level, origin, room, palette, baseCeilingY);
            case SHED -> buildShedRoof(level, origin, room, palette, baseCeilingY, random);
            case FLAT_PARAPET -> buildFlatRoofWithParapet(level, origin, room, palette, baseCeilingY, random);
            case DOME -> buildDomeRoof(level, origin, room, palette, baseCeilingY);
            default -> buildStandardFlatRoof(level, origin, room, palette, baseCeilingY);
        }
    }
    
    private enum RoofStyle {
        FLAT, FLAT_PARAPET, GABLED, SHED, DOME
    }
    
    private static RoofStyle selectRoofStyle(Room room, RandomSource random) {
        // Larger rooms get more interesting roofs
        boolean isLarge = room.getWidth() * room.getDepth() > 60;
        boolean isTall = room.getHeight() >= 5;
        
        if (room.getType() == Room.RoomType.REACTOR_CORE) {
            return RoofStyle.DOME; // Dome for reactor
        } else if (isLarge && isTall && random.nextFloat() < 0.4f) {
            return RoofStyle.GABLED;
        } else if (isLarge && random.nextFloat() < 0.3f) {
            return RoofStyle.SHED;
        } else if (random.nextFloat() < 0.2f) {
            return RoofStyle.FLAT_PARAPET;
        } else {
            return RoofStyle.FLAT;
        }
    }
    
    private static void buildStandardFlatRoof(ServerLevel level, BlockPos origin,
                                               Room room, StructureType.BlockPalette palette,
                                               int baseY) {
        boolean hasRafters = room.getWidth() > 4 && room.getDepth() > 4;
        
        for (int x = room.getX() - 1; x < room.getX() + room.getWidth() + 1; x++) {
            for (int z = room.getZ() - 1; z < room.getZ() + room.getDepth() + 1; z++) {
                BlockPos pos = origin.offset(x, baseY, z);
                setStructureBlock(level, pos, palette.primary().defaultBlockState(), 2);
                
                // Add rafters protruding downwards
                if (hasRafters && x >= room.getX() && x < room.getX() + room.getWidth() &&
                    z >= room.getZ() && z < room.getZ() + room.getDepth()) {
                    
                    boolean isRafter = (x - room.getX()) % 4 == 0 || (z - room.getZ()) % 4 == 0;
                    if (isRafter) {
                        setStructureBlock(level, pos.below(), palette.secondary().defaultBlockState(), 2);
                    }
                    
                    // Add central light fixture
                    if (x == room.getCenterX() && z == room.getCenterZ()) {
                        setStructureBlock(level, pos.below(), Blocks.GLOWSTONE.defaultBlockState(), 2);
                        setStructureBlock(level, pos.below(2), Blocks.IRON_BARS.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
    
    private static void buildGabledRoof(ServerLevel level, BlockPos origin,
                                         Room room, StructureType.BlockPalette palette,
                                         int baseY) {
        // Gabled roof - peaks in center along longest axis
        boolean alongX = room.getWidth() >= room.getDepth();
        int span = alongX ? room.getDepth() : room.getWidth();
        int peakHeight = Math.min(4, span / 3);
        
        for (int y = 0; y <= peakHeight; y++) {
            int inset = y; // Each layer up moves inward by 1
            int roofY = baseY + y;
            
            for (int x = room.getX() - 1 + (alongX ? 0 : inset); 
                 x < room.getX() + room.getWidth() + 1 - (alongX ? 0 : inset); x++) {
                for (int z = room.getZ() - 1 + (alongX ? inset : 0); 
                     z < room.getZ() + room.getDepth() + 1 - (alongX ? inset : 0); z++) {
                    
                    BlockPos pos = origin.offset(x, roofY, z);
                    
                    // Determine block type
                    boolean isEdge = (alongX && (z == room.getZ() - 1 + inset || z == room.getZ() + room.getDepth() - inset)) ||
                                    (!alongX && (x == room.getX() - 1 + inset || x == room.getX() + room.getWidth() - inset));
                    Block roofBlock = isEdge ? palette.secondary() : palette.primary();
                    
                    setStructureBlock(level, pos, roofBlock.defaultBlockState(), 2);
                }
            }
        }
        
        // Fill interior under roof
        for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
            for (int z = room.getZ(); z < room.getZ() + room.getDepth(); z++) {
                int distFromCenter = alongX ? 
                    Math.abs(z - (room.getZ() + room.getDepth() / 2)) : 
                    Math.abs(x - (room.getX() + room.getWidth() / 2));
                int roofHeightAtPos = peakHeight - distFromCenter;
                
                for (int y = 0; y < roofHeightAtPos; y++) {
                    BlockPos fillPos = origin.offset(x, baseY + y, z);
                    if (y == roofHeightAtPos - 1) {
                        setStructureBlock(level, fillPos, palette.primary().defaultBlockState(), 2);
                    } else if (y > 0) {
                        setStructureBlock(level, fillPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
    
    private static void buildShedRoof(ServerLevel level, BlockPos origin,
                                       Room room, StructureType.BlockPalette palette,
                                       int baseY, RandomSource random) {
        // Single slope direction
        boolean slopeX = random.nextBoolean();
        int span = slopeX ? room.getWidth() : room.getDepth();
        int slopeHeight = Math.min(3, span / 4);
        
        for (int x = room.getX() - 1; x < room.getX() + room.getWidth() + 1; x++) {
            for (int z = room.getZ() - 1; z < room.getZ() + room.getDepth() + 1; z++) {
                int offset = slopeX ? (x - room.getX()) : (z - room.getZ());
                float slopeFactor = (float) offset / span;
                int roofY = baseY + Math.round(slopeFactor * slopeHeight);
                
                BlockPos pos = origin.offset(x, roofY, z);
                setStructureBlock(level, pos, palette.primary().defaultBlockState(), 2);
            }
        }
    }
    
    private static void buildFlatRoofWithParapet(ServerLevel level, BlockPos origin,
                                                  Room room, StructureType.BlockPalette palette,
                                                  int baseY, RandomSource random) {
        // Flat roof with low wall around edge
        for (int x = room.getX() - 1; x < room.getX() + room.getWidth() + 1; x++) {
            for (int z = room.getZ() - 1; z < room.getZ() + room.getDepth() + 1; z++) {
                BlockPos pos = origin.offset(x, baseY, z);
                boolean isEdge = x == room.getX() - 1 || x == room.getX() + room.getWidth() ||
                                z == room.getZ() - 1 || z == room.getZ() + room.getDepth();
                
                if (isEdge) {
                    // Parapet wall (1-2 blocks high)
                    setStructureBlock(level, pos, palette.secondary().defaultBlockState(), 2);
                    setStructureBlock(level, pos.above(), palette.secondary().defaultBlockState(), 2);
                } else {
                    setStructureBlock(level, pos, palette.primary().defaultBlockState(), 2);
                }
            }
        }
        
        // Add occasional vents on parapet
        if (random.nextFloat() < 0.5f) {
            int ventX = room.getX() + random.nextInt(room.getWidth());
            int ventZ = room.getZ() - 1;
            BlockPos ventPos = origin.offset(ventX, baseY + 1, ventZ);
            setStructureBlock(level, ventPos, Blocks.IRON_BARS.defaultBlockState(), 2);
        }
    }
    
    private static void buildDomeRoof(ServerLevel level, BlockPos origin,
                                       Room room, StructureType.BlockPalette palette,
                                       int baseY) {
        // Simple dome approximation
        int centerX = room.getCenterX();
        int centerZ = room.getZ() + room.getDepth() / 2;
        int radius = Math.min(room.getWidth(), room.getDepth()) / 2;
        int domeHeight = Math.min(4, radius / 2 + 1);
        
        for (int y = 0; y <= domeHeight; y++) {
            int layerRadius = radius - (y * radius / domeHeight);
            int roofY = baseY + y;
            
            for (int x = centerX - layerRadius; x <= centerX + layerRadius; x++) {
                for (int z = centerZ - layerRadius; z <= centerZ + layerRadius; z++) {
                    int distSq = (x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ);
                    if (distSq <= layerRadius * layerRadius) {
                        BlockPos pos = origin.offset(x, roofY, z);
                        Block block = (y == domeHeight) ? palette.accent() : palette.primary();
                        setStructureBlock(level, pos, block.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
    
    private static void buildDoorway(ServerLevel level, BlockPos origin,
                                     Corridor corridor, List<Room> rooms, 
                                     StructureType.BlockPalette palette, RandomSource random) {
        // Find where the corridor enters/exits rooms
        for (Room room : rooms) {
            if (room == corridor.getStartRoom() || room == corridor.getEndRoom()) {
                // Check path positions for doorway candidates
                for (BlockPos p : corridor.getPath()) {
                    // Check if this point is on the room boundary (wall)
                    boolean onBoundary = (p.getX() == room.getX() - 1 || p.getX() == room.getX() + room.getWidth()) ||
                                         (p.getZ() == room.getZ() - 1 || p.getZ() == room.getZ() + room.getDepth());
                    
                    if (onBoundary && room.contains(p.getX(), p.getY(), p.getZ(), 1)) {
                        // Place an archway
                        BlockPos doorwayPos = origin.offset(p.getX(), p.getY(), p.getZ());
                        
                        // Clear the arch opening (2x3)
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int y = 0; y < 3; y++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    // Identify if we should clear this spot or place frame
                                    BlockPos target = doorwayPos.offset(dx, y, dz);
                                    
                                    // Is this spot part of the corridor width?
                                    boolean inCorridor = false;
                                    for (int wdx = -corridor.getWidth()/2; wdx <= corridor.getWidth()/2; wdx++) {
                                        for (int wdz = -corridor.getWidth()/2; wdz <= corridor.getWidth()/2; wdz++) {
                                            if (dx == wdx && dz == wdz) inCorridor = true;
                                        }
                                    }

                                    if (inCorridor && y < 3) {
                                        setStructureBlock(level, target, Blocks.AIR.defaultBlockState(), 2);
                                    }
                                }
                            }
                        }
                        
                        // Place the frame (Arch)
                        // Simple arch: cap the top
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                setStructureBlock(level, doorwayPos.offset(dx, 3, dz), palette.secondary().defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static boolean placeProp(ServerLevel level, BlockPos pos, Block block) {
        if (level.getBlockState(pos).isAir()) {
            setStructureBlock(level, pos, block.defaultBlockState(), 2);
            return true;
        }
        return false;
    }

    private static boolean placeLootContainer(ServerLevel level, BlockPos pos, Block block,
                                              ResourceKey<LootTable> lootTable, RandomSource random) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        BlockState state = ModBlocks.STRUCTURE_CACHE.get().defaultBlockState();
        setStructureBlock(level, pos, state, 2);
        return assignLootTable(level, pos, lootTable, random);
    }

    private static boolean assignLootTable(ServerLevel level, BlockPos pos,
                                           ResourceKey<LootTable> lootTable, RandomSource random) {
        if (lootTable == null) {
            EchoAshfallProtocol.LOGGER.warn("No loot table available for structure container at {}", pos);
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(lootTable, random.nextLong());
            container.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
            return true;
        }

        EchoAshfallProtocol.LOGGER.warn("Placed structure container at {} without a loot-capable block entity", pos);
        return false;
    }

    /**
     * Place a cluster of blocks to form a complex prop
     */
    private static void placePropCluster(ServerLevel level, BlockPos origin, Room room, RandomSource random, String type) {
        BlockPos pos = room.getRandomPosition(random, 2);
        BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());

        switch (type) {
            case "growth_tank" -> {
                // 1x2x1 Glass tank with slime/water
                placeProp(level, worldPos, Blocks.GLASS);
                placeProp(level, worldPos.above(), Blocks.GLASS);
                placeProp(level, worldPos.offset(0, 0, 0), Blocks.SLIME_BLOCK); // Inside-like
                setStructureBlock(level, worldPos, Blocks.SLIME_BLOCK.defaultBlockState(), 2);
                setStructureBlock(level, worldPos.above(), Blocks.TINTED_GLASS.defaultBlockState(), 2);
            }
            case "server_cluster" -> {
                // 3x2x1 Server rack row
                for (int dx = -1; dx <= 1; dx++) {
                    setStructureBlock(level, worldPos.offset(dx, 0, 0), Blocks.BOOKSHELF.defaultBlockState(), 2);
                    setStructureBlock(level, worldPos.offset(dx, 1, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                    // Add cabling to ceiling
                    for (int dy = 2; dy < room.getHeight() - 1; dy++) {
                        setStructureBlock(level, worldPos.offset(dx, dy, 0), Blocks.IRON_BARS.defaultBlockState(), 2);
                    }
                }
            }
            case "terminal_desk" -> {
                // Simple desk with screen
                setStructureBlock(level, worldPos, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(), 2);
                setStructureBlock(level, worldPos.above(), Blocks.GLOWSTONE.defaultBlockState(), 2);
                setStructureBlock(level, worldPos.above().north(), Blocks.IRON_BARS.defaultBlockState(), 2); // "Screen frame"
            }
        }
    }
    
    private static void populateRoom(ServerLevel level, BlockPos origin,
                                      Room room, StructureType type, RandomSource random) {
        // Add chests
        if (room.isMainLootRoom() || random.nextFloat() < 0.4f) {
            int chestCount = room.isMainLootRoom() ? 2 : 1;
            for (int i = 0; i < chestCount; i++) {
                BlockPos chestPos = room.getRandomPosition(random, 2);
                BlockPos worldPos = origin.offset(chestPos.getX(), chestPos.getY(), chestPos.getZ());
                
                if (level.getBlockState(worldPos).isAir()) {
                    ResourceKey<LootTable> lootTable = getLootTableForRoom(room, type);
                    placeLootContainer(level, worldPos, Blocks.CHEST, lootTable, random);
                }
            }
        }
        
        // Structure-specific themed population
        switch (type) {
            case BIO_LAB -> populateBioLab(level, origin, room, random);
            case DATA_CENTER -> populateDataCenter(level, origin, room, random);
            case MILITARY_VAULT -> populateMilitaryVault(level, origin, room, random);
            case REACTOR_RUIN -> populateReactorRuin(level, origin, room, random);
            case DROP_POD -> populateDropPod(level, origin, room, random);
            case SUBWAY_STATION -> populateSubwayStation(level, origin, room, random);
            case SATELLITE_ARRAY -> populateSatelliteArray(level, origin, room, random);
            case RADIO_TOWER -> populateRadioTower(level, origin, room, random);
            case SEWER_JUNCTION -> populateSewerJunction(level, origin, room, random);
            case TRAIN_YARD -> populateTrainYard(level, origin, room, random);
            // === EXPLORATION 1.1: FACTION HUBS ===
            case RADWARDEN_OUTPOST -> populateRadwardenOutpost(level, origin, room, random);
            case CRASHBREAK_SALVAGE_YARD -> populateCrashbreakSalvageYard(level, origin, room, random);
            case SPOREBOUND_SANCTUM -> populateSporeboundSanctum(level, origin, room, random);
            // === EXPLORATION 1.1: WORLD POIs ===
            case CRYOGENIC_RUINS -> populateCryogenicRuins(level, origin, room, random);
            case RELAY_STATION -> populateRelayStation(level, origin, room, random);
            case DERELICT_WORKSHOP -> populateDerelictWorkshop(level, origin, room, random);
            case ABANDONED_MINE -> populateAbandonedMine(level, origin, room, random);
            case OBSERVATION_POST -> populateObservationPost(level, origin, room, random);
        }
    }

    private static void populateBioLab(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Lab tables and growth tanks
        int clusterCount = 1 + random.nextInt(2);
        for (int i = 0; i < clusterCount; i++) {
            placePropCluster(level, origin, room, random, "growth_tank");
        }
        
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            placePropCluster(level, origin, room, random, "terminal_desk");
        }

        // Containment barriers
        if (room.getType() == Room.RoomType.CONTAINMENT) {
            for (int dx = 1; dx < room.getWidth() - 1; dx++) {
                placeProp(level, origin.offset(room.getX() + dx, room.getY(), room.getZ() + room.getDepth() / 2), Blocks.IRON_BARS);
            }
        }
    }

    private static void populateDataCenter(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Server clusters
        int clusterCount = 1 + random.nextInt(3);
        for (int i = 0; i < clusterCount; i++) {
            placePropCluster(level, origin, room, random, "server_cluster");
        }
        
        // Control terminals
        placePropCluster(level, origin, room, random, "terminal_desk");
    }

    private static void populateMilitaryVault(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Ammo crates (Barrels)
        for (int i = 0; i < 3 + random.nextInt(5); i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeLootContainer(level, worldPos, Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.MILITARY_VAULT), random);
        }
        // Security barriers
        if (room.getType() == Room.RoomType.ARMORY) {
            placeProp(level, origin.offset(room.getCenterX(), room.getY(), room.getZ() + 1), Blocks.IRON_BARS);
        }
    }
    
    private static void populateReactorRuin(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Control rods (Glowstone + Iron Bars)
        BlockPos center = room.getCenter();
        for (int y = 0; y < 3; y++) {
            placeProp(level, origin.offset(center.getX(), room.getY() + y, center.getZ()), Blocks.GLOWSTONE);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    placeProp(level, origin.offset(center.getX() + dx, room.getY() + y, center.getZ() + dz), Blocks.IRON_BARS);
                }
            }
        }
        // Warning lights
        if (random.nextFloat() < 0.4f) {
            placeProp(level, origin.offset(room.getCenterX(), room.getY() + room.getHeight() - 1, room.getCenterZ()), Blocks.REDSTONE_LAMP);
        }
    }

    private static void populateDropPod(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Emergency supplies
        for (int i = 0; i < 2; i++) {
            BlockPos pos = room.getRandomPosition(random, 1);
            placeLootContainer(level, origin.offset(pos.getX(), pos.getY(), pos.getZ()),
                    Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.DROP_POD), random);
        }
        // Impact crater effect (Cobblestone scattered)
        for (int i = 0; i < 5; i++) {
            BlockPos pos = room.getRandomPosition(random, 0);
            placeProp(level, origin.offset(pos.getX(), room.getY() - 1, pos.getZ()), Blocks.COBBLESTONE);
        }
    }

    private static void populateSubwayStation(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Platform edges (Iron Bars)
        for (int x = 0; x < room.getWidth(); x++) {
            placeProp(level, origin.offset(room.getX() + x, room.getY(), room.getZ() + 1), Blocks.IRON_BARS);
        }
        // Benches (Stairs)
        for (int i = 0; i < 2; i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            placeProp(level, origin.offset(pos.getX(), pos.getY(), pos.getZ()), Blocks.OAK_STAIRS);
        }
    }

    private static void populateSatelliteArray(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Control pedestals (Lecterns)
        BlockPos pos = room.getCenter();
        placeProp(level, origin.offset(pos.getX(), room.getY(), pos.getZ()), Blocks.LECTERN);
        // Power cables (Copper)
        for (int z = 0; z < room.getDepth(); z++) {
            placeProp(level, origin.offset(room.getCenterX(), room.getY(), room.getZ() + z), Blocks.COPPER_BLOCK);
        }
    }

    private static void populateRadioTower(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Maintenance ladder
        for (int y = 0; y < room.getHeight(); y++) {
            placeProp(level, origin.offset(room.getX() + 1, room.getY() + y, room.getZ() + 1), Blocks.LADDER);
        }
        // Equipment (Chests)
        BlockPos pos = room.getRandomPosition(random, 1);
        placeLootContainer(level, origin.offset(pos.getX(), pos.getY(), pos.getZ()),
                Blocks.CHEST, STRUCTURE_LOOT_TABLES.get(StructureType.RADIO_TOWER), random);
    }

    private static void populateSewerJunction(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Overflow pipes (Iron Bars)
        for (int y = 1; y < 3; y++) {
            placeProp(level, origin.offset(room.getX(), room.getY() + y, room.getCenterZ()), Blocks.IRON_BARS);
        }
        // Slime deposits
        if (random.nextFloat() < 0.3f) {
            BlockPos pos = room.getRandomPosition(random, 1);
            placeProp(level, origin.offset(pos.getX(), room.getY(), pos.getZ()), Blocks.SLIME_BLOCK);
        }
    }

    private static void populateTrainYard(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Cargo containers (Barrels + Trapdoors)
        for (int i = 0; i < 3; i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            if (placeLootContainer(level, worldPos, Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.TRAIN_YARD), random)) {
                placeProp(level, worldPos.above(), Blocks.OAK_TRAPDOOR);
            }
        }
        // Rails
        for (int x = 0; x < room.getWidth(); x++) {
            placeProp(level, origin.offset(room.getX() + x, room.getY(), room.getCenterZ()), Blocks.RAIL);
        }
    }
    
    /**
     * Get the appropriate loot table for a room based on its type or structure
     */
    private static ResourceKey<LootTable> getLootTableForRoom(Room room, StructureType type) {
        // Try room-specific loot table first
        ResourceKey<LootTable> roomLoot = ROOM_LOOT_TABLES.get(room.getType());
        if (roomLoot != null) {
            return roomLoot;
        }
        
        // Fall back to structure-specific loot table
        ResourceKey<LootTable> structureLoot = STRUCTURE_LOOT_TABLES.get(type);
        if (structureLoot != null) {
            return structureLoot;
        }
        
        // Ultimate fallback
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/industrial_factory_cache"));
    }
    
    /**
     * Place mob spawners in dangerous rooms
     */
    private static void placeMobSpawners(ServerLevel level, BlockPos origin,
                                          Room room, StructureType type, RandomSource random) {
        Float dangerLevel = ROOM_DANGER_LEVELS.getOrDefault(room.getType(), 0.1f);
        
        // Roll for spawner placement based on danger
        if (random.nextFloat() >= dangerLevel) {
            return;
        }
        
        // Get appropriate mob for room type and structure
        EntityType<?> mobType = getMobForRoom(room, type, random);
        if (mobType == null) return;
        
        // Find position for spawner (usually against a wall or in center)
        BlockPos spawnerPos = findSpawnerPosition(level, origin, room, random);
        if (spawnerPos == null) return;
        
        // Place spawner block
        setStructureBlock(level, spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        
        // Configure spawner
        if (level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(mobType, random);
            // Spawner will use default delay set by setEntityId
        }
    }
    
    /**
     * Find a good position for a mob spawner within a room
     */
    private static BlockPos findSpawnerPosition(ServerLevel level, BlockPos origin, 
                                                 Room room, RandomSource random) {
        // Try up to 5 positions
        for (int attempt = 0; attempt < 5; attempt++) {
            BlockPos pos = room.getRandomPosition(random, 3);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            
            // Must be air with solid block below
            if (level.getBlockState(worldPos).isAir() && 
                !level.getBlockState(worldPos.below()).isAir()) {
                return worldPos;
            }
        }
        return null;
    }
    
    /**
     * Get appropriate mob type for room and structure
     */
    private static EntityType<?> getMobForRoom(Room room, StructureType type, RandomSource random) {
        // Structure-specific mob selections
        switch (type) {
            case BIO_LAB -> {
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.RAD_ZOMBIE.get();
                    case 1 -> ModEntities.MUTATED_CRAWLER.get();
                    default -> ModEntities.TOXIC_SLIME.get();
                };
            }
            case DATA_CENTER -> {
                return switch (random.nextInt(2)) {
                    case 0 -> ModEntities.ECHO_DRONE.get();
                    default -> ModEntities.RUST_WALKER.get();
                };
            }
            case MILITARY_VAULT -> {
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.SCAVENGER_BANDIT.get();
                    case 1 -> ModEntities.RUST_WALKER.get();
                    default -> ModEntities.RAD_ZOMBIE.get();
                };
            }
            case REACTOR_RUIN -> {
                // High radiation areas have glowing ghouls and toxic slimes
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.GLOWING_GHOUL.get();
                    case 1 -> ModEntities.TOXIC_SLIME.get();
                    default -> ModEntities.RAD_ZOMBIE.get();
                };
            }
            case DROP_POD -> {
                // Drop pods rarely have enemies
                return random.nextFloat() < 0.3f ? ModEntities.MUTATED_CRAWLER.get() : null;
            }
            case SUBWAY_STATION -> {
                // Underground areas have crawlers and zombies
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.MUTATED_CRAWLER.get();
                    case 1 -> ModEntities.RAD_ZOMBIE.get();
                    default -> ModEntities.SCAVENGER_BANDIT.get();
                };
            }
            case SATELLITE_ARRAY -> {
                // Satellite arrays have drones and rust walkers
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.ECHO_DRONE.get();
                    case 1 -> ModEntities.RUST_WALKER.get();
                    default -> ModEntities.MUTATED_CRAWLER.get();
                };
            }
            case RADIO_TOWER -> {
                // Radio towers have glowing ghouls (radiation) and bandits
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.GLOWING_GHOUL.get();
                    case 1 -> ModEntities.SCAVENGER_BANDIT.get();
                    default -> ModEntities.RAD_ZOMBIE.get();
                };
            }
            case SEWER_JUNCTION -> {
                // Sewers have toxic slimes and crawlers
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.TOXIC_SLIME.get();
                    case 1 -> ModEntities.MUTATED_CRAWLER.get();
                    default -> ModEntities.RAD_ZOMBIE.get();
                };
            }
            case TRAIN_YARD -> {
                // Train yards have bandits and rust walkers
                return switch (random.nextInt(3)) {
                    case 0 -> ModEntities.SCAVENGER_BANDIT.get();
                    case 1 -> ModEntities.RUST_WALKER.get();
                    default -> ModEntities.RAD_ZOMBIE.get();
                };
            }
            default -> {
                return ModEntities.RAD_ZOMBIE.get();
            }
        }
    }
    
    /**
     * Populate a container (chest, barrel) with random loot
     */
    private static void populateContainer(ServerLevel level, BlockPos pos, RandomSource random, int minItems, int maxItems) {
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container)) {
            return;
        }
        
        int itemCount = minItems + random.nextInt(maxItems - minItems + 1);
        for (int i = 0; i < itemCount; i++) {
            // Add random scrap or useful items
            var item = switch (random.nextInt(8)) {
                case 0 -> net.minecraft.world.item.Items.IRON_INGOT;
                case 1 -> net.minecraft.world.item.Items.GOLD_NUGGET;
                case 2 -> net.minecraft.world.item.Items.REDSTONE;
                case 3 -> net.minecraft.world.item.Items.BREAD;
                case 4 -> net.minecraft.world.item.Items.TORCH;
                case 5 -> net.minecraft.world.item.Items.STRING;
                case 6 -> net.minecraft.world.item.Items.BONE;
                default -> net.minecraft.world.item.Items.STICK;
            };
            container.setItem(random.nextInt(27), new net.minecraft.world.item.ItemStack(item, 1 + random.nextInt(3)));
        }
    }
    
    // === EXPLORATION 1.1: FACTION HUB POPULATION ===
    
    private static void populateRadwardenOutpost(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Military crates (Dispensers + Iron Trapdoors)
        for (int i = 0; i < 1 + random.nextInt(3); i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.IRON_TRAPDOOR);
        }
        // Automated defense (Dispensers as turrets)
        if (random.nextBoolean()) {
            BlockPos pos = room.getCenterPosition();
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY() + 2, pos.getZ());
            placeProp(level, worldPos, Blocks.DISPENSER);
        }
    }
    
    private static void populateCrashbreakSalvageYard(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Trade crates (Barrels)
        for (int i = 0; i < 2 + random.nextInt(4); i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeLootContainer(level, worldPos, Blocks.BARREL,
                    STRUCTURE_LOOT_TABLES.get(StructureType.CRASHBREAK_SALVAGE_YARD), random);
        }
        // Emerald decoration
        BlockPos center = room.getCenterPosition();
        BlockPos worldCenter = origin.offset(center.getX(), center.getY(), center.getZ());
        placeProp(level, worldCenter, Blocks.EMERALD_BLOCK);
    }
    
    private static void populateSporeboundSanctum(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Bio-containment (Slime blocks + Glowstone)
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.SLIME_BLOCK);
        }
        // Healing beacons
        if (random.nextBoolean()) {
            BlockPos pos = room.getCenterPosition();
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.GLOWSTONE);
        }
    }
    
    // === EXPLORATION 1.1: WORLD POI POPULATION ===
    
    private static void populateCryogenicRuins(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Frozen equipment (Packed Ice)
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.PACKED_ICE);
        }
        // Cryo-pods (Glass + Blue Ice)
        BlockPos center = room.getCenterPosition();
        BlockPos worldCenter = origin.offset(center.getX(), center.getY(), center.getZ());
        placeProp(level, worldCenter, Blocks.BLUE_ICE);
        placeProp(level, worldCenter.above(), Blocks.GLASS);
    }
    
    private static void populateRelayStation(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Communication equipment (Redstone Lamps + Daylight Detectors)
        BlockPos pos = room.getCenterPosition();
        BlockPos worldPos = origin.offset(pos.getX(), pos.getY() + 2, pos.getZ());
        placeProp(level, worldPos, Blocks.REDSTONE_LAMP);
        placeProp(level, worldPos.above(), Blocks.DAYLIGHT_DETECTOR);
        // Copper wiring decoration
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            BlockPos randPos = room.getRandomPosition(random, 2);
            BlockPos worldRandPos = origin.offset(randPos.getX(), randPos.getY(), randPos.getZ());
            placeProp(level, worldRandPos, Blocks.EXPOSED_COPPER);
        }
    }
    
    private static void populateDerelictWorkshop(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Workbenches (Crafting Tables + Anvils)
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            BlockPos pos = room.getRandomPosition(random, 2);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.CRAFTING_TABLE);
        }
        // Repair station (Anvil)
        if (random.nextBoolean()) {
            BlockPos pos = room.getCenterPosition();
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.ANVIL);
        }
    }
    
    private static void populateAbandonedMine(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Ore veins (Coal Ore + Iron Ore)
        for (int i = 0; i < 3 + random.nextInt(5); i++) {
            BlockPos pos = room.getRandomPosition(random, 1);
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, random.nextBoolean() ? Blocks.COAL_ORE : Blocks.IRON_ORE);
        }
        // Mining equipment (Rails in shafts)
        if (room.getType() == Room.RoomType.SHAFT) {
            BlockPos pos = room.getCenterPosition();
            BlockPos worldPos = origin.offset(pos.getX(), pos.getY(), pos.getZ());
            placeProp(level, worldPos, Blocks.RAIL);
        }
    }
    
    private static void populateObservationPost(ServerLevel level, BlockPos origin, Room room, RandomSource random) {
        // Weather monitoring (Daylight Detectors + Comparators)
        BlockPos pos = room.getCenterPosition();
        BlockPos worldPos = origin.offset(pos.getX(), pos.getY() + 1, pos.getZ());
        placeProp(level, worldPos, Blocks.DAYLIGHT_DETECTOR);
        placeProp(level, worldPos.below(), Blocks.COMPARATOR);
        // Data storage (Barrels)
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            BlockPos randPos = room.getRandomPosition(random, 2);
            BlockPos worldRandPos = origin.offset(randPos.getX(), randPos.getY(), randPos.getZ());
            placeLootContainer(level, worldRandPos, Blocks.BARREL,
                    STRUCTURE_LOOT_TABLES.get(StructureType.OBSERVATION_POST), random);
        }
    }
    
    /**
     * Apply decay and damage effects to structures for post-apocalyptic aesthetic.
     * Includes wall damage, roof holes, debris, and weathering.
     */
    private static void applyDecayAndDamage(ServerLevel level, BlockPos origin,
                                              List<Room> rooms, List<Corridor> corridors,
                                              StructureType type, RandomSource random) {
        // Base damage chance varies by structure type (ruins have more damage)
        float baseDamageChance = switch (type) {
            case REACTOR_RUIN -> 0.35f;
            case ABANDONED_MINE -> 0.30f;
            case CRYOGENIC_RUINS -> 0.25f;
            case SEWER_JUNCTION -> 0.20f;
            case DERELICT_WORKSHOP -> 0.20f;
            default -> 0.15f; // Standard structures
        };
        
        // Apply damage to rooms
        for (Room room : rooms) {
            float roomDamageChance = baseDamageChance;
            
            // Dangerous rooms have more damage
            if (room.getType() == Room.RoomType.REACTOR_CORE || 
                room.getType() == Room.RoomType.CONTAINMENT) {
                roomDamageChance += 0.10f;
            }
            
            applyRoomDamage(level, origin, room, roomDamageChance, random);
        }
        
        // Add exterior debris piles
        addExteriorRubblePiles(level, origin, rooms, random);
        
        // Add interior rubble and debris
        for (Room room : rooms) {
            addInteriorDebris(level, origin, room, random);
        }
    }
    
    private static void applyRoomDamage(ServerLevel level, BlockPos origin, Room room, 
                                         float damageChance, RandomSource random) {
        // Wall damage - remove random wall blocks
        int wallDamageCount = (int) ((room.getWidth() + room.getDepth()) * 2 * damageChance * random.nextFloat());
        for (int i = 0; i < wallDamageCount; i++) {
            // Pick a random wall position
            int wall = random.nextInt(4); // 0=N, 1=S, 2=E, 3=W
            int x = room.getX() + random.nextInt(room.getWidth());
            int z = room.getZ() + random.nextInt(room.getDepth());
            int y = room.getY() + random.nextInt(room.getHeight());
            
            // Constrain to wall
            switch (wall) {
                case 0 -> z = room.getZ() - 1; // North wall
                case 1 -> z = room.getZ() + room.getDepth(); // South wall
                case 2 -> x = room.getX() + room.getWidth(); // East wall
                case 3 -> x = room.getX() - 1; // West wall
            }
            
            BlockPos damagePos = origin.offset(x, y, z);
            BlockState state = level.getBlockState(damagePos);
            
            // Only damage solid blocks (walls)
            if (AshfallInteractionRules.supportsPlacement(state) && !state.is(Blocks.AIR)) {
                // 50% chance to remove completely, 50% to replace with damaged variant
                if (random.nextBoolean()) {
                    setStructureBlock(level, damagePos, Blocks.AIR.defaultBlockState(), 2);
                } else {
                    // Replace with cracked/decayed version if possible
                    Block damagedBlock = getDamagedVariant(state.getBlock());
                    if (damagedBlock != state.getBlock()) {
                        setStructureBlock(level, damagePos, damagedBlock.defaultBlockState(), 2);
                    }
                }
            }
        }
        
        // Floor damage - potholes and cracks
        int floorDamageCount = (int) (room.getWidth() * room.getDepth() * damageChance * 0.3f);
        for (int i = 0; i < floorDamageCount; i++) {
            int x = room.getX() + random.nextInt(room.getWidth());
            int z = room.getZ() + random.nextInt(room.getDepth());
            BlockPos floorPos = origin.offset(x, room.getY() - 1, z);
            
            BlockState state = level.getBlockState(floorPos);
            if (AshfallInteractionRules.supportsPlacement(state) && random.nextFloat() < 0.5f) {
                // Create pothole
                setStructureBlock(level, floorPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                // Sometimes extend down
                if (random.nextFloat() < 0.3f) {
                    setStructureBlock(level, floorPos.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }
        
        // Ceiling/roof damage - holes and collapses
        if (damageChance > 0.20f || random.nextFloat() < 0.3f) {
            int roofDamageCount = (int) (damageChance * 5 * random.nextFloat());
            for (int i = 0; i < roofDamageCount; i++) {
                int x = room.getX() + random.nextInt(room.getWidth());
                int z = room.getZ() + random.nextInt(room.getDepth());
                
                // Create small hole in ceiling
                for (int y = room.getY() + room.getHeight() - 1; y >= room.getY(); y--) {
                    BlockPos roofPos = origin.offset(x, y, z);
                    if (!level.getBlockState(roofPos).isAir()) {
                        setStructureBlock(level, roofPos, Blocks.AIR.defaultBlockState(), 2);
                        // Sometimes place debris below
                        if (random.nextFloat() < 0.4f && y > room.getY()) {
                            BlockPos debrisPos = origin.offset(x, room.getY(), z);
                            if (level.getBlockState(debrisPos).isAir()) {
                                setStructureBlock(level, debrisPos, Blocks.GRAVEL.defaultBlockState(), 2);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Get a damaged/decayed variant of a block for weathering effects
     */
    private static Block getDamagedVariant(Block original) {
        if (original == Blocks.STONE_BRICKS) return Blocks.CRACKED_STONE_BRICKS;
        if (original == Blocks.DEEPSLATE_BRICKS) return Blocks.CRACKED_DEEPSLATE_BRICKS;
        if (original == Blocks.NETHER_BRICKS) return Blocks.CRACKED_NETHER_BRICKS;
        if (original == Blocks.POLISHED_BLACKSTONE_BRICKS) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        if (original == Blocks.STONE) return Blocks.COBBLESTONE;
        if (original == Blocks.SMOOTH_STONE) return Blocks.COBBLESTONE;
        if (original == Blocks.COBBLESTONE) return Blocks.MOSSY_COBBLESTONE;
        if (original == Blocks.STONE_BRICKS) return Blocks.MOSSY_STONE_BRICKS;
        if (original == Blocks.POLISHED_ANDESITE) return Blocks.ANDESITE;
        if (original == Blocks.POLISHED_DIORITE) return Blocks.DIORITE;
        if (original == Blocks.POLISHED_GRANITE) return Blocks.GRANITE;
        if (original == Blocks.GLASS) return Blocks.GLASS_PANE;
        if (original == Blocks.IRON_BLOCK) return Blocks.IRON_BARS;
        if (original == Blocks.COPPER_BLOCK) return Blocks.EXPOSED_COPPER;
        return original;
    }
    
    private static void addExteriorRubblePiles(ServerLevel level, BlockPos origin, 
                                                 List<Room> rooms, RandomSource random) {
        if (rooms.isEmpty()) return;
        
        // Find structure bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int baseY = rooms.get(0).getY();
        
        for (Room room : rooms) {
            minX = Math.min(minX, room.getX() - 2);
            maxX = Math.max(maxX, room.getX() + room.getWidth() + 2);
            minZ = Math.min(minZ, room.getZ() - 2);
            maxZ = Math.max(maxZ, room.getZ() + room.getDepth() + 2);
        }
        
        // Place rubble piles around perimeter
        int pileCount = 3 + random.nextInt(6);
        for (int i = 0; i < pileCount; i++) {
            // Pick random position on perimeter
            int x, z;
            if (random.nextBoolean()) {
                x = random.nextBoolean() ? minX - 1 - random.nextInt(3) : maxX + 1 + random.nextInt(3);
                z = minZ + random.nextInt(maxZ - minZ);
            } else {
                x = minX + random.nextInt(maxX - minX);
                z = random.nextBoolean() ? minZ - 1 - random.nextInt(3) : maxZ + 1 + random.nextInt(3);
            }
            
            // Build rubble pile (1-3 blocks high)
            int pileHeight = 1 + random.nextInt(3);
            for (int h = 0; h < pileHeight; h++) {
                BlockPos rubblePos = origin.offset(x, baseY - 1 + h, z);
                if (level.getBlockState(rubblePos).isAir()) {
                    Block rubbleBlock = random.nextFloat() < 0.6f ? Blocks.COBBLESTONE : Blocks.GRAVEL;
                    setStructureBlock(level, rubblePos, rubbleBlock.defaultBlockState(), 2);
                }
            }
        }
    }
    
    // ====================================================================
    // Pass 1 - Multi-floor helpers
    // ====================================================================

    /**
     * Returns the set of rooms that have another room stacked directly above
     * them (same x/z footprint, Y == this.Y + this.height).
     */
    private static java.util.Set<Room> computeStackedBelow(List<Room> rooms) {
        java.util.Set<Room> result = new java.util.HashSet<>();
        for (Room lower : rooms) {
            for (Room upper : rooms) {
                if (upper == lower) continue;
                if (upper.getFloorIndex() != lower.getFloorIndex() + 1) continue;
                // Footprints must overlap meaningfully.
                int x0 = Math.max(lower.getX(), upper.getX());
                int x1 = Math.min(lower.getX() + lower.getWidth(),
                                  upper.getX() + upper.getWidth());
                int z0 = Math.max(lower.getZ(), upper.getZ());
                int z1 = Math.min(lower.getZ() + lower.getDepth(),
                                  upper.getZ() + upper.getDepth());
                if (x1 - x0 >= 2 && z1 - z0 >= 2
                        && upper.getY() <= lower.getY() + lower.getHeight() + 1) {
                    result.add(lower);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Carve a 1-block hole in the ceiling/floor between this room and the
     * room above it, then place a ladder column spanning the gap. Skips if
     * we can't find a clear corner near a wall to anchor the ladder.
     */
    private static void carveLadderColumn(ServerLevel level, BlockPos origin,
                                            Room lower, List<Room> rooms) {
        Room upper = null;
        for (Room r : rooms) {
            if (r == lower) continue;
            if (r.getFloorIndex() != lower.getFloorIndex() + 1) continue;
            int x0 = Math.max(lower.getX(), r.getX());
            int x1 = Math.min(lower.getX() + lower.getWidth(), r.getX() + r.getWidth());
            int z0 = Math.max(lower.getZ(), r.getZ());
            int z1 = Math.min(lower.getZ() + lower.getDepth(), r.getZ() + r.getDepth());
            if (x1 - x0 >= 2 && z1 - z0 >= 2) {
                upper = r;
                break;
            }
        }
        if (upper == null) return;

        // Pick a tile inside the overlap, near a wall (lx=lower.X+1).
        int ladderX = Math.max(lower.getX() + 1, upper.getX() + 1);
        int ladderZ = Math.max(lower.getZ() + 1, upper.getZ() + 1);

        // Carve the slab between floors and column upward.
        int yStart = lower.getY() + 1;
        int yEnd = upper.getY() + 1;
        for (int y = yStart; y <= yEnd; y++) {
            BlockPos p = origin.offset(ladderX, y, ladderZ);
            setStructureBlock(level, p, Blocks.LADDER.defaultBlockState(), 2);
        }
        // Make sure the tile is open at top.
        setStructureBlock(level, origin.offset(ladderX, yEnd + 1, ladderZ),
                Blocks.AIR.defaultBlockState(), 2);
    }

    // ====================================================================
    // Pass 3 - Furniture density system
    // ====================================================================

    /**
     * A reusable furniture piece. Footprint is given in floor-plan blocks;
     * the placer is responsible for laying every block including any vertical
     * column. The piece is anchored at its NW corner (lowest x/z, room floor Y).
     */
    private record FurniturePiece(String name, int footprintX, int footprintZ,
                                  FurniturePlacer placer) { }

    @FunctionalInterface
    private interface FurniturePlacer {
        void place(ServerLevel level, BlockPos anchor, RandomSource random);
    }

    private static final Map<String, FurniturePiece> FURNITURE = new HashMap<>();
    private static final Map<Room.RoomType, List<String>> ROOM_RECIPES = new HashMap<>();

    static {
        FURNITURE.put("desk_with_terminal", new FurniturePiece("desk_with_terminal", 1, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(), 2);
                    setStructureBlock(level, p.above(), Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
                }));

        FURNITURE.put("lab_table", new FurniturePiece("lab_table", 2, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 2);
                    setStructureBlock(level, p.east(), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 2);
                    if (rnd.nextFloat() < 0.5f) {
                        setStructureBlock(level, p.above(), Blocks.BREWING_STAND.defaultBlockState(), 2);
                    } else {
                        setStructureBlock(level, p.east().above(), Blocks.CAULDRON.defaultBlockState(), 2);
                    }
                }));

        FURNITURE.put("server_rack", new FurniturePiece("server_rack", 1, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.BOOKSHELF.defaultBlockState(), 2);
                    setStructureBlock(level, p.above(), Blocks.BOOKSHELF.defaultBlockState(), 2);
                    setStructureBlock(level, p.above(2), rnd.nextFloat() < 0.6f
                            ? Blocks.REDSTONE_LAMP.defaultBlockState()
                            : Blocks.COBWEB.defaultBlockState(), 2);
                }));

        FURNITURE.put("bunk_bed", new FurniturePiece("bunk_bed", 1, 2,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.RED_BED.defaultBlockState(), 2);
                    setStructureBlock(level, p.south(), Blocks.RED_BED.defaultBlockState(), 2);
                    setStructureBlock(level, p.above(), Blocks.OAK_PLANKS.defaultBlockState(), 2);
                    setStructureBlock(level, p.south().above(), Blocks.OAK_PLANKS.defaultBlockState(), 2);
                }));

        FURNITURE.put("weapon_rack", new FurniturePiece("weapon_rack", 1, 1,
                (level, p, rnd) -> {
                    placeLootContainer(level, p, Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.MILITARY_VAULT), rnd);
                    setStructureBlock(level, p.above(), Blocks.ANVIL.defaultBlockState(), 2);
                }));

        FURNITURE.put("chemical_drum", new FurniturePiece("chemical_drum", 1, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.CAULDRON.defaultBlockState(), 2);
                    setStructureBlock(level, p.above(), Blocks.TINTED_GLASS.defaultBlockState(), 2);
                }));

        FURNITURE.put("medkit_cabinet", new FurniturePiece("medkit_cabinet", 1, 1,
                (level, p, rnd) -> {
                    placeLootContainer(level, p, Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.BIO_LAB), rnd);
                    setStructureBlock(level, p.above(), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
                }));

        FURNITURE.put("crate_stack", new FurniturePiece("crate_stack", 2, 1,
                (level, p, rnd) -> {
                    placeLootContainer(level, p, Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.DERELICT_WORKSHOP), rnd);
                    placeLootContainer(level, p.east(), Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.DERELICT_WORKSHOP), rnd);
                    if (rnd.nextFloat() < 0.6f) {
                        placeLootContainer(level, p.above(), Blocks.BARREL, STRUCTURE_LOOT_TABLES.get(StructureType.DERELICT_WORKSHOP), rnd);
                    }
                }));

        FURNITURE.put("workbench", new FurniturePiece("workbench", 1, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
                }));

        FURNITURE.put("broken_furniture", new FurniturePiece("broken_furniture", 1, 1,
                (level, p, rnd) -> {
                    Block b = switch (rnd.nextInt(4)) {
                        case 0 -> Blocks.OAK_FENCE;
                        case 1 -> Blocks.COBWEB;
                        case 2 -> Blocks.GRAVEL;
                        default -> Blocks.OAK_PLANKS;
                    };
                    setStructureBlock(level, p, b.defaultBlockState(), 2);
                }));

        FURNITURE.put("control_panel", new FurniturePiece("control_panel", 2, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.DAYLIGHT_DETECTOR.defaultBlockState(), 2);
                    setStructureBlock(level, p.east(), Blocks.OBSERVER.defaultBlockState(), 2);
                }));

        FURNITURE.put("planter", new FurniturePiece("planter", 1, 1,
                (level, p, rnd) -> {
                    setStructureBlock(level, p, Blocks.PODZOL.defaultBlockState(), 2);
                    setStructureBlock(level, p.above(), rnd.nextFloat() < 0.5f
                            ? Blocks.FERN.defaultBlockState()
                            : Blocks.WARPED_ROOTS.defaultBlockState(), 2);
                }));

        // Recipe lists per room type (drives populateRoomFurniture)
        ROOM_RECIPES.put(Room.RoomType.LABORATORY,
                List.of("lab_table", "chemical_drum", "desk_with_terminal", "medkit_cabinet"));
        ROOM_RECIPES.put(Room.RoomType.MEDBAY,
                List.of("medkit_cabinet", "lab_table", "desk_with_terminal"));
        ROOM_RECIPES.put(Room.RoomType.SERVER_ROOM,
                List.of("server_rack", "server_rack", "desk_with_terminal", "control_panel"));
        ROOM_RECIPES.put(Room.RoomType.CONTROL_ROOM,
                List.of("control_panel", "desk_with_terminal", "server_rack"));
        ROOM_RECIPES.put(Room.RoomType.ARMORY,
                List.of("weapon_rack", "weapon_rack", "crate_stack"));
        ROOM_RECIPES.put(Room.RoomType.BARRACKS,
                List.of("bunk_bed", "bunk_bed", "crate_stack", "workbench"));
        ROOM_RECIPES.put(Room.RoomType.STORAGE,
                List.of("crate_stack", "crate_stack", "broken_furniture"));
        ROOM_RECIPES.put(Room.RoomType.WORKSHOP,
                List.of("workbench", "weapon_rack", "crate_stack", "broken_furniture"));
        ROOM_RECIPES.put(Room.RoomType.GREENHOUSE,
                List.of("planter", "planter", "planter", "lab_table"));
        ROOM_RECIPES.put(Room.RoomType.MARKET,
                List.of("crate_stack", "workbench", "desk_with_terminal", "weapon_rack"));
        ROOM_RECIPES.put(Room.RoomType.OFFICE,
                List.of("desk_with_terminal", "desk_with_terminal", "control_panel"));
        ROOM_RECIPES.put(Room.RoomType.RECEPTION,
                List.of("desk_with_terminal", "broken_furniture"));
        ROOM_RECIPES.put(Room.RoomType.MAIN_HALL,
                List.of("desk_with_terminal", "crate_stack", "broken_furniture"));
        ROOM_RECIPES.put(Room.RoomType.ENTRANCE,
                List.of("crate_stack", "broken_furniture"));
        ROOM_RECIPES.put(Room.RoomType.HALLWAY,
                List.of("broken_furniture"));
    }

    /**
     * Furniture density pass - places reusable furniture pieces on the room
     * floor using a 2-D occupancy grid. Density target is roughly 0.25-0.4 of
     * floor area (i.e. 5-10 pieces in a 5x5 room).
     *
     * Placement is biased 60% to wall-adjacent cells and 40% to interior cells
     * so rooms feel "lined" with equipment rather than randomly littered.
     *
     * Skips rooms with no recipe (e.g. STAIRCASE, REACTOR_CORE which has its
     * own custom population in populateReactorRuin).
     */
    private static void populateRoomFurniture(ServerLevel level, BlockPos origin,
                                                Room room, RandomSource random) {
        List<String> recipe = ROOM_RECIPES.get(room.getType());
        if (recipe == null || recipe.isEmpty()) return;
        if (room.getWidth() < 3 || room.getDepth() < 3) return;

        // Occupancy grid in room-local coordinates. true = cell taken.
        int gw = room.getWidth();
        int gd = room.getDepth();
        boolean[][] taken = new boolean[gw][gd];

        // Mark the centre column open (avoids blocking walkways in tiny rooms).
        // For larger rooms, we let placement happen anywhere except 1-block
        // perimeter walls (handled by clamping below).

        int floorArea = gw * gd;
        int targetPieces = Math.max(2, (int) (floorArea * 0.30f));
        int placed = 0;
        int attempts = 0;
        int maxAttempts = targetPieces * 6;

        while (placed < targetPieces && attempts < maxAttempts) {
            attempts++;
            String pieceName = recipe.get(random.nextInt(recipe.size()));
            FurniturePiece piece = FURNITURE.get(pieceName);
            if (piece == null) continue;

            // Bias 60% wall-adjacent.
            boolean wallBias = random.nextFloat() < 0.60f;
            int lx, lz;
            if (wallBias) {
                // Pick a random side and snug to it.
                int side = random.nextInt(4);
                switch (side) {
                    case 0 -> { lx = 0; lz = random.nextInt(gd); }                       // west
                    case 1 -> { lx = gw - piece.footprintX(); lz = random.nextInt(gd); } // east
                    case 2 -> { lx = random.nextInt(gw); lz = 0; }                       // north
                    default -> { lx = random.nextInt(gw); lz = gd - piece.footprintZ();} // south
                }
            } else {
                lx = random.nextInt(Math.max(1, gw - piece.footprintX()));
                lz = random.nextInt(Math.max(1, gd - piece.footprintZ()));
            }

            // Bounds check + occupancy check.
            if (lx < 0 || lz < 0) continue;
            if (lx + piece.footprintX() > gw) continue;
            if (lz + piece.footprintZ() > gd) continue;
            if (!isFootprintFree(taken, lx, lz, piece.footprintX(), piece.footprintZ())) continue;

            // Place - anchor at (room.x + lx, room.y, room.z + lz).
            BlockPos anchor = origin.offset(room.getX() + lx, room.getY(), room.getZ() + lz);

            // Sanity: don't overwrite anything non-air at the anchor (existing
            // populate* may have placed themed props there already).
            if (!level.getBlockState(anchor).isAir()) {
                markFootprint(taken, lx, lz, piece.footprintX(), piece.footprintZ());
                continue;
            }

            piece.placer().place(level, anchor, random);
            markFootprint(taken, lx, lz, piece.footprintX(), piece.footprintZ());
            placed++;
        }
    }

    private static boolean isFootprintFree(boolean[][] taken, int lx, int lz, int w, int d) {
        for (int x = lx; x < lx + w; x++) {
            for (int z = lz; z < lz + d; z++) {
                if (taken[x][z]) return false;
            }
        }
        return true;
    }

    private static void markFootprint(boolean[][] taken, int lx, int lz, int w, int d) {
        for (int x = lx; x < lx + w; x++) {
            for (int z = lz; z < lz + d; z++) {
                taken[x][z] = true;
            }
        }
    }

    /**
     * Exterior approach pass (Pass 6).
     *
     * Three sub-passes around the entrance room:
     *   1. A 2-wide path of palette `decayed` blocks extending ~6 blocks out
     *      from the entrance face along -Z. Reads as a road/walkway.
     *   2. 30% chance of a small ruined outbuilding (4x3x4 shell, no roof,
     *      one chest with structure loot) placed off to the side.
     *   3. A rubble halo: 8-14 scattered palette `decayed` / iron-bar / cobweb
     *      placements within a 7-block ring around the entrance, skipping any
     *      position inside a room footprint so we don't stomp the build.
     */
    private static void buildExteriorApproach(ServerLevel level, BlockPos origin,
                                                List<Room> rooms, StructureType type,
                                                StructureType.BlockPalette palette,
                                                RandomSource random) {
        if (rooms.isEmpty()) return;
        Room entrance = rooms.get(0);
        Block path = pathBlockForType(type, palette);

        // 1. Approach path: 2-wide strip along -Z from entrance airlock.
        int pathStartZ = entrance.getZ() - 4; // outside the airlock (which extends to Z-3)
        int cx = entrance.getCenterX();
        for (int dz = 0; dz < 6; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos p = origin.offset(cx + dx, entrance.getY() - 1, pathStartZ - dz);
                if (isInsideAnyRoom(rooms, cx + dx, entrance.getY() - 1, pathStartZ - dz)) continue;
                setStructureBlock(level, p, path.defaultBlockState(), 2);
                // Clear above the path so it's walkable.
                BlockPos above = p.above();
                if (!level.getBlockState(above).isAir()
                        && level.getBlockState(above).getBlock() != Blocks.GLOWSTONE) {
                    setStructureBlock(level, above, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // 2. Outbuilding: 30% chance. Place to the east of the entrance path.
        if (random.nextFloat() < 0.30f) {
            buildRuinedOutbuilding(level, origin, rooms, type, palette,
                    cx + 5, entrance.getY(), pathStartZ - 2, random);
        }

        // 3. Rubble halo: scatter around the perimeter.
        int rubbleCount = 8 + random.nextInt(7);
        for (int i = 0; i < rubbleCount; i++) {
            int rx = cx + random.nextInt(15) - 7;
            int rz = entrance.getZ() + random.nextInt(15) - 11;
            if (isInsideAnyRoom(rooms, rx, entrance.getY(), rz)) continue;
            BlockPos p = origin.offset(rx, entrance.getY() - 1, rz);
            BlockPos above = p.above();
            if (!level.getBlockState(above).isAir()) continue;

            float roll = random.nextFloat();
            Block rubble;
            if (roll < 0.55f) rubble = palette.decayed();
            else if (roll < 0.80f) rubble = Blocks.IRON_BARS;
            else if (roll < 0.93f) rubble = Blocks.COBWEB;
            else rubble = Blocks.GRAVEL;
            setStructureBlock(level, above, rubble.defaultBlockState(), 2);
        }
    }

    private static boolean isInsideAnyRoom(List<Room> rooms, int x, int y, int z) {
        for (Room r : rooms) {
            if (x >= r.getX() - 1 && x < r.getX() + r.getWidth() + 1
                    && z >= r.getZ() - 1 && z < r.getZ() + r.getDepth() + 1
                    && y >= r.getY() - 1 && y < r.getY() + r.getHeight() + 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 4x3x4 roofless ruined shell with a single loot chest. Uses the palette's
     * decayed block for walls so it reads as ruined sibling architecture.
     */
    private static void buildRuinedOutbuilding(ServerLevel level, BlockPos origin,
                                                 List<Room> rooms, StructureType type,
                                                 StructureType.BlockPalette palette,
                                                 int x0, int y0, int z0, RandomSource random) {
        int w = 4, d = 4, h = 3;
        if (isInsideAnyRoom(rooms, x0 + w / 2, y0, z0 + d / 2)) return;

        // Walls (with ~30% missing blocks for ruined feel).
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                boolean isPerimeter = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
                if (!isPerimeter) continue;
                for (int dy = 0; dy < h; dy++) {
                    if (random.nextFloat() < 0.30f) continue; // ruin gap
                    BlockPos p = origin.offset(x0 + dx, y0 + dy, z0 + dz);
                    Block b = random.nextFloat() < 0.5f ? palette.decayed() : palette.secondary();
                    setStructureBlock(level, p, b.defaultBlockState(), 2);
                }
            }
        }
        // Floor.
        for (int dx = 1; dx < w - 1; dx++) {
            for (int dz = 1; dz < d - 1; dz++) {
                BlockPos p = origin.offset(x0 + dx, y0 - 1, z0 + dz);
                setStructureBlock(level, p, palette.primary().defaultBlockState(), 2);
            }
        }
        // Single loot chest in the corner.
        BlockPos chestPos = origin.offset(x0 + 1, y0, z0 + 1);
        if (level.getBlockState(chestPos).isAir()) {
            ResourceKey<LootTable> lootTable = STRUCTURE_LOOT_TABLES.get(type);
            if (lootTable != null) {
                placeLootContainer(level, chestPos, Blocks.CHEST, lootTable, random);
            }
        }
    }

    private static Block pathBlockForType(StructureType type, StructureType.BlockPalette palette) {
        return switch (type) {
            case BIO_LAB, SPOREBOUND_SANCTUM -> Blocks.PACKED_MUD;
            case CRYOGENIC_RUINS -> Blocks.PACKED_ICE;
            case ABANDONED_MINE -> Blocks.GRAVEL;
            case SEWER_JUNCTION -> Blocks.MOSSY_COBBLESTONE;
            default -> Blocks.CRACKED_STONE_BRICKS;
        };
    }

    /**
     * Facade pass - punches windows on exterior wall segments and hangs a
     * structure-name sign over the entrance. Runs after walls + ceilings have
     * been laid so the wall fabric exists to be carved into.
     *
     * "Exterior" detection is heuristic: a wall face is treated as exterior if,
     * for the room in question, no other room's bounding box overlaps the strip
     * one block outward from that face. This catches false positives only when
     * two rooms are diagonally adjacent (rare with BSP output) and is cheap.
     */
    private static void applyFacadePass(ServerLevel level, BlockPos origin,
                                         List<Room> rooms, StructureType type,
                                         StructureType.BlockPalette palette,
                                         RandomSource random) {
        if (rooms.isEmpty()) return;

        Block windowBlock = windowGlassForType(type);
        Block lintelBlock = palette.accent();

        for (Room room : rooms) {
            if (room.getWidth() < 4 || room.getDepth() < 4) continue;

            // North face (z = room.Z - 1), outward = -Z
            if (isFaceExterior(rooms, room, Direction2D.NORTH)) {
                punchWindowRow(level, origin, room, Direction2D.NORTH,
                        windowBlock, lintelBlock, random);
            }
            if (isFaceExterior(rooms, room, Direction2D.SOUTH)) {
                punchWindowRow(level, origin, room, Direction2D.SOUTH,
                        windowBlock, lintelBlock, random);
            }
            if (isFaceExterior(rooms, room, Direction2D.WEST)) {
                punchWindowRow(level, origin, room, Direction2D.WEST,
                        windowBlock, lintelBlock, random);
            }
            if (isFaceExterior(rooms, room, Direction2D.EAST)) {
                punchWindowRow(level, origin, room, Direction2D.EAST,
                        windowBlock, lintelBlock, random);
            }
        }

        // Hang a sign above the entrance airlock built by buildEntrance.
        Room entrance = rooms.get(0);
        BlockPos signLocal = new BlockPos(
                entrance.getCenterX(),
                entrance.getY() + 3,
                entrance.getZ() - 3);
        BlockPos signWorld = origin.offset(signLocal.getX(), signLocal.getY(), signLocal.getZ());
        if (level.getBlockState(signWorld).isAir()) {
            setStructureBlock(level, signWorld, Blocks.OAK_HANGING_SIGN.defaultBlockState(), 2);
        }
    }

    private enum Direction2D { NORTH, SOUTH, WEST, EAST }

    /**
     * A face is exterior if no other room's footprint (with 1-block wall
     * padding) overlaps the strip one block outward from the face.
     */
    private static boolean isFaceExterior(List<Room> rooms, Room room, Direction2D face) {
        int rx0 = room.getX();
        int rx1 = room.getX() + room.getWidth();
        int rz0 = room.getZ();
        int rz1 = room.getZ() + room.getDepth();

        // Strip one block outside the face.
        int sx0, sx1, sz0, sz1;
        switch (face) {
            case NORTH -> { sx0 = rx0 - 1; sx1 = rx1 + 1; sz0 = rz0 - 2; sz1 = rz0 - 1; }
            case SOUTH -> { sx0 = rx0 - 1; sx1 = rx1 + 1; sz0 = rz1; sz1 = rz1 + 1; }
            case WEST  -> { sx0 = rx0 - 2; sx1 = rx0 - 1; sz0 = rz0 - 1; sz1 = rz1 + 1; }
            case EAST  -> { sx0 = rx1; sx1 = rx1 + 1; sz0 = rz0 - 1; sz1 = rz1 + 1; }
            default -> { return false; }
        }

        for (Room other : rooms) {
            if (other == room) continue;
            int ox0 = other.getX() - 1;
            int ox1 = other.getX() + other.getWidth() + 1;
            int oz0 = other.getZ() - 1;
            int oz1 = other.getZ() + other.getDepth() + 1;
            // Only consider rooms on the same Y level.
            if (other.getY() != room.getY()) continue;
            boolean xOverlap = ox0 < sx1 && ox1 > sx0;
            boolean zOverlap = oz0 < sz1 && oz1 > sz0;
            if (xOverlap && zOverlap) return false;
        }
        return true;
    }

    /**
     * Punch 2-3 windows along an exterior wall face. Each window is 1x2 (Y+1,Y+2)
     * with an accent lintel at Y+3 (clamped to ceiling height). Skips the
     * entrance face if the room is the entrance to avoid stomping the airlock.
     */
    private static void punchWindowRow(ServerLevel level, BlockPos origin, Room room,
                                        Direction2D face, Block windowBlock, Block lintelBlock,
                                        RandomSource random) {
        // Skip the entrance face on the entrance room - buildEntrance owns that wall.
        if (room.isEntrance() && face == Direction2D.NORTH) return;

        int wallY1 = room.getY() + 1;
        int wallY2 = room.getY() + 2;
        int lintelY = Math.min(room.getY() + 3, room.getY() + room.getHeight() - 1);

        // Walk the face and pick window slots (every 3 blocks, starting offset 1).
        boolean alongX = face == Direction2D.NORTH || face == Direction2D.SOUTH;
        int len = alongX ? room.getWidth() : room.getDepth();
        int targetCount = Math.min(3, Math.max(1, len / 4));
        int placed = 0;

        for (int i = 1; i < len - 1 && placed < targetCount; i += 3) {
            // Skip the middle block (likely doorway/connection point)
            if (Math.abs(i - len / 2) <= 1 && random.nextFloat() < 0.5f) continue;

            int wx, wz;
            if (face == Direction2D.NORTH) { wx = room.getX() + i; wz = room.getZ() - 1; }
            else if (face == Direction2D.SOUTH) { wx = room.getX() + i; wz = room.getZ() + room.getDepth(); }
            else if (face == Direction2D.WEST) { wx = room.getX() - 1; wz = room.getZ() + i; }
            else /* EAST */ { wx = room.getX() + room.getWidth(); wz = room.getZ() + i; }

            BlockPos lower = origin.offset(wx, wallY1, wz);
            BlockPos upper = origin.offset(wx, wallY2, wz);
            BlockPos lintel = origin.offset(wx, lintelY, wz);

            // Only carve if both target positions are currently solid wall
            // (don't break through pillars or pre-existing openings).
            if (level.getBlockState(lower).isAir() || level.getBlockState(upper).isAir()) continue;

            setStructureBlock(level, lower, windowBlock.defaultBlockState(), 2);
            setStructureBlock(level, upper, windowBlock.defaultBlockState(), 2);
            if (lintelY > wallY2 && !level.getBlockState(lintel).isAir()) {
                setStructureBlock(level, lintel, lintelBlock.defaultBlockState(), 2);
            }
            placed++;
        }
    }

    private static Block windowGlassForType(StructureType type) {
        return switch (type) {
            case BIO_LAB, SPOREBOUND_SANCTUM -> Blocks.TINTED_GLASS;
            case CRYOGENIC_RUINS -> Blocks.LIGHT_BLUE_STAINED_GLASS;
            case REACTOR_RUIN -> Blocks.YELLOW_STAINED_GLASS;
            case DATA_CENTER, RELAY_STATION, SATELLITE_ARRAY,
                 OBSERVATION_POST -> Blocks.WHITE_STAINED_GLASS;
            case SEWER_JUNCTION, ABANDONED_MINE -> Blocks.IRON_BARS;
            default -> Blocks.GLASS_PANE;
        };
    }

    /**
     * Place interior lighting keyed to structure palette.
     * Density: ~1 light per 25 floor-area blocks. 30% of slots leave a broken
     * fixture (cobweb / soul fire) for ruin aesthetic.
     *
     * Lights are placed at wall Y+2 against an interior wall block, so the
     * fixture is recessed into the room rather than floating mid-air.
     */
    private static void placeRoomLighting(ServerLevel level, BlockPos origin, Room room,
                                           StructureType type, RandomSource random) {
        if (room.getWidth() < 3 || room.getDepth() < 3) return;

        Block lightBlock = lightForType(type);
        Block brokenBlock = brokenLightForType(type);

        int floorArea = room.getWidth() * room.getDepth();
        int lightCount = Math.max(1, floorArea / 25);
        int lightY = room.getY() + Math.min(2, Math.max(1, room.getHeight() - 2));

        // Candidate positions: 1 block inside each wall, evenly spaced along it.
        List<BlockPos> candidates = new ArrayList<>();
        int wx0 = room.getX();
        int wx1 = room.getX() + room.getWidth() - 1;
        int wz0 = room.getZ();
        int wz1 = room.getZ() + room.getDepth() - 1;

        for (int x = wx0 + 1; x < wx1; x += 3) {
            candidates.add(new BlockPos(x, lightY, wz0));
            candidates.add(new BlockPos(x, lightY, wz1));
        }
        for (int z = wz0 + 1; z < wz1; z += 3) {
            candidates.add(new BlockPos(wx0, lightY, z));
            candidates.add(new BlockPos(wx1, lightY, z));
        }
        // Add ceiling-center mount for larger rooms
        if (floorArea > 35) {
            int ceilingY = room.getY() + room.getHeight() - 1;
            candidates.add(new BlockPos(room.getCenterX(), ceilingY, room.getCenterZ()));
        }

        // Shuffle and pick the first lightCount that are placeable
        java.util.Collections.shuffle(candidates, new java.util.Random(random.nextLong()));
        int placed = 0;
        for (BlockPos local : candidates) {
            if (placed >= lightCount) break;
            BlockPos worldPos = origin.offset(local.getX(), local.getY(), local.getZ());
            if (!level.getBlockState(worldPos).isAir()) continue;

            boolean broken = random.nextFloat() < 0.30f;
            Block toPlace = broken ? brokenBlock : lightBlock;
            setStructureBlock(level, worldPos, toPlace.defaultBlockState(), 2);
            placed++;
        }
    }

    private static Block lightForType(StructureType type) {
        return switch (type) {
            case BIO_LAB, SPOREBOUND_SANCTUM -> Blocks.SOUL_LANTERN;
            case CRYOGENIC_RUINS -> Blocks.SEA_LANTERN;
            case DATA_CENTER, REACTOR_RUIN, RELAY_STATION, SATELLITE_ARRAY,
                 OBSERVATION_POST -> Blocks.REDSTONE_LAMP;
            case SEWER_JUNCTION -> Blocks.SHROOMLIGHT;
            case ABANDONED_MINE -> Blocks.TORCH;
            default -> Blocks.LANTERN;
        };
    }

    private static Block brokenLightForType(StructureType type) {
        // Pair each light with a thematically-broken counterpart.
        return switch (type) {
            case BIO_LAB, SPOREBOUND_SANCTUM -> Blocks.COBWEB;
            case CRYOGENIC_RUINS -> Blocks.PACKED_ICE;
            case REACTOR_RUIN -> Blocks.SOUL_FIRE;
            case SEWER_JUNCTION -> Blocks.COBWEB;
            default -> Blocks.COBWEB;
        };
    }

    private static void addInteriorDebris(ServerLevel level, BlockPos origin, Room room,
                                           RandomSource random) {
        int debrisCount = random.nextInt(4);
        for (int i = 0; i < debrisCount; i++) {
            int x = room.getX() + random.nextInt(room.getWidth());
            int z = room.getZ() + random.nextInt(room.getDepth());
            BlockPos debrisPos = origin.offset(x, room.getY(), z);
            
            if (level.getBlockState(debrisPos).isAir()) {
                Block debrisBlock = switch (random.nextInt(5)) {
                    case 0 -> Blocks.GRAVEL;
                    case 1 -> Blocks.COBBLESTONE;
                    case 2 -> Blocks.MOSSY_COBBLESTONE;
                    case 3 -> Blocks.STONE;
                    default -> Blocks.DIRT;
                };
                setStructureBlock(level, debrisPos, debrisBlock.defaultBlockState(), 2);
            }
        }
    }

    private static GuardianSiteTheme guardianTheme(BiomeGuardianProfile profile) {
        GuardianSiteTheme theme = GUARDIAN_THEMES.get(profile.bossPath());
        if (theme == null) {
            throw new IllegalStateException("Missing guardian site theme for " + profile.bossPath());
        }
        return theme;
    }

    private static Supplier<Block> block(Block block) {
        return () -> block;
    }

    private static int clampInt(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private enum GuardianEntranceForm {
        RESCUE_HATCH,
        BUNKER_DOOR,
        SUBWAY_STAIR,
        FREIGHT_LIFT,
        TOXIC_SINKHOLE,
        IMPACT_BREACH,
        REACTOR_HATCH,
        FROZEN_SHAFT,
        NEXUS_BREACH
    }

    private enum GuardianDescentForm {
        LADDER_SHAFT,
        STAIR_TUNNEL,
        LIFT_SHAFT,
        SINKHOLE,
        BREACH_SHAFT
    }

    private record GuardianSiteLayout(
            Room entranceRoom,
            Room bossRoom,
            boolean dedicatedBossChamber,
            String guardianId
    ) {}

    private record GuardianAccessCandidate(BlockPos entrance, BlockPos roomEntry, int score) {}

    private record GuardianRouteReport(
            BlockPos entrance,
            BlockPos roomEntry,
            boolean reached,
            boolean fallbackRoute
    ) {}

    private record GuardianSiteTheme(
            GuardianEntranceForm entranceForm,
            GuardianDescentForm descentForm,
            int surfaceRadius,
            float scatterDensity,
            Supplier<Block> floorBlock,
            Supplier<Block> trimBlock,
            Supplier<Block> accentBlock,
            Supplier<Block> hazardBlock,
            Supplier<Block> lightBlock,
            Supplier<Block> coreBlock,
            Supplier<Block> propBlock
    ) {
        Block floor() {
            return floorBlock.get();
        }

        Block trim() {
            return trimBlock.get();
        }

        Block accent() {
            return accentBlock.get();
        }

        Block hazard() {
            return hazardBlock.get();
        }

        Block light() {
            return lightBlock.get();
        }

        Block core() {
            return coreBlock.get();
        }

        Block prop() {
            return propBlock.get();
        }
    }

    private record BiomeStructureProfile(
            StructureType mainStructure,
            Set<StructureType> dungeonStructures,
            Supplier<EntityType<? extends BiomeBossEntity>> bossType
    ) {}
}

