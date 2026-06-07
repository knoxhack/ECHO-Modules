package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class TitanMethaneShelf {
    private TitanMethaneShelf() {
    }

    public static void seedLandingSite(ServerLevel level, BlockPos origin) {
        BlockPos floor = origin.below(2);
        placeMethaneShelf(level, floor);
        placePumpStation(level, floor.offset(-6, 1, -4));
        placeSurveyDome(level, floor.offset(7, 1, 5));
        level.setBlock(floor.offset(2, 1, 0), ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState(), 3);
        RouteCache.place(level, floor.offset(-3, 1, 4),
                new ItemStack(ModItems.TITAN_METHANE_CELL.get(), 2),
                new ItemStack(ModItems.TITAN_SURVEY_CORE.get()),
                new ItemStack(ModItems.NEXUS_DRIVE_CORE.get()),
                new ItemStack(ModItems.THERMAL_STABILIZER.get()),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 3),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 3));
    }

    private static void placeMethaneShelf(ServerLevel level, BlockPos origin) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                if (x * x + z * z <= 92) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.TITAN_THOLIN_DUST.get().defaultBlockState(), 3);
                    if ((x - z) % 5 == 0) {
                        level.setBlock(origin.offset(x, -1, z), ModBlocks.METHANE_ICE.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placePumpStation(ServerLevel level, BlockPos origin) {
        for (int z = -4; z <= 4; z++) {
            level.setBlock(origin.offset(0, 0, z), ModBlocks.OXYGEN_PIPE.get().defaultBlockState(), 3);
            if (z % 2 == 0) {
                level.setBlock(origin.offset(1, 0, z), ModBlocks.TITAN_METHANE_PUMP.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(-1, 1, 0), ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState(), 3);
    }

    private static void placeSurveyDome(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, 0, z), ModBlocks.METHANE_ICE.get().defaultBlockState(), 3);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    level.setBlock(origin.offset(x, 1, z), ModBlocks.FROZEN_CABLE.get().defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 1, 0), ModBlocks.NAVIGATION_CONSOLE.get().defaultBlockState(), 3);
    }
}
