package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class LunarScarZone {
    private LunarScarZone() {
    }

    public static void seedLandingSite(ServerLevel level, BlockPos origin) {
        BlockPos floor = origin.below(2);
        placeMoonShelf(level, floor);
        placeMiningOutpost(level, floor.offset(8, 1, 0));
        placeNexusCrater(level, floor.offset(-10, 0, -7));
        placeHeliumExtractor(level, floor.offset(-4, 1, 9));
        level.setBlock(floor.offset(-1, 1, 7), ModBlocks.SURVEY_MARKER.get().defaultBlockState(), 3);
        RouteCache.place(level, floor.offset(-3, 1, 7),
                new ItemStack(ModItems.LUNAR_CORE_SAMPLE.get()),
                new ItemStack(ModItems.HELIUM_EXTRACTOR_CORE.get()),
                new ItemStack(ModItems.HELIUM_3_CELL.get()),
                new ItemStack(ModItems.LUNAR_TITANIUM.get(), 3),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeMoonShelf(ServerLevel level, BlockPos origin) {
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                int distance = Math.abs(x) + Math.abs(z);
                if (distance <= 11) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.MOON_DUST.get().defaultBlockState(), 3);
                    if (distance % 5 == 0) {
                        level.setBlock(origin.offset(x, -1, z), ModBlocks.LUNAR_ROCK.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeMiningOutpost(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, 0, z), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
            }
        }
        for (int y = 1; y <= 3; y++) {
            level.setBlock(origin.offset(-2, y, -2), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(2, y, -2), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(-2, y, 2), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(2, y, 2), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.VACUUM_SMELTER.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 1, 0), ModBlocks.ORBITAL_FABRICATOR.get().defaultBlockState(), 3);
    }

    private static void placeNexusCrater(ServerLevel level, BlockPos origin) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x * x + z * z <= 18) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(origin.above(), ModBlocks.NEXUS_DUST_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(origin.above(2), ModBlocks.MOON_GLASS.get().defaultBlockState(), 3);
    }

    private static void placeHeliumExtractor(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 4; y++) {
            level.setBlock(origin.offset(0, y, 0), ModBlocks.OXYGEN_PIPE.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(1, 0, 0), ModBlocks.LUNAR_TITANIUM_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(-1, 0, 0), ModBlocks.MOON_GLASS.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 4, 0), ModBlocks.HELIUM_EXTRACTOR_NODE.get().defaultBlockState(), 3);
    }
}
