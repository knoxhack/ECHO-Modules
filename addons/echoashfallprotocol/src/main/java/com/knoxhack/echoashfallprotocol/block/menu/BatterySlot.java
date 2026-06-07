package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BatterySlot extends Slot {
    public BatterySlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return EnergyAccess.isEnergyItem(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
