package com.knoxhack.echoritualcore.ritual;

import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RitualItemAccess {
    private final ServerPlayer player;
    private final ItemStack focus;
    private final List<OfferingPedestalBlockEntity> pedestals;

    public RitualItemAccess(ServerPlayer player, ItemStack focus, List<OfferingPedestalBlockEntity> pedestals) {
        this.player = player;
        this.focus = focus == null ? ItemStack.EMPTY : focus;
        this.pedestals = pedestals == null ? List.of() : List.copyOf(pedestals);
    }

    public boolean has(Item item, int count) {
        if (count <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        return count(item) >= count;
    }

    public int count(Item item) {
        int found = 0;
        if (!focus.isEmpty() && focus.is(item)) {
            found += focus.getCount();
        }
        for (OfferingPedestalBlockEntity pedestal : pedestals) {
            ItemStack stack = pedestal.getItem(OfferingPedestalBlockEntity.SLOT);
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == focus) {
                continue;
            }
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    public boolean consume(Item item, int count) {
        if (!has(item, count)) {
            return false;
        }
        if (count <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        int remaining = count;
        if (!focus.isEmpty() && focus.is(item)) {
            int take = Math.min(remaining, focus.getCount());
            focus.shrink(take);
            remaining -= take;
        }
        for (OfferingPedestalBlockEntity pedestal : pedestals) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stack = pedestal.getItem(OfferingPedestalBlockEntity.SLOT);
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                pedestal.removeItem(OfferingPedestalBlockEntity.SLOT, take);
                remaining -= take;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == focus) {
                continue;
            }
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return remaining <= 0;
    }
}
