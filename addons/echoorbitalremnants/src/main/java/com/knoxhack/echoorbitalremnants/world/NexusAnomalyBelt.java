package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class NexusAnomalyBelt {
    private NexusAnomalyBelt() {
    }

    public static void seedEntrySite(ServerLevel level, BlockPos origin) {
        BlockPos anchor = origin.below(2);
        placeFoldedStation(level, anchor);
        placeFloatingRocks(level, anchor);
        placeCoreSpire(level, anchor.offset(0, 2, -10));
        level.setBlock(anchor.offset(0, 1, -8), ModBlocks.NEXUS_ANCHOR.get().defaultBlockState(), 3);
        level.setBlock(anchor.offset(-2, 1, -6), ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState(), 3);
        level.setBlock(anchor.offset(2, 1, -6), ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState(), 3);
        RouteCache.place(level, anchor.offset(2, 1, -8),
                new ItemStack(ModItems.NEXUS_DUST.get(), 6),
                new ItemStack(ModItems.NEXUS_STABILIZER_SHARD.get()),
                new ItemStack(ModItems.LUNAR_CORE_FRAGMENT.get()),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 4));
    }

    private static void placeFoldedStation(ServerLevel level, BlockPos origin) {
        for (int i = -5; i <= 5; i++) {
            level.setBlock(origin.offset(i, 0, 0), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(0, Math.abs(i % 4), i), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(2, 1, 0), ModBlocks.NEXUS_DUST_BLOCK.get().defaultBlockState(), 3);
    }

    private static void placeFloatingRocks(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 9; i++) {
            int x = (i * 7) % 17 - 8;
            int y = 2 + (i % 5);
            int z = (i * 5) % 19 - 9;
            BlockPos pos = origin.offset(x, y, z);
            level.setBlock(pos, ModBlocks.NEXUS_TOUCHED_STONE.get().defaultBlockState(), 3);
            if (i % 3 == 0) {
                level.setBlock(pos.above(), ModBlocks.CRYO_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    private static void placeCoreSpire(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 8; y++) {
            level.setBlock(origin.offset(0, y, 0), ModBlocks.NEXUS_DUST_BLOCK.get().defaultBlockState(), 3);
            if (y % 2 == 0) {
                level.setBlock(origin.offset(1, y, 0), ModBlocks.MOON_GLASS.get().defaultBlockState(), 3);
                level.setBlock(origin.offset(-1, y, 0), ModBlocks.MOON_GLASS.get().defaultBlockState(), 3);
            }
        }
    }
}
