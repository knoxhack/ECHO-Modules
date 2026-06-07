package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.item.ItemStack;

public class RouteTerrainGenerator extends ChunkGenerator {
    public static final MapCodec<RouteTerrainGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Route.CODEC.fieldOf("route").forGetter(RouteTerrainGenerator::route),
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
    ).apply(instance, RouteTerrainGenerator::new));

    private static final int BASE_Y = 58;
    private static final int SITE_SPACING_BASE = 7;
    private final Route route;

    public RouteTerrainGenerator(Route route, BiomeSource biomeSource) {
        super(biomeSource);
        this.route = route;
    }

    public Route route() {
        return route;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return ModWorldgen.ROUTE_TERRAIN.get();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getBlockX(localX);
                int worldZ = chunkPos.getBlockZ(localZ);
                int top = topHeight(route, worldX, worldZ);
                if (top <= BASE_Y) {
                    continue;
                }
                for (int y = BASE_Y; y <= top; y++) {
                    BlockState state = stateFor(route, worldX, y, worldZ, top);
                    pos.set(localX, y, localZ);
                    chunk.setBlockState(pos, state);
                    ocean.update(localX, y, localZ, state);
                    surface.update(localX, y, localZ, state);
                }
            }
        }

        placeRouteFeatures(chunk, chunkPos, route);
        placeDeepSite(chunk, chunkPos, route);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        placeSurfaceDetails(chunk, chunk.getPos(), route);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
        carveRouteVoids(chunk, chunk.getPos(), route);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return switch (route) {
            case ORBIT, NEXUS -> 96;
            case MOON, MARS, EUROPA, SATURN, TITAN -> 72;
        };
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return Math.max(level.getMinY(), topHeight(route, x, z) + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        int top = topHeight(route, x, z);
        for (int y = BASE_Y; y <= top && y - level.getMinY() < states.length; y++) {
            int index = y - level.getMinY();
            if (index >= 0) {
                states[index] = stateFor(route, x, y, z, top);
            }
        }
        for (int i = 0; i < states.length; i++) {
            if (states[i] == null) {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(level.getMinY(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
        lines.add("ECHO-7 route terrain: " + route.getSerializedName());
    }

    public static int topHeight(Route route, int x, int z) {
        int wave = wave(x, z);
        int radial = Math.abs(x % 41 - 20) + Math.abs(z % 41 - 20);
        return switch (route) {
            case ORBIT -> debrisBand(x, z) ? 92 + Math.floorMod(wave, 4) : BASE_Y - 1;
            case MOON -> Math.max(60, 65 + wave / 2 - (scarTrench(x, z) ? 4 : radial % 5 == 0 ? 2 : 0));
            case MARS -> Math.max(60, 66 + wave + (basaltRidge(x, z) ? 4 : 0) - (pressureCavern(x, z) ? 2 : 0));
            case EUROPA -> Math.max(60, 64 + wave / 2 + (thermalPocket(x, z) ? 2 : 0) - (iceFracture(x, z) ? 2 : 0));
            case SATURN -> saturnRingPlate(x, z) ? Math.max(62, 67 + wave / 2 + (ringSpoke(x, z) ? 3 : 0)) : BASE_Y - 1;
            case TITAN -> Math.max(60, 65 + wave / 2 + (methaneRidge(x, z) ? 2 : 0) - (methaneSink(x, z) ? 3 : 0));
            case NEXUS -> nexusIsland(x, z) || nexusBridge(x, z) ? 78 + wave + (nexusBridge(x, z) ? -2 : 0) : BASE_Y - 1;
        };
    }

    public static BlockState landmarkBlock(Route route) {
        return switch (route) {
            case ORBIT -> ModBlocks.SIGNAL_RELAY.get().defaultBlockState();
            case MOON -> ModBlocks.SURVEY_MARKER.get().defaultBlockState();
            case MARS -> ModBlocks.SIGNAL_RELAY.get().defaultBlockState();
            case EUROPA -> ModBlocks.THERMAL_VENT.get().defaultBlockState();
            case SATURN -> ModBlocks.SATURN_RING_RELAY.get().defaultBlockState();
            case TITAN -> ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState();
            case NEXUS -> ModBlocks.NEXUS_ANCHOR.get().defaultBlockState();
        };
    }

    public static BlockState routeObjectiveBlock(Route route) {
        return switch (route) {
            case ORBIT -> ModBlocks.STATION_RELAY_NODE.get().defaultBlockState();
            case MOON -> ModBlocks.HELIUM_EXTRACTOR_NODE.get().defaultBlockState();
            case MARS -> ModBlocks.MARS_PRESSURE_CONSOLE.get().defaultBlockState();
            case EUROPA -> ModBlocks.EUROPA_THERMAL_ARRAY.get().defaultBlockState();
            case SATURN -> ModBlocks.SATURN_RING_RELAY.get().defaultBlockState();
            case TITAN -> ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState();
            case NEXUS -> ModBlocks.NEXUS_ANCHOR.get().defaultBlockState();
        };
    }

    public static List<ItemStack> cacheItems(Route route) {
        return cacheItems(route, 0);
    }

    public static List<ItemStack> cacheItems(Route route, int variant) {
        int normalized = Math.floorMod(variant, 3);
        return switch (route) {
            case ORBIT -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.ORBIT_SURVEY_DATA.get()), new ItemStack(ModItems.STATION_RELAY_FUSE.get()), new ItemStack(ModItems.NAVIGATION_CHIP.get()), new ItemStack(ModItems.OXYGEN_CANISTER.get(), 2), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
                case 2 -> List.of(new ItemStack(ModItems.ORBIT_SURVEY_DATA.get()), new ItemStack(ModItems.STATION_RELAY_FUSE.get()), new ItemStack(ModBlocks.BROKEN_SOLAR_PANEL.get(), 3), new ItemStack(ModItems.VACUUM_CIRCUIT.get()), new ItemStack(ModItems.OXYGEN_CANISTER.get()));
                default -> List.of(new ItemStack(ModItems.ORBIT_SURVEY_DATA.get()), new ItemStack(ModItems.STATION_RELAY_FUSE.get()), new ItemStack(ModItems.VACUUM_CIRCUIT.get(), 2), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
            };
            case MOON -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.LUNAR_CORE_SAMPLE.get()), new ItemStack(ModItems.HELIUM_EXTRACTOR_CORE.get()), new ItemStack(ModItems.HELIUM_3_CELL.get()), new ItemStack(ModItems.LUNAR_TITANIUM.get(), 2), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get()));
                case 2 -> List.of(new ItemStack(ModItems.LUNAR_CORE_SAMPLE.get()), new ItemStack(ModItems.HELIUM_EXTRACTOR_CORE.get()), new ItemStack(ModItems.LUNAR_TITANIUM.get()), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 3), new ItemStack(ModItems.NEXUS_DUST.get(), 2));
                default -> List.of(new ItemStack(ModItems.LUNAR_CORE_SAMPLE.get()), new ItemStack(ModItems.HELIUM_EXTRACTOR_CORE.get()), new ItemStack(ModItems.LUNAR_TITANIUM.get(), 2), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2));
            };
            case MARS -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.MARTIAN_PRESSURE_VALVE.get()), new ItemStack(ModItems.PRESSURE_REGULATOR.get()), new ItemStack(ModItems.OXYGEN_BOOSTER.get()), new ItemStack(ModItems.MARTIAN_SILICA.get()), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get()));
                case 2 -> List.of(new ItemStack(ModItems.MARTIAN_PRESSURE_VALVE.get()), new ItemStack(ModItems.PRESSURE_REGULATOR.get()), new ItemStack(ModItems.MARTIAN_SILICA.get(), 3), new ItemStack(ModBlocks.OXYGEN_PIPE.get(), 2), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get()));
                default -> List.of(new ItemStack(ModItems.MARTIAN_PRESSURE_VALVE.get()), new ItemStack(ModItems.PRESSURE_REGULATOR.get()), new ItemStack(ModItems.MARTIAN_SILICA.get(), 2), new ItemStack(ModBlocks.BROKEN_SOLAR_PANEL.get()), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get()));
            };
            case EUROPA -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.EUROPA_THERMAL_PROBE.get()), new ItemStack(ModItems.EUROPA_PROBE_ARRAY.get()), new ItemStack(ModItems.CRYO_BATTERY.get()), new ItemStack(ModItems.CRYO_CRYSTAL.get()), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
                case 2 -> List.of(new ItemStack(ModItems.EUROPA_THERMAL_PROBE.get()), new ItemStack(ModItems.EUROPA_PROBE_ARRAY.get()), new ItemStack(ModItems.THERMAL_SPACE_LINER.get()), new ItemStack(ModItems.CRYO_CRYSTAL.get()), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
                default -> List.of(new ItemStack(ModItems.EUROPA_THERMAL_PROBE.get()), new ItemStack(ModItems.EUROPA_PROBE_ARRAY.get()), new ItemStack(ModItems.CRYO_CRYSTAL.get(), 2), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
            };
            case SATURN -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.SATURN_RING_FRAGMENT.get()), new ItemStack(ModItems.SATURN_RELAY_LENS.get()), new ItemStack(ModItems.TITAN_TRANSFER_WINDOW.get()), new ItemStack(ModItems.OXYGEN_CANISTER.get(), 2), new ItemStack(ModItems.ORBITAL_ALLOY.get()));
                case 2 -> List.of(new ItemStack(ModItems.SATURN_RING_FRAGMENT.get(), 2), new ItemStack(ModItems.SATURN_RELAY_LENS.get()), new ItemStack(ModItems.VACUUM_CIRCUIT.get(), 2), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2));
                default -> List.of(new ItemStack(ModItems.SATURN_RING_FRAGMENT.get()), new ItemStack(ModItems.SATURN_RELAY_LENS.get()), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2), new ItemStack(ModItems.ORBITAL_ALLOY.get(), 2));
            };
            case TITAN -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.TITAN_METHANE_CELL.get()), new ItemStack(ModItems.TITAN_SURVEY_CORE.get()), new ItemStack(ModItems.NEXUS_DRIVE_CORE.get()), new ItemStack(ModItems.THERMAL_STABILIZER.get()), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
                case 2 -> List.of(new ItemStack(ModItems.TITAN_METHANE_CELL.get(), 2), new ItemStack(ModItems.TITAN_SURVEY_CORE.get()), new ItemStack(ModItems.NEXUS_STABILIZER_SHARD.get(), 2), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2));
                default -> List.of(new ItemStack(ModItems.TITAN_METHANE_CELL.get()), new ItemStack(ModItems.TITAN_SURVEY_CORE.get()), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 3), new ItemStack(ModItems.CRYO_BATTERY.get()));
            };
            case NEXUS -> switch (normalized) {
                case 1 -> List.of(new ItemStack(ModItems.NEXUS_STABILIZER_SHARD.get()), new ItemStack(ModItems.LUNAR_CORE_FRAGMENT.get()), new ItemStack(ModItems.NEXUS_DUST.get(), 3), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
                case 2 -> List.of(new ItemStack(ModItems.NEXUS_STABILIZER_SHARD.get()), new ItemStack(ModItems.NEXUS_DUST.get(), 6), new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2));
                default -> List.of(new ItemStack(ModItems.NEXUS_STABILIZER_SHARD.get()), new ItemStack(ModItems.LUNAR_CORE_FRAGMENT.get()), new ItemStack(ModItems.NEXUS_DUST.get(), 4), new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
            };
        };
    }

    public static int siteVariant(Route route, int chunkX, int chunkZ) {
        if (!hasDeepSite(route, chunkX, chunkZ)) {
            return -1;
        }
        return Math.floorMod(chunkX * 13 + chunkZ * 19 + route.ordinal() * 7, 3);
    }

    public static boolean hasDeepSite(Route route, int chunkX, int chunkZ) {
        int spacing = Math.max(3, SITE_SPACING_BASE - safeFeatureDensity());
        return Math.floorMod(chunkX * 31 + chunkZ * 17 + route.ordinal() * 11, spacing) == 0;
    }

    public static String siteName(Route route, int variant) {
        int normalized = Math.floorMod(variant, 3);
        return switch (route) {
            case ORBIT -> switch (normalized) {
                case 1 -> "docking rib corridor";
                case 2 -> "solar breaker yard";
                default -> "station relay spine";
            };
            case MOON -> switch (normalized) {
                case 1 -> "scar drill cairn";
                case 2 -> "Nexus impact survey pit";
                default -> "helium extractor camp";
            };
            case MARS -> switch (normalized) {
                case 1 -> "pressure pipe yard";
                case 2 -> "dust-shield pylon";
                default -> "buried habitat wing";
            };
            case EUROPA -> switch (normalized) {
                case 1 -> "frozen cable substation";
                case 2 -> "cryo vault vent";
                default -> "thermal array lab";
            };
            case SATURN -> switch (normalized) {
                case 1 -> "ring relay rib";
                case 2 -> "salvager trade spine";
                default -> "Saturn ice breaker yard";
            };
            case TITAN -> switch (normalized) {
                case 1 -> "methane pump field";
                case 2 -> "tholin survey dome";
                default -> "Titan pressure shelf";
            };
            case NEXUS -> switch (normalized) {
                case 1 -> "folded station bridge";
                case 2 -> "Nexus growth cluster";
                default -> "Nexus anchor island";
            };
        };
    }

    public static BlockState hazardBlock(Route route, int variant) {
        int normalized = Math.floorMod(variant, 3);
        return switch (route) {
            case ORBIT -> (normalized == 1 ? ModBlocks.ORBITAL_PLATING : ModBlocks.BROKEN_SOLAR_PANEL).get().defaultBlockState();
            case MOON -> (normalized == 1 ? ModBlocks.LUNAR_TITANIUM_BLOCK : ModBlocks.NEXUS_TOUCHED_STONE).get().defaultBlockState();
            case MARS -> (normalized == 1 ? ModBlocks.OXYGEN_PIPE : ModBlocks.MARTIAN_DUST).get().defaultBlockState();
            case EUROPA -> (normalized == 1 ? ModBlocks.CRYO_CRYSTAL_BLOCK : ModBlocks.PACKED_CRYO_ICE).get().defaultBlockState();
            case SATURN -> (normalized == 1 ? ModBlocks.SATURN_RING_RELAY : ModBlocks.SATURN_ICE_RUBBLE).get().defaultBlockState();
            case TITAN -> (normalized == 1 ? ModBlocks.METHANE_ICE : ModBlocks.TITAN_THOLIN_DUST).get().defaultBlockState();
            case NEXUS -> (normalized == 1 ? ModBlocks.ORBITAL_PLATING : ModBlocks.NEXUS_GROWTH).get().defaultBlockState();
        };
    }

    private static BlockState stateFor(Route route, int x, int y, int z, int top) {
        boolean surface = y == top;
        return switch (route) {
            case ORBIT -> (Math.floorMod(x + z + y, 7) == 0 ? ModBlocks.BROKEN_SOLAR_PANEL : ModBlocks.ORBITAL_PLATING).get().defaultBlockState();
            case MOON -> surface ? ModBlocks.LUNAR_REGOLITH.get().defaultBlockState()
                    : (Math.floorMod(x * 3 + z, 11) == 0 ? ModBlocks.LUNAR_TITANIUM_BLOCK : ModBlocks.LUNAR_ROCK).get().defaultBlockState();
            case MARS -> surface ? ModBlocks.MARTIAN_DUST.get().defaultBlockState()
                    : (Math.floorMod(x - z, 13) == 0 ? ModBlocks.MARTIAN_SILICA_BLOCK : ModBlocks.MARTIAN_BASALT).get().defaultBlockState();
            case EUROPA -> surface ? ModBlocks.CRYO_ICE.get().defaultBlockState()
                    : (Math.floorMod(x * x + z * z, 17) == 0 ? ModBlocks.CRYO_CRYSTAL_BLOCK : ModBlocks.PACKED_CRYO_ICE).get().defaultBlockState();
            case SATURN -> surface ? ModBlocks.SATURN_ICE_RUBBLE.get().defaultBlockState()
                    : (Math.floorMod(x + z * 3, 11) == 0 ? ModBlocks.ORBITAL_PLATING : ModBlocks.STATION_WALL_PANEL).get().defaultBlockState();
            case TITAN -> surface ? ModBlocks.TITAN_THOLIN_DUST.get().defaultBlockState()
                    : (Math.floorMod(x * 2 + z, 13) == 0 ? ModBlocks.METHANE_ICE : ModBlocks.SATURN_ICE_RUBBLE).get().defaultBlockState();
            case NEXUS -> surface ? ModBlocks.NEXUS_GROWTH.get().defaultBlockState()
                    : (Math.floorMod(x + y + z, 9) == 0 ? ModBlocks.NEXUS_DUST_BLOCK : ModBlocks.NEXUS_TOUCHED_STONE).get().defaultBlockState();
        };
    }

    private static void placeRouteFeatures(ChunkAccess chunk, ChunkPos chunkPos, Route route) {
        for (int i = 0; i < 3; i++) {
            int localX = 3 + i * 5;
            int localZ = 3 + Math.floorMod(chunkPos.x() + chunkPos.z() + i * 4, 10);
            int worldX = chunkPos.getBlockX(localX);
            int worldZ = chunkPos.getBlockZ(localZ);
            int top = topHeight(route, worldX, worldZ);
            if (top <= BASE_Y && route != Route.ORBIT && route != Route.NEXUS) {
                continue;
            }
            int y = Math.max(top + 1, route == Route.ORBIT ? 91 : route == Route.NEXUS ? 78 : BASE_Y + 3);
            BlockState state = routeFeatureState(route, worldX, worldZ, i);
            setSafe(chunk, localX, y, localZ, state);
            if (i == 1 && route == Route.ORBIT) {
                setSafe(chunk, localX, y, localZ + 1, ModBlocks.STATION_WALL_PANEL.get().defaultBlockState());
            }
        }
    }

    private static void placeSurfaceDetails(ChunkAccess chunk, ChunkPos chunkPos, Route route) {
        for (int localX = 2; localX < 16; localX += 5) {
            for (int localZ = 2; localZ < 16; localZ += 5) {
                int worldX = chunkPos.getBlockX(localX);
                int worldZ = chunkPos.getBlockZ(localZ);
                int top = topHeight(route, worldX, worldZ);
                if (top <= BASE_Y) {
                    continue;
                }
                if (Math.floorMod(worldX * 5 + worldZ * 3 + route.ordinal(), 11) == 0) {
                    setSafe(chunk, localX, top + 1, localZ, routeFeatureState(route, worldX, worldZ, 0));
                }
            }
        }
    }

    private static void carveRouteVoids(ChunkAccess chunk, ChunkPos chunkPos, Route route) {
        if (route == Route.ORBIT || route == Route.NEXUS) {
            return;
        }
        for (int localX = 1; localX < 15; localX++) {
            for (int localZ = 1; localZ < 15; localZ++) {
                int worldX = chunkPos.getBlockX(localX);
                int worldZ = chunkPos.getBlockZ(localZ);
                if (!carvePocket(route, worldX, worldZ)) {
                    continue;
                }
                int top = topHeight(route, worldX, worldZ);
                for (int y = BASE_Y + 1; y < top - 1; y++) {
                    setSafe(chunk, localX, y, localZ, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void placeDeepSite(ChunkAccess chunk, ChunkPos chunkPos, Route route) {
        int variant = siteVariant(route, chunkPos.x(), chunkPos.z());
        if (variant < 0) {
            return;
        }
        int localX = 8;
        int localZ = 8;
        int worldX = chunkPos.getBlockX(localX);
        int worldZ = chunkPos.getBlockZ(localZ);
        int y = Math.max(70, topHeight(route, worldX, worldZ) + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState anchor = landmarkBlock(route);
        int radius = route == Route.NEXUS || variant == 1 ? 3 : 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (route == Route.ORBIT && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                pos.set(localX + dx, y - 1, localZ + dz);
                chunk.setBlockState(pos, supportState(anchor, variant));
            }
        }
        pos.set(localX, y, localZ);
        chunk.setBlockState(pos, anchor);
        placeSiteDetails(chunk, route, variant, localX, localZ, y);
    }

    private static void placeSiteDetails(ChunkAccess chunk, Route route, int variant, int localX, int localZ, int y) {
        placeTraversalHook(chunk, route, variant, localX, localZ, y);
        switch (route) {
            case ORBIT -> placeOrbitSite(chunk, variant, localX, localZ, y);
            case MOON -> placeMoonSite(chunk, variant, localX, localZ, y);
            case MARS -> placeMarsSite(chunk, variant, localX, localZ, y);
            case EUROPA -> placeEuropaSite(chunk, variant, localX, localZ, y);
            case SATURN -> placeSaturnSite(chunk, variant, localX, localZ, y);
            case TITAN -> placeTitanSite(chunk, variant, localX, localZ, y);
            case NEXUS -> placeNexusSite(chunk, variant, localX, localZ, y);
        }
    }

    private static void placeOrbitSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState plating = ModBlocks.ORBITAL_PLATING.get().defaultBlockState();
        BlockState wall = ModBlocks.STATION_WALL_PANEL.get().defaultBlockState();
        BlockState panel = ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState();
        BlockState beacon = ModBlocks.DOCKING_BEACON.get().defaultBlockState();
        if (variant == 0) {
            lineLocal(chunk, x - 5, y - 1, z, x + 5, y - 1, z, plating);
            ribPairs(chunk, x, z, y, wall, 4);
            pillar(chunk, x - 4, z - 2, y, 3, wall);
            pillar(chunk, x + 4, z + 2, y, 3, wall);
            setSafe(chunk, x + 1, y, z, ModBlocks.STATION_RELAY_NODE.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.ORBIT, variant));
            setSafe(chunk, x + 3, y, z, panel);
            setSafe(chunk, x, y, z - 4, beacon);
            placeCache(chunk, x + 2, y, z + 3, Route.ORBIT, variant);
        } else if (variant == 1) {
            lineLocal(chunk, x - 5, y - 1, z - 2, x + 5, y - 1, z - 2, plating);
            lineLocal(chunk, x - 5, y - 1, z + 2, x + 5, y - 1, z + 2, plating);
            for (int i = -4; i <= 4; i += 2) {
                lineLocal(chunk, x + i, y, z - 2, x + i, y, z + 2, wall);
            }
            setSafe(chunk, x, y, z, ModBlocks.STATION_RELAY_NODE.get().defaultBlockState());
            setSafe(chunk, x - 4, y, z, hazardBlock(Route.ORBIT, variant));
            setSafe(chunk, x + 4, y, z, beacon);
            placeCache(chunk, x + 1, y, z + 4, Route.ORBIT, variant);
        } else {
            rectFrame(chunk, x - 5, z - 4, x + 5, z + 4, y - 1, plating);
            for (int dz = -3; dz <= 3; dz += 3) {
                lineLocal(chunk, x - 5, y, z + dz, x + 5, y, z + dz, panel);
            }
            setSafe(chunk, x, y, z, ModBlocks.STATION_RELAY_NODE.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z - 3, hazardBlock(Route.ORBIT, variant));
            setSafe(chunk, x + 3, y, z + 3, beacon);
            placeCache(chunk, x - 2, y, z + 3, Route.ORBIT, variant);
        }
    }

    private static void placeMoonSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState regolith = ModBlocks.LUNAR_REGOLITH.get().defaultBlockState();
        BlockState rock = ModBlocks.LUNAR_ROCK.get().defaultBlockState();
        BlockState titanium = ModBlocks.LUNAR_TITANIUM_BLOCK.get().defaultBlockState();
        if (variant == 0) {
            rectFrame(chunk, x - 4, z - 4, x + 4, z + 4, y - 1, rock);
            pillar(chunk, x - 3, z - 3, y, 3, titanium);
            pillar(chunk, x + 3, z - 3, y, 2, titanium);
            setSafe(chunk, x - 1, y, z + 1, ModBlocks.HELIUM_EXTRACTOR_NODE.get().defaultBlockState());
            setSafe(chunk, x + 2, y, z + 1, titanium);
            setSafe(chunk, x - 2, y, z - 1, hazardBlock(Route.MOON, variant));
            placeCache(chunk, x + 2, y, z - 3, Route.MOON, variant);
        } else if (variant == 1) {
            lineLocal(chunk, x - 5, y - 1, z - 3, x + 5, y - 1, z + 3, ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState());
            lineLocal(chunk, x - 4, y, z + 2, x + 2, y, z - 4, ModBlocks.OXYGEN_PIPE.get().defaultBlockState());
            pillar(chunk, x, z, y, 4, titanium);
            setSafe(chunk, x - 1, y, z + 1, ModBlocks.HELIUM_EXTRACTOR_NODE.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z - 1, hazardBlock(Route.MOON, variant));
            setSafe(chunk, x + 2, y, z + 2, ModBlocks.SURVEY_MARKER.get().defaultBlockState());
            placeCache(chunk, x + 3, y, z - 2, Route.MOON, variant);
        } else {
            ring(chunk, x, z, y - 1, 5, rock);
            ring(chunk, x, z, y, 3, ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState());
            setSafe(chunk, x, y, z + 2, regolith);
            setSafe(chunk, x - 1, y, z + 1, ModBlocks.HELIUM_EXTRACTOR_NODE.get().defaultBlockState());
            setSafe(chunk, x + 3, y, z - 1, hazardBlock(Route.MOON, variant));
            placeCache(chunk, x - 3, y, z - 2, Route.MOON, variant);
        }
    }

    private static void placeMarsSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState basalt = ModBlocks.MARTIAN_BASALT.get().defaultBlockState();
        BlockState silica = ModBlocks.MARTIAN_SILICA_BLOCK.get().defaultBlockState();
        BlockState pipe = ModBlocks.OXYGEN_PIPE.get().defaultBlockState();
        if (variant == 0) {
            rectFrame(chunk, x - 5, z - 3, x + 5, z + 3, y - 1, basalt);
            rectFill(chunk, x - 3, z - 2, x + 3, z + 2, y, silica);
            lineLocal(chunk, x - 5, y, z + 4, x + 5, y, z + 4, pipe);
            setSafe(chunk, x + 1, y + 1, z - 1, ModBlocks.MARS_PRESSURE_CONSOLE.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.MARS, variant));
            placeCache(chunk, x - 2, y, z - 3, Route.MARS, variant);
        } else if (variant == 1) {
            for (int dz = -4; dz <= 4; dz += 2) {
                lineLocal(chunk, x - 5, y, z + dz, x + 5, y, z + dz, pipe);
            }
            for (int dx = -4; dx <= 4; dx += 4) {
                lineLocal(chunk, x + dx, y, z - 5, x + dx, y, z + 5, pipe);
            }
            setSafe(chunk, x + 1, y + 1, z - 1, ModBlocks.MARS_PRESSURE_CONSOLE.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.MARS, variant));
            setSafe(chunk, x + 3, y, z + 1, basalt);
            placeCache(chunk, x - 2, y, z - 3, Route.MARS, variant);
        } else {
            pillar(chunk, x, z, y, 6, basalt);
            pillar(chunk, x - 3, z + 2, y, 3, silica);
            pillar(chunk, x + 3, z - 2, y, 3, silica);
            ring(chunk, x, z, y - 1, 4, ModBlocks.MARTIAN_DUST.get().defaultBlockState());
            setSafe(chunk, x + 1, y, z - 1, ModBlocks.MARS_PRESSURE_CONSOLE.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.MARS, variant));
            setSafe(chunk, x + 2, y, z + 3, pipe);
            placeCache(chunk, x - 1, y, z - 3, Route.MARS, variant);
        }
    }

    private static void placeEuropaSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState ice = ModBlocks.PACKED_CRYO_ICE.get().defaultBlockState();
        BlockState cable = ModBlocks.FROZEN_CABLE.get().defaultBlockState();
        BlockState crystal = ModBlocks.CRYO_CRYSTAL_BLOCK.get().defaultBlockState();
        if (variant == 0) {
            rectFrame(chunk, x - 4, z - 4, x + 4, z + 4, y - 1, ice);
            lineLocal(chunk, x - 4, y, z, x + 4, y, z, cable);
            lineLocal(chunk, x, y, z - 4, x, y, z + 4, cable);
            setSafe(chunk, x + 1, y, z + 1, ModBlocks.EUROPA_THERMAL_ARRAY.get().defaultBlockState());
            setSafe(chunk, x, y, z + 3, ModBlocks.THERMAL_VENT.get().defaultBlockState());
            setSafe(chunk, x + 3, y, z, hazardBlock(Route.EUROPA, variant));
            placeCache(chunk, x - 2, y, z - 3, Route.EUROPA, variant);
        } else if (variant == 1) {
            for (int dz = -4; dz <= 4; dz += 2) {
                lineLocal(chunk, x - 5, y, z + dz, x + 5, y, z + dz, cable);
            }
            pillar(chunk, x - 3, z - 2, y, 4, crystal);
            pillar(chunk, x + 3, z + 2, y, 3, crystal);
            setSafe(chunk, x + 1, y, z + 1, ModBlocks.EUROPA_THERMAL_ARRAY.get().defaultBlockState());
            setSafe(chunk, x + 2, y, z, hazardBlock(Route.EUROPA, variant));
            placeCache(chunk, x - 2, y, z - 3, Route.EUROPA, variant);
        } else {
            ring(chunk, x, z, y - 1, 5, ice);
            ring(chunk, x, z, y, 3, crystal);
            pillar(chunk, x, z, y, 4, ModBlocks.THERMAL_VENT.get().defaultBlockState());
            setSafe(chunk, x + 1, y, z + 1, ModBlocks.EUROPA_THERMAL_ARRAY.get().defaultBlockState());
            setSafe(chunk, x + 3, y, z, hazardBlock(Route.EUROPA, variant));
            placeCache(chunk, x - 2, y, z - 3, Route.EUROPA, variant);
        }
    }

    private static void placeSaturnSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState rubble = ModBlocks.SATURN_ICE_RUBBLE.get().defaultBlockState();
        BlockState plating = ModBlocks.ORBITAL_PLATING.get().defaultBlockState();
        BlockState wall = ModBlocks.STATION_WALL_PANEL.get().defaultBlockState();
        if (variant == 0) {
            lineLocal(chunk, x - 6, y - 1, z, x + 6, y - 1, z, rubble);
            lineLocal(chunk, x - 6, y - 1, z - 2, x + 6, y - 1, z - 2, plating);
            lineLocal(chunk, x - 6, y - 1, z + 2, x + 6, y - 1, z + 2, plating);
            lineLocal(chunk, x - 4, y, z - 2, x + 4, y, z + 2, wall);
            lineLocal(chunk, x - 4, y, z + 2, x + 4, y, z - 2, wall);
            setSafe(chunk, x, y, z, ModBlocks.SATURN_RING_RELAY.get().defaultBlockState());
            setSafe(chunk, x + 2, y, z + 2, ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.SATURN, variant));
            placeCache(chunk, x + 3, y, z - 3, Route.SATURN, variant);
        } else if (variant == 1) {
            rectFrame(chunk, x - 5, z - 4, x + 5, z + 4, y - 1, plating);
            ribPairs(chunk, x, z, y, wall, 4);
            lineLocal(chunk, x - 5, y, z - 1, x + 5, y, z - 1, rubble);
            lineLocal(chunk, x - 5, y, z + 1, x + 5, y, z + 1, rubble);
            setSafe(chunk, x, y, z, ModBlocks.SATURN_RING_RELAY.get().defaultBlockState());
            setSafe(chunk, x - 2, y, z + 2, ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState());
            setSafe(chunk, x + 3, y, z, hazardBlock(Route.SATURN, variant));
            placeCache(chunk, x - 3, y, z - 3, Route.SATURN, variant);
        } else {
            ring(chunk, x, z, y - 1, 5, rubble);
            ring(chunk, x, z, y, 3, plating);
            pillar(chunk, x, z, y, 4, ModBlocks.SATURN_RING_RELAY.get().defaultBlockState());
            setSafe(chunk, x - 2, y, z + 1, ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState());
            setSafe(chunk, x + 2, y, z - 1, hazardBlock(Route.SATURN, variant));
            placeCache(chunk, x + 3, y, z + 2, Route.SATURN, variant);
        }
    }

    private static void placeTitanSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState dust = ModBlocks.TITAN_THOLIN_DUST.get().defaultBlockState();
        BlockState ice = ModBlocks.METHANE_ICE.get().defaultBlockState();
        BlockState pipe = ModBlocks.OXYGEN_PIPE.get().defaultBlockState();
        if (variant == 0) {
            rectFill(chunk, x - 4, z - 3, x + 4, z + 3, y - 1, dust);
            rectFrame(chunk, x - 5, z - 4, x + 5, z + 4, y, ice);
            lineLocal(chunk, x - 5, y, z, x + 5, y, z, pipe);
            lineLocal(chunk, x, y, z - 4, x, y, z + 4, pipe);
            setSafe(chunk, x, y, z, ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState());
            setSafe(chunk, x + 2, y, z + 2, ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.TITAN, variant));
            placeCache(chunk, x + 3, y, z - 3, Route.TITAN, variant);
        } else if (variant == 1) {
            ring(chunk, x, z, y - 1, 5, ice);
            ring(chunk, x, z, y, 3, dust);
            for (int dz = -4; dz <= 4; dz += 2) {
                lineLocal(chunk, x - 4, y, z + dz, x + 4, y, z + dz, pipe);
            }
            setSafe(chunk, x, y, z, ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState());
            setSafe(chunk, x - 2, y, z + 2, ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState());
            placeCache(chunk, x - 3, y, z - 3, Route.TITAN, variant);
        } else {
            rectFrame(chunk, x - 5, z - 5, x + 5, z + 5, y - 1, ice);
            rectFill(chunk, x - 3, z - 3, x + 3, z + 3, y, dust);
            pillar(chunk, x, z, y, 4, ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState());
            setSafe(chunk, x + 2, y, z, ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.TITAN, variant));
            placeCache(chunk, x + 3, y, z + 2, Route.TITAN, variant);
        }
    }

    private static void placeNexusSite(ChunkAccess chunk, int variant, int x, int z, int y) {
        BlockState touched = ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState();
        BlockState growth = ModBlocks.NEXUS_GROWTH.get().defaultBlockState();
        BlockState dust = ModBlocks.NEXUS_DUST_BLOCK.get().defaultBlockState();
        if (variant == 0) {
            ring(chunk, x, z, y - 1, 5, touched);
            ring(chunk, x, z, y, 3, growth);
            pillar(chunk, x, z, y, 4, ModBlocks.NEXUS_ANCHOR.get().defaultBlockState());
            setSafe(chunk, x - 2, y, z + 2, ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.NEXUS, variant));
            setSafe(chunk, x + 2, y, z, dust);
            placeCache(chunk, x + 3, y, z - 2, Route.NEXUS, variant);
        } else if (variant == 1) {
            lineLocal(chunk, x - 6, y - 1, z, x + 6, y - 1, z, ModBlocks.ORBITAL_PLATING.get().defaultBlockState());
            lineLocal(chunk, x - 3, y, z - 2, x + 3, y, z + 2, ModBlocks.STATION_WALL_PANEL.get().defaultBlockState());
            setSafe(chunk, x, y, z, ModBlocks.NEXUS_ANCHOR.get().defaultBlockState());
            setSafe(chunk, x - 2, y, z + 2, ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.NEXUS, variant));
            setSafe(chunk, x + 2, y, z + 2, dust);
            placeCache(chunk, x + 3, y, z - 2, Route.NEXUS, variant);
        } else {
            for (int dx = -4; dx <= 4; dx += 2) {
                pillar(chunk, x + dx, z + Math.floorMod(dx, 3) - 1, y, 2 + Math.abs(dx) / 2, growth);
            }
            ring(chunk, x, z, y - 1, 4, touched);
            setSafe(chunk, x, y, z, ModBlocks.NEXUS_ANCHOR.get().defaultBlockState());
            setSafe(chunk, x - 2, y, z + 1, ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState());
            setSafe(chunk, x - 3, y, z, hazardBlock(Route.NEXUS, variant));
            setSafe(chunk, x + 2, y, z, dust);
            placeCache(chunk, x + 3, y, z - 2, Route.NEXUS, variant);
        }
    }

    private static void lineLocal(ChunkAccess chunk, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int steps = Math.max(Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)), Math.abs(z2 - z1));
        if (steps == 0) {
            setSafe(chunk, x1, y1, z1, state);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            setSafe(chunk, x, y, z, state);
        }
    }

    private static void rectFill(ChunkAccess chunk, int x1, int z1, int x2, int z2, int y, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                setSafe(chunk, x, y, z, state);
            }
        }
    }

    private static void rectFrame(ChunkAccess chunk, int x1, int z1, int x2, int z2, int y, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            setSafe(chunk, x, y, z1, state);
            setSafe(chunk, x, y, z2, state);
        }
        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            setSafe(chunk, x1, y, z, state);
            setSafe(chunk, x2, y, z, state);
        }
    }

    private static void pillar(ChunkAccess chunk, int x, int z, int y, int height, BlockState state) {
        for (int dy = 0; dy < height; dy++) {
            setSafe(chunk, x, y + dy, z, state);
        }
    }

    private static void ribPairs(ChunkAccess chunk, int x, int z, int y, BlockState state, int span) {
        for (int dx = -span; dx <= span; dx += 2) {
            setSafe(chunk, x + dx, y, z - 2, state);
            setSafe(chunk, x + dx, y + 1, z - 2, state);
            setSafe(chunk, x + dx, y, z + 2, state);
            setSafe(chunk, x + dx, y + 1, z + 2, state);
        }
    }

    private static void ring(ChunkAccess chunk, int x, int z, int y, int radius, BlockState state) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distance = Math.abs(dx) + Math.abs(dz);
                if (distance >= radius - 1 && distance <= radius + 1) {
                    setSafe(chunk, x + dx, y, z + dz, state);
                }
            }
        }
    }

    private static void placeTraversalHook(ChunkAccess chunk, Route route, int variant, int localX, int localZ, int y) {
        BlockState hook = switch (route) {
            case ORBIT, SATURN, NEXUS -> ModBlocks.STATION_WALL_PANEL.get().defaultBlockState();
            case MOON -> ModBlocks.LUNAR_ROCK.get().defaultBlockState();
            case MARS -> ModBlocks.OXYGEN_PIPE.get().defaultBlockState();
            case EUROPA -> ModBlocks.FROZEN_CABLE.get().defaultBlockState();
            case TITAN -> ModBlocks.METHANE_ICE.get().defaultBlockState();
        };
        int length = variant == 1 ? 5 : 3;
        for (int i = 1; i <= length; i++) {
            setSafe(chunk, localX - i, y - 1, localZ + 3, hook);
        }
    }

    private static BlockState routeFeatureState(Route route, int x, int z, int salt) {
        return switch (route) {
            case ORBIT -> Math.floorMod(x + z + salt, 3) == 0 ? ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState() : ModBlocks.SATELLITE_PLATING.get().defaultBlockState();
            case MOON -> scarTrench(x, z) ? ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState() : ModBlocks.LUNAR_TITANIUM_BLOCK.get().defaultBlockState();
            case MARS -> pressureCavern(x, z) ? ModBlocks.MARTIAN_BASALT.get().defaultBlockState() : ModBlocks.MARTIAN_SILICA_BLOCK.get().defaultBlockState();
            case EUROPA -> thermalPocket(x, z) ? ModBlocks.THERMAL_VENT.get().defaultBlockState() : ModBlocks.FROZEN_CABLE.get().defaultBlockState();
            case SATURN -> ringSpoke(x, z) ? ModBlocks.SATURN_RING_RELAY.get().defaultBlockState() : ModBlocks.SATURN_ICE_RUBBLE.get().defaultBlockState();
            case TITAN -> methaneSink(x, z) ? ModBlocks.METHANE_ICE.get().defaultBlockState() : ModBlocks.TITAN_THOLIN_DUST.get().defaultBlockState();
            case NEXUS -> nexusBridge(x, z) ? ModBlocks.ORBITAL_PLATING.get().defaultBlockState() : ModBlocks.NEXUS_DUST_BLOCK.get().defaultBlockState();
        };
    }

    private static void setSafe(ChunkAccess chunk, int x, int y, int z, BlockState state) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y >= 384) {
            return;
        }
        chunk.setBlockState(new BlockPos(x, y, z), state);
    }

    private static void placeCache(ChunkAccess chunk, int x, int y, int z, Route route, int variant) {
        if (!Config.DEEP_SITE_CACHES_ENABLED.get()) {
            return;
        }
        BlockState marker = cacheMarkerState(route);
        setSafe(chunk, x, y, z - 1, marker);
        setSafe(chunk, x, y + 1, z - 1, marker);
        setSafe(chunk, x + 1, y, z - 1, marker);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = Blocks.CHEST.defaultBlockState();
        chunk.setBlockState(pos, state);
        ChestBlockEntity chest = new ChestBlockEntity(pos, state);
        List<ItemStack> items = cacheItems(route, variant);
        for (int slot = 0; slot < items.size(); slot++) {
            chest.setItem(slot, boostedSupport(items.get(slot)));
        }
        chunk.setBlockEntity(chest);
    }

    private static BlockState cacheMarkerState(Route route) {
        return switch (route) {
            case ORBIT -> ModBlocks.DOCKING_BEACON.get().defaultBlockState();
            case MOON -> ModBlocks.LUNAR_TITANIUM_BLOCK.get().defaultBlockState();
            case MARS -> ModBlocks.OXYGEN_PIPE.get().defaultBlockState();
            case EUROPA -> ModBlocks.FROZEN_CABLE.get().defaultBlockState();
            case SATURN -> ModBlocks.SATURN_RING_RELAY.get().defaultBlockState();
            case TITAN -> ModBlocks.METHANE_ICE.get().defaultBlockState();
            case NEXUS -> ModBlocks.NEXUS_DUST_BLOCK.get().defaultBlockState();
        };
    }

    private static ItemStack boostedSupport(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (copy.is(ModItems.EMERGENCY_OXYGEN_CELL.get())
                || copy.is(ModItems.SUIT_SEALANT_PATCH.get())
                || copy.is(ModItems.OXYGEN_CANISTER.get())) {
            int multiplier = Config.ARRIVAL_CACHE_SUPPORT_MULTIPLIER.get();
            copy.setCount(Math.min(copy.getMaxStackSize(), Math.max(1, copy.getCount() * multiplier)));
        }
        return copy;
    }

    private static BlockState supportState(BlockState anchor, int variant) {
        if (variant == 1 && anchor.is(ModBlocks.SIGNAL_RELAY.get())) {
            return ModBlocks.ORBITAL_PLATING.get().defaultBlockState();
        }
        if (anchor.is(ModBlocks.THERMAL_VENT.get())) {
            return ModBlocks.PACKED_CRYO_ICE.get().defaultBlockState();
        }
        if (anchor.is(ModBlocks.NEXUS_ANCHOR.get())) {
            return ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState();
        }
        if (anchor.is(ModBlocks.SATURN_RING_RELAY.get())) {
            return ModBlocks.SATURN_ICE_RUBBLE.get().defaultBlockState();
        }
        if (anchor.is(ModBlocks.TITAN_METHANE_PUMP.get())) {
            return ModBlocks.METHANE_ICE.get().defaultBlockState();
        }
        if (anchor.is(ModBlocks.SURVEY_MARKER.get())) {
            return ModBlocks.LUNAR_ROCK.get().defaultBlockState();
        }
        return ModBlocks.STATION_WALL_PANEL.get().defaultBlockState();
    }

    private static int safeFeatureDensity() {
        try {
            return Config.ROUTE_FEATURE_DENSITY.get();
        } catch (IllegalStateException ignored) {
            return 3;
        }
    }

    private static boolean debrisBand(int x, int z) {
        return Math.floorMod(x / 16 + z / 16, 3) == 0 || Math.abs(Math.floorMod(x, 37) - 18) < 3;
    }

    private static boolean scarTrench(int x, int z) {
        return Math.abs(Math.floorMod(x + z * 2, 47) - 23) < 3;
    }

    private static boolean basaltRidge(int x, int z) {
        return Math.abs(Math.floorMod(x * 2 - z, 53) - 26) < 4;
    }

    private static boolean pressureCavern(int x, int z) {
        return Math.floorMod(x * x + z * 7, 67) < 7;
    }

    private static boolean iceFracture(int x, int z) {
        return Math.abs(Math.floorMod(x - z * 3, 43) - 21) < 3;
    }

    private static boolean thermalPocket(int x, int z) {
        return Math.floorMod(x * 5 + z * z, 71) < 6;
    }

    private static boolean nexusIsland(int x, int z) {
        int cx = Math.floorMod(x, 47) - 23;
        int cz = Math.floorMod(z, 47) - 23;
        return cx * cx + cz * cz < 230;
    }

    private static boolean nexusBridge(int x, int z) {
        return Math.abs(Math.floorMod(x + z, 47) - 23) < 2 || Math.abs(Math.floorMod(x - z, 47) - 23) < 2;
    }

    private static boolean saturnRingPlate(int x, int z) {
        return Math.abs(Math.floorMod(z, 41) - 20) < 8 || Math.abs(Math.floorMod(x + z, 67) - 33) < 3;
    }

    private static boolean ringSpoke(int x, int z) {
        return Math.abs(Math.floorMod(x * 2 - z, 59) - 29) < 3;
    }

    private static boolean methaneRidge(int x, int z) {
        return Math.abs(Math.floorMod(x + z * 2, 61) - 30) < 4;
    }

    private static boolean methaneSink(int x, int z) {
        return Math.floorMod(x * x + z * 11, 73) < 8;
    }

    private static boolean carvePocket(Route route, int x, int z) {
        return switch (route) {
            case MOON -> scarTrench(x, z) && Math.floorMod(x + z, 4) == 0;
            case MARS -> pressureCavern(x, z);
            case EUROPA -> iceFracture(x, z) || thermalPocket(x, z);
            case TITAN -> methaneSink(x, z);
            default -> false;
        };
    }

    private static int wave(int x, int z) {
        return Math.floorMod(x * 734287 + z * 912271 + x * z * 13, 13) - 6;
    }

    public enum Route implements StringRepresentable {
        ORBIT("orbit"),
        MOON("moon"),
        MARS("mars"),
        EUROPA("europa"),
        SATURN("saturn"),
        TITAN("titan"),
        NEXUS("nexus");

        public static final Codec<Route> CODEC = StringRepresentable.fromEnum(Route::values);
        private final String name;

        Route(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
