package com.knoxhack.echoashfallprotocol.block.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MachineInventory extends SimpleContainer {
    private final Runnable onChanged;

    public MachineInventory(int size, Runnable onChanged) {
        super(size);
        this.onChanged = onChanged;
    }

    public ItemStack getStackInSlot(int slot) {
        return getItem(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        setItem(slot, stack);
    }

    public void serialize(ValueOutput output) {
        int count = 0;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                output.putInt("Slot_" + count, slot);
                output.store("Stack_" + count, ItemStack.CODEC, stack);
                count++;
            }
        }
        output.putInt("ItemCount", count);
    }

    public void deserialize(ValueInput input) {
        clearContent();
        int count = input.getIntOr("ItemCount", 0);
        for (int i = 0; i < count; i++) {
            int slot = input.getIntOr("Slot_" + i, -1);
            ItemStack stack = input.read("Stack_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (slot >= 0 && slot < getContainerSize() && !stack.isEmpty()) {
                setItem(slot, stack);
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        onChanged.run();
    }
}
