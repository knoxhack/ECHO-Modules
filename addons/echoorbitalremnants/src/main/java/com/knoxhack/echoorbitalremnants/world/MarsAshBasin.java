package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class MarsAshBasin {
    private MarsAshBasin() {
    }

    public static void seedLandingSite(ServerLevel level, BlockPos origin) {
        BlockPos floor = origin.below(2);
        placeAshShelf(level, floor);
        placeBuriedHabitat(level, floor.offset(-8, 1, 4));
        placeTerraformingTower(level, floor.offset(9, 1, -5));
        placeRustedLaunchSilo(level, floor.offset(3, 1, 10));
        level.setBlock(floor.offset(-4, 1, 4), ModBlocks.SIGNAL_RELAY.get().defaultBlockState(), 3);
        RouteCache.place(level, floor.offset(-6, 1, 4),
                new ItemStack(ModItems.MARTIAN_SILICA.get(), 2),
                new ItemStack(ModItems.MARTIAN_PRESSURE_VALVE.get()),
                new ItemStack(ModItems.PRESSURE_REGULATOR.get()),
                new ItemStack(ModBlocks.BROKEN_SOLAR_PANEL.get().asItem(), 2),
                new ItemStack(ModItems.OXYGEN_BOOSTER.get()),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeAshShelf(ServerLevel level, BlockPos origin) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                if (x * x + z * z <= 82) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.MARTIAN_DUST.get().defaultBlockState(), 3);
                    if ((x + z) % 6 == 0) {
                        level.setBlock(origin.offset(x, -1, z), ModBlocks.MARTIAN_SILICA_BLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeBuriedHabitat(ServerLevel level, BlockPos origin) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, 0, z), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
                if (Math.abs(x) == 3 || Math.abs(z) == 2) {
                    level.setBlock(origin.offset(x, 1, z), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.NAVIGATION_CONSOLE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 1, 0), ModBlocks.MARS_PRESSURE_CONSOLE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 1, 0), ModBlocks.SOLAR_RECLAIMER.get().defaultBlockState(), 3);
    }

    private static void placeTerraformingTower(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 8; y++) {
            level.setBlock(origin.offset(0, y, 0), ModBlocks.OXYGEN_PIPE.get().defaultBlockState(), 3);
            if (y % 3 == 0) {
                level.setBlock(origin.offset(1, y, 0), ModBlocks.MARTIAN_SILICA_BLOCK.get().defaultBlockState(), 3);
                level.setBlock(origin.offset(-1, y, 0), ModBlocks.BROKEN_SOLAR_PANEL.get().defaultBlockState(), 3);
            }
        }
    }

    private static void placeRustedLaunchSilo(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 5; y++) {
            level.setBlock(origin.offset(0, y, 0), ModBlocks.LAUNCH_PLATFORM.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(1, y, 0), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(-1, y, 0), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 5, 0), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
    }
}
