package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class OrbitalDebrisField {
    private OrbitalDebrisField() {
    }

    public static void seedArrivalField(ServerLevel level, BlockPos origin) {
        BlockPos deck = origin.below(2);
        placeDockingDeck(level, deck);
        placeSolarWing(level, deck.offset(-11, 1, 0), -1);
        placeSolarWing(level, deck.offset(11, 1, 0), 1);
        placeCargoPod(level, deck.offset(0, 2, 10));
        placeCargoPod(level, deck.offset(8, 4, -9));
        placeBrokenSatellite(level, deck.offset(-9, 5, -8));
        placeMemoryBeacon(level, deck.offset(0, 3, -12));
        level.setBlock(deck.offset(-3, 1, -3), ModBlocks.SIGNAL_RELAY.get().defaultBlockState(), 3);
        level.setBlock(deck.offset(-1, 1, -3), ModBlocks.STATION_RELAY_NODE.get().defaultBlockState(), 3);
        RouteCache.place(level, deck.offset(3, 1, -3),
                new ItemStack(ModItems.ORBIT_SURVEY_DATA.get()),
                new ItemStack(ModItems.STATION_RELAY_FUSE.get()),
                new ItemStack(ModItems.OXYGEN_CANISTER.get(), 2),
                new ItemStack(ModItems.VACUUM_CIRCUIT.get()),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 3));
        recordWorldMarker(level, origin, "Station ECHO Debris Field", "Arrival field seeded with docking deck, relay, and salvage cache.");
    }

    public static void seedAmbientDebris(ServerLevel level, BlockPos origin) {
        int seed = Math.abs(origin.getX() * 31 + origin.getZ() * 17 + origin.getY());
        BlockPos center = origin.offset((seed % 17) - 8, (seed % 5) - 2, ((seed / 3) % 17) - 8);
        for (int i = 0; i < 5; i++) {
            BlockPos pos = center.offset(i - 2, i % 3, (i * 2) % 5 - 2);
            level.setBlock(pos, ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            if (i % 2 == 0) {
                level.setBlock(pos.above(), ModBlocks.FROZEN_CABLE.get().defaultBlockState(), 3);
            }
        }
        recordWorldMarker(level, center, "Ambient Orbital Debris", "Ambient orbital wreckage seeded near the route.");
    }

    private static void placeDockingDeck(ServerLevel level, BlockPos origin) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean rim = Math.abs(x) == 4 || Math.abs(z) == 4;
                level.setBlock(origin.offset(x, 0, z), (rim ? ModBlocks.SATELLITE_PLATING : ModBlocks.STATION_WALL_PANEL).get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(-2, 1, 2), ModBlocks.STATION_LIFE_SUPPORT_CORE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 1, 2), ModBlocks.NAVIGATION_CONSOLE.get().defaultBlockState(), 3);
    }

    private static void placeSolarWing(ServerLevel level, BlockPos origin, int direction) {
        for (int x = 0; x < 8; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(origin.offset(x * direction, 0, z), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
            }
        }
        for (int x = 0; x < 8; x += 2) {
            level.setBlock(origin.offset(x * direction, -1, 0), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
        }
    }

    private static void placeCargoPod(ServerLevel level, BlockPos origin) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean shell = Math.abs(x) + Math.abs(y) + Math.abs(z) >= 2;
                    if (shell) {
                        level.setBlock(origin.offset(x, y, z), ModBlocks.SATELLITE_PLATING.get().defaultBlockState(), 3);
                    }
                }
            }
        }
        level.setBlock(origin, ModBlocks.CRYO_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
    }

    private static void placeBrokenSatellite(ServerLevel level, BlockPos origin) {
        for (int i = -3; i <= 3; i++) {
            level.setBlock(origin.offset(i, 0, 0), ModBlocks.SATELLITE_PLATING.get().defaultBlockState(), 3);
            if (Math.abs(i) > 1) {
                level.setBlock(origin.offset(i, 0, 1), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
                level.setBlock(origin.offset(i, 0, -1), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.above(), ModBlocks.VACUUM_CIRCUIT_BLOCK.get().defaultBlockState(), 3);
    }

    private static void placeMemoryBeacon(ServerLevel level, BlockPos origin) {
        level.setBlock(origin, ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
        level.setBlock(origin.below(), ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState(), 3);
        level.setBlock(origin.below(2), ModBlocks.ORBITAL_ALLOY_BLOCK.get().defaultBlockState(), 3);
    }

    private static void recordWorldMarker(ServerLevel level, BlockPos pos, String title, String summary) {
        if (level == null || pos == null) {
            return;
        }
        Identifier markerId = Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID,
                "world_marker/orbital_debris/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        WorldMarker marker = new WorldMarker(
                markerId,
                Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_debris_field"),
                WorldMarkerType.ORBITAL_DEBRIS,
                title,
                summary,
                level.dimension(),
                pos,
                96,
                true,
                level.getGameTime());
        EchoCoreServices.worldMarkerService().revealMarker(level, marker);
    }
}
