package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record AutomationOutput(
        Identifier itemId,
        int count,
        String label) {
    public AutomationOutput {
        if (itemId == null) {
            throw new IllegalArgumentException("Automation output requires an item id.");
        }
        count = Math.max(1, count);
        label = label == null || label.isBlank() ? itemId.getPath().replace('_', ' ') : label.strip();
    }

    public ItemStack stack() {
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    public String displayName() {
        return label;
    }

    public String summary() {
        return count + "x " + label;
    }
}
