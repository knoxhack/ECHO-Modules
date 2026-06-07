package com.knoxhack.echoterminal.api.mission;

import net.minecraft.world.item.ItemStack;

public record TerminalMissionReward(ItemStack stack, String label, String detail) {
    public TerminalMissionReward {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
        label = label == null || label.isBlank()
                ? (stack.isEmpty() ? "Reward" : stack.getHoverName().getString())
                : label;
        detail = detail == null ? "" : detail;
    }

    public static TerminalMissionReward of(ItemStack stack) {
        return new TerminalMissionReward(stack, "", "");
    }

    public static TerminalMissionReward text(String label, String detail) {
        return new TerminalMissionReward(ItemStack.EMPTY, label, detail);
    }
}
