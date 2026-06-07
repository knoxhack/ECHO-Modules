package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class GroundRecoverySites {
    private GroundRecoverySites() {
    }

    public static List<GroundRecoverySite> seedStarterSites(ServerLevel level, BlockPos origin) {
        List<GroundRecoverySite> sites = new ArrayList<>();
        sites.add(placeCritical(level, origin.offset(18, 0, 0), GroundRecoverySiteType.ABANDONED_LAUNCH_PAD, GroundRecoverySites::placeLaunchPad));
        sites.add(placeCritical(level, origin.offset(-18, 0, 12), GroundRecoverySiteType.CRASHED_SATELLITE_FIELD, GroundRecoverySites::placeCrashedSatellite));
        sites.add(placeCritical(level, origin.offset(6, 0, -20), GroundRecoverySiteType.ORBITAL_COMMS_ARRAY, GroundRecoverySites::placeCommsArray));
        sites.add(placeCritical(level, origin.offset(-18, 0, -18), GroundRecoverySiteType.CRYO_CREW_BUNKER, GroundRecoverySites::placeCryoCrewBunker));
        sites.add(placeCritical(level, origin.offset(22, 0, 18), GroundRecoverySiteType.FALLEN_ESCAPE_POD, GroundRecoverySites::placeFallenEscapePod));
        placeAmbientRuins(level, origin);
        sites.forEach(site -> recordWorldMarker(level, site));
        return List.copyOf(sites);
    }

    private static GroundRecoverySite placeCritical(ServerLevel level, BlockPos approximate, GroundRecoverySiteType type,
                                                    BiConsumer<ServerLevel, BlockPos> placer) {
        BlockPos surface = surface(level, approximate);
        placer.accept(level, surface);
        return new GroundRecoverySite(type, surface, false);
    }

    private static void placeLaunchPad(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, -1, z), ModBlocks.LAUNCH_PLATFORM.get().defaultBlockState(), 3);
            }
        }
        for (int y = 0; y < 4; y++) {
            level.setBlock(origin.offset(-2, y, -2), ModBlocks.ROCKET_ASSEMBLY_FRAME.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(2, y, -2), ModBlocks.ROCKET_ASSEMBLY_FRAME.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 0, 2), ModBlocks.NAVIGATION_CONSOLE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 2), ModBlocks.FUEL_REFINERY.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(-1, 0, 2), ModBlocks.OXYGEN_COMPRESSOR.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(0, 0, -3),
                new ItemStack(ModItems.ROCKET_NOSE_CONE.get()),
                new ItemStack(ModItems.LANDING_GEAR.get()),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeCrashedSatellite(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 8; i++) {
            level.setBlock(origin.offset(i - 4, i % 2, 0), ModBlocks.SATELLITE_PLATING.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(i - 4, i % 2, 1), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 0, -1), ModBlocks.VACUUM_CIRCUIT_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, -1), ModBlocks.FROZEN_CABLE.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(-2, 0, -2),
                new ItemStack(ModItems.VACUUM_CIRCUIT.get(), 2),
                new ItemStack(ModItems.HEAT_SHIELD_PLATE.get()),
                new ItemStack(ModItems.ORBITAL_TRANSPONDER.get()),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeCommsArray(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 6; y++) {
            level.setBlock(origin.offset(0, y, 0), Blocks.IRON_BARS.defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 6, 0), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 5, 0), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(-1, 5, 0), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(2, 0, 0),
                new ItemStack(ModItems.NAVIGATION_CHIP.get()),
                new ItemStack(ModItems.OXYGEN_CANISTER.get()),
                new ItemStack(ModItems.FUEL_TANK.get()),
                new ItemStack(ModItems.ORBITAL_TRANSPONDER.get()));
    }

    private static void placeCryoCrewBunker(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, -1, z), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
                    level.setBlock(origin.offset(x, 1, z), ModBlocks.FROZEN_CABLE.get().defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(origin.offset(0, 0, 0), ModBlocks.OXYGEN_PIPE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 0), ModBlocks.CRYO_ICE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(-1, 0, 0), ModBlocks.CRYO_ICE.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(0, 0, 3),
                new ItemStack(ModItems.SEALED_SUIT_FRAGMENT.get(), 8),
                new ItemStack(ModItems.OXYGEN_TANK.get()),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 3),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeFallenEscapePod(ServerLevel level, BlockPos origin) {
        for (int i = -2; i <= 2; i++) {
            level.setBlock(origin.offset(i, 0, 0), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(i, 1, 0), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            if (i % 2 == 0) {
                level.setBlock(origin.offset(i, 0, 1), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(0, 0, -1), ModBlocks.FROZEN_CABLE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 1, -1), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(2, 0, -2),
                new ItemStack(ModItems.SALVAGED_ENGINE.get()),
                new ItemStack(ModItems.ECHO_FLIGHT_CORE.get()),
                new ItemStack(ModItems.SEALED_SUIT_FRAGMENT.get(), 4),
                new ItemStack(ModItems.ORBITAL_TRANSPONDER.get()));
    }

    private static void placeAmbientRuins(ServerLevel level, BlockPos origin) {
        placeAmbientSatelliteShard(level, surface(level, origin.offset(-30, 0, 3)));
        placeAmbientSolarSkid(level, surface(level, origin.offset(28, 0, -16)));
        placeAmbientFuelCrate(level, surface(level, origin.offset(0, 0, 30)));
    }

    private static void placeAmbientSatelliteShard(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 4; i++) {
            level.setBlock(origin.offset(i - 2, 0, i % 2), ModBlocks.SATELLITE_PLATING.get().defaultBlockState(), 3);
        }
        RouteCache.place(level, origin.offset(0, 0, -1),
                new ItemStack(ModItems.FROZEN_WIRING.get(), 2),
                new ItemStack(ModItems.VACUUM_CIRCUIT.get()));
    }

    private static void placeAmbientSolarSkid(ServerLevel level, BlockPos origin) {
        level.setBlock(origin, ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 0), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 0, 1), ModBlocks.SOLAR_GLASS.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(-1, 0, 0),
                new ItemStack(ModItems.NAVIGATION_CHIP.get()),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get()));
    }

    private static void placeAmbientFuelCrate(ServerLevel level, BlockPos origin) {
        level.setBlock(origin, ModBlocks.FUEL_REFINERY.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 0), ModBlocks.OXYGEN_PIPE.get().defaultBlockState(), 3);
        RouteCache.place(level, origin.offset(0, 0, 1),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2),
                new ItemStack(ModItems.OXYGEN_CANISTER.get()));
    }

    private static void recordWorldMarker(ServerLevel level, GroundRecoverySite site) {
        if (level == null || site == null) {
            return;
        }
        BlockPos pos = site.pos();
        Identifier markerId = Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID,
                "world_marker/ground_recovery/" + site.type().id() + "/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        WorldMarker marker = new WorldMarker(
                markerId,
                Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_debris_field"),
                WorldMarkerType.ORBITAL_DEBRIS,
                site.type().displayName(),
                "Earth recovery landmark: " + site.type().rewardRole(),
                level.dimension(),
                pos,
                64,
                true,
                level.getGameTime());
        EchoCoreServices.worldMarkerService().revealMarker(level, marker);
    }

    private static BlockPos surface(ServerLevel level, BlockPos approximate) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, approximate);
    }
}
