package com.knoxhack.echo.npcore.trade;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EchoNpcTradeRuntime {
    private EchoNpcTradeRuntime() {
    }

    public static boolean hasCosts(ServerPlayer player, List<EchoNpcTradeCost> costs) {
        for (EchoNpcTradeCost cost : costs) {
            if (countItem(player, cost.item()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean consumeCosts(ServerPlayer player, List<EchoNpcTradeCost> costs) {
        if (!hasCosts(player, costs)) {
            return false;
        }
        for (EchoNpcTradeCost cost : costs) {
            removeItem(player, cost.item(), cost.count());
        }
        return true;
    }

    public static ItemStack stack(EchoNpcTradeCost cost) {
        Item item = BuiltInRegistries.ITEM.getOptional(cost.item()).orElse(Items.AIR);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, cost.count());
    }

    private static int countItem(ServerPlayer player, Identifier itemId) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            return 0;
        }
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static void removeItem(ServerPlayer player, Identifier itemId, int count) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }
    }
}
