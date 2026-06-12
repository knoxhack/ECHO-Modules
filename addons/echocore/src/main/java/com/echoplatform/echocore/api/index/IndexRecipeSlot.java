package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record IndexRecipeSlot(IndexSlotRole role, List<ItemStack> stacks, String label) {
    public IndexRecipeSlot {
        role = role == null ? IndexSlotRole.INFO : role;
        stacks = stacks == null ? List.of() : List.copyOf(stacks);
        label = label == null ? "" : label;
    }

    public static IndexRecipeSlot of(IndexSlotRole role, ItemStack stack, String label) {
        return new IndexRecipeSlot(role, stack == null || stack.isEmpty() ? List.of() : List.of(stack), label);
    }

    public static IndexRecipeSlot input(ItemStack stack) {
        return of(IndexSlotRole.INPUT, stack, "");
    }

    public static IndexRecipeSlot inputs(List<ItemStack> stacks) {
        return new IndexRecipeSlot(IndexSlotRole.INPUT, stacks, "");
    }

    public static IndexRecipeSlot output(ItemStack stack) {
        return of(IndexSlotRole.OUTPUT, stack, "");
    }

    public static IndexRecipeSlot machine(ItemStack stack) {
        return of(IndexSlotRole.MACHINE, stack, "Machine");
    }

    public static IndexRecipeSlot catalyst(ItemStack stack, String label) {
        return of(IndexSlotRole.CATALYST, stack, label);
    }

    public static IndexRecipeSlot info(String label) {
        return new IndexRecipeSlot(IndexSlotRole.INFO, List.of(), label);
    }
}
