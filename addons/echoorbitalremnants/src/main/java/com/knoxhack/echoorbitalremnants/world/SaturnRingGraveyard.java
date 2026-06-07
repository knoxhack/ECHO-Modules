package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class SaturnRingGraveyard {
    private SaturnRingGraveyard() {
    }

    public static void seedLandingSite(ServerLevel level, BlockPos origin) {
        BlockPos floor = origin.below(2);
        placeRingShelf(level, floor);
        placeRelaySpine(level, floor.offset(-8, 1, 2));
        placeSalvageHub(level, floor.offset(8, 1, -3));
        level.setBlock(floor.offset(0, 1, 6), ModBlocks.SATURN_RING_RELAY.get().defaultBlockState(), 3);
        RouteCache.place(level, floor.offset(4, 1, 4),
                new ItemStack(ModItems.SATURN_RING_FRAGMENT.get(), 2),
                new ItemStack(ModItems.SATURN_RELAY_LENS.get()),
                new ItemStack(ModItems.TITAN_TRANSFER_WINDOW.get()),
                new ItemStack(ModItems.ORBITAL_ALLOY.get(), 2),
                new ItemStack(ModItems.OXYGEN_CANISTER.get(), 2),
                new ItemStack(ModItems.SUIT_SEALANT_PATCH.get(), 2),
                new ItemStack(ModItems.EMERGENCY_OXYGEN_CELL.get(), 2));
    }

    private static void placeRingShelf(ServerLevel level, BlockPos origin) {
        for (int x = -9; x <= 9; x++) {
            for (int z = -6; z <= 6; z++) {
                if (Math.abs(x) + Math.abs(z) <= 13) {
                    level.setBlock(origin.offset(x, 0, z), ModBlocks.SATURN_ICE_RUBBLE.get().defaultBlockState(), 3);
                    if ((x * 5 + z * 7) % 9 == 0) {
                        level.setBlock(origin.offset(x, -1, z), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeRelaySpine(ServerLevel level, BlockPos origin) {
        for (int x = -5; x <= 5; x++) {
            level.setBlock(origin.offset(x, 0, 0), ModBlocks.STATION_WALL_PANEL.get().defaultBlockState(), 3);
            if (x % 2 == 0) {
                level.setBlock(origin.offset(x, 1, 0), ModBlocks.SATURN_RING_RELAY.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(0, 1, 2), ModBlocks.FACTION_RELAY_HUB.get().defaultBlockState(), 3);
    }

    private static void placeSalvageHub(ServerLevel level, BlockPos origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, 0, z), ModBlocks.ORBITAL_PLATING.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(origin.offset(0, 1, 0), ModBlocks.FACTION_VENDOR_KIOSK.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 1, 0), ModBlocks.DOCKING_BEACON.get().defaultBlockState(), 3);
    }
}
