package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class EuropaCryoOcean {
    private EuropaCryoOcean() {
    }

    public static void seedLandingSite(ServerLevel level, BlockPos origin) {
        BlockPos floor = origin.below(2);
        placeIceShelf(level, floor);
        placeSubIceLab(level, floor.offset(7, 1, 0));
        placeDrillStation(level, floor.offset(-8, 1, -3));
        placeDeepSignalBuoy(level, floor.offset(0, 1, 9));
        level.setBlock(floor.offset(3, 1, 1), ModBlocks.THERMAL_VENT.get().defaultBlockState(), 3);
        RouteCache.place(level, floor.offset(5, 1, 0),
                new ItemStack(ModItems.CRYO_CRYSTAL.get(), 2),
                new ItemStack(ModItems.EUROPA_THERMAL_PROBE.get()),
                new ItemStack(ModItems.EUROPA_PROBE_ARRAY.get()),
                new ItemStack(ModItems.CRYO_BATTERY.get()),
                new ItemStack(ModItems.THERMAL_SPACE_LINER.get()),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeIceShelf(ServerLevel level, BlockPos origin) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                if (Math.abs(x) + Math.abs(z) <= 13) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.CRYO_ICE.get().defaultBlockState(), 3);
                    if ((x * x + z * z) % 7 == 0) {
                        level.setBlock(origin.offset(x, -1, z), ModBlocks.CRYO_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeSubIceLab(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, 0, z), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    level.setBlock(origin.offset(x, 1, z), ModBlocks.FROZEN_CABLE.get().defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.VACUUM_SMELTER.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(-1, 1, 0), ModBlocks.EUROPA_THERMAL_ARRAY.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 1, 0), ModBlocks.STATION_LIFE_SUPPORT_CORE.get().defaultBlockState(), 3);
    }

    private static void placeDrillStation(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 6; y++) {
            level.setBlock(origin.offset(0, y, 0), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            if (y % 2 == 0) {
                level.setBlock(origin.offset(1, y, 0), ModBlocks.OXYGEN_PIPE.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(0, -1, 0), ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState(), 3);
    }

    private static void placeDeepSignalBuoy(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 5; y++) {
            level.setBlock(origin.offset(0, y, 0), ModBlocks.CRYO_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.above(5), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
    }
}
