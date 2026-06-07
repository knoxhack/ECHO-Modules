package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record AutomationIngredient(
        Identifier itemId,
        Identifier tagId,
        int count,
        String label) {
    public AutomationIngredient {
        count = Math.max(1, count);
        label = label == null || label.isBlank() ? defaultLabel(itemId, tagId) : label.strip();
        if (itemId == null && tagId == null) {
            throw new IllegalArgumentException("Automation ingredient requires an item or tag id.");
        }
    }

    public static AutomationIngredient item(Identifier itemId, int count) {
        return new AutomationIngredient(itemId, null, count, "");
    }

    public static AutomationIngredient tag(Identifier tagId, int count, String label) {
        return new AutomationIngredient(null, tagId, count, label);
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (itemId != null) {
            return BuiltInRegistries.ITEM.getOptional(itemId)
                    .map(stack::is)
                    .orElse(false);
        }
        return stack.is(TagKey.create(Registries.ITEM, tagId));
    }

    public List<ItemStack> exampleStacks() {
        if (itemId == null) {
            return List.of();
        }
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(item -> List.of(new ItemStack(item, count)))
                .orElse(List.of());
    }

    public String displayName() {
        return label;
    }

    public String summary() {
        return count + "x " + label;
    }

    private static String defaultLabel(Identifier itemId, Identifier tagId) {
        Identifier id = itemId == null ? tagId : itemId;
        return id == null ? "ingredient" : id.getPath().replace('_', ' ');
    }
}
