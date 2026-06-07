package com.knoxhack.echoterminal.api.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record TerminalRecipeSlot(Role role, List<ItemStack> stacks, String label) {
    public TerminalRecipeSlot {
        role = role == null ? Role.INFO : role;
        stacks = stacks == null ? List.of() : stacks.stream()
                .map(stack -> stack == null ? ItemStack.EMPTY : stack.copy())
                .toList();
        label = label == null ? "" : label.strip();
    }

    public static TerminalRecipeSlot input(ItemLike item) {
        return of(Role.INPUT, new ItemStack(item), "");
    }

    public static TerminalRecipeSlot input(ItemStack stack) {
        return of(Role.INPUT, stack, "");
    }

    public static TerminalRecipeSlot inputs(List<ItemStack> stacks) {
        return new TerminalRecipeSlot(Role.INPUT, stacks, "");
    }

    public static TerminalRecipeSlot output(ItemLike item) {
        return of(Role.OUTPUT, new ItemStack(item), "");
    }

    public static TerminalRecipeSlot output(ItemStack stack) {
        return of(Role.OUTPUT, stack, "");
    }

    public static TerminalRecipeSlot outputs(List<ItemStack> stacks) {
        return new TerminalRecipeSlot(Role.OUTPUT, stacks, "");
    }

    public static TerminalRecipeSlot catalyst(ItemLike item) {
        return of(Role.CATALYST, new ItemStack(item), "Catalyst");
    }

    public static TerminalRecipeSlot catalyst(ItemStack stack) {
        return of(Role.CATALYST, stack, "Catalyst");
    }

    public static TerminalRecipeSlot machine(ItemStack stack) {
        return of(Role.MACHINE, stack, "Machine");
    }

    public static TerminalRecipeSlot info(ItemStack stack, String label) {
        return of(Role.INFO, stack, label);
    }

    public static TerminalRecipeSlot text(Role role, String label) {
        return new TerminalRecipeSlot(role, List.of(), label);
    }

    public static TerminalRecipeSlot text(String label) {
        return text(Role.INFO, label);
    }

    public static TerminalRecipeSlot of(Role role, ItemStack stack, String label) {
        return new TerminalRecipeSlot(role, List.of(stack == null ? ItemStack.EMPTY : stack), label);
    }

    public enum Role {
        INPUT,
        OUTPUT,
        CATALYST,
        MACHINE,
        INFO
    }
}
