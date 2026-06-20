package com.knoxhack.echoterminal.api.mission;

import net.minecraft.world.item.ItemStack;

public record TerminalMissionReward(ItemStack stack, String label, String detail) {
    public TerminalMissionReward {
        stack = safeCopy(stack);
        label = label == null || label.isBlank()
                ? (safeIsEmpty(stack) ? "Reward" : safeHoverName(stack, "Reward"))
                : label;
        detail = detail == null ? "" : detail;
    }

    public static TerminalMissionReward of(ItemStack stack) {
        return new TerminalMissionReward(stack, "", "");
    }

    public static TerminalMissionReward text(String label, String detail) {
        return new TerminalMissionReward(ItemStack.EMPTY, label, detail);
    }

    private static ItemStack safeCopy(ItemStack stack) {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        try {
            return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        } catch (RuntimeException | LinkageError exception) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean safeIsEmpty(ItemStack stack) {
        if (stack == null) {
            return true;
        }
        try {
            return stack.isEmpty();
        } catch (RuntimeException | LinkageError exception) {
            return true;
        }
    }

    private static String safeHoverName(ItemStack stack, String fallback) {
        try {
            return stack.getHoverName().getString();
        } catch (RuntimeException | LinkageError exception) {
            return fallback;
        }
    }
}
