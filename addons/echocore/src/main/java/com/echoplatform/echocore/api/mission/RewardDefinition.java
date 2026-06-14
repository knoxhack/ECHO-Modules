package com.echoplatform.echocore.api.mission;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

public record RewardDefinition(
        Identifier id,
        MissionRewardClaimMode claimMode,
        ItemStack stack,
        String label,
        String detail,
        Map<String, String> metadata) implements IRewardView {
    public RewardDefinition {
        claimMode = claimMode == null ? MissionRewardClaimMode.IMMEDIATE : claimMode;
        stack = stack == null ? ItemStack.EMPTY : stack;
        label = label == null ? "" : label;
        detail = detail == null ? "" : detail;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static RewardDefinition item(Identifier id, MissionRewardClaimMode claimMode, ItemStack stack) {
        return new RewardDefinition(id, claimMode, stack, safeLabel(stack), "", Map.of());
    }

    public static RewardDefinition text(Identifier id, String label, String detail) {
        return new RewardDefinition(id, MissionRewardClaimMode.IMMEDIATE, ItemStack.EMPTY, label, detail, Map.of());
    }

    public boolean claimable() {
        return true;
    }

    public boolean claimed() {
        return false;
    }

    private static String safeLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            return stack.getHoverName().getString();
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }
}
