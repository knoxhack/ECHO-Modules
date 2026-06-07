package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class RouteCache {
    private RouteCache() {
    }

    public static void place(ServerLevel level, BlockPos pos, ItemStack... items) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof Container container) {
            for (int i = 0; i < Math.min(items.length, container.getContainerSize()); i++) {
                container.setItem(i, boostedSupport(items[i]));
            }
            container.setChanged();
        }
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
}
