package com.knoxhack.echorecovery.content;

import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record RecoveryRuleDefinition(
        Identifier id,
        RecoveryItemRuleResult result,
        SelectorKind selectorKind,
        Identifier selector,
        int priority) {
    public RecoveryRuleDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Recovery rule id is required.");
        }
        if (result == null) {
            throw new IllegalArgumentException("Recovery rule action is required: " + id);
        }
        if (selectorKind == null || selector == null) {
            throw new IllegalArgumentException("Recovery rule selector is required: " + id);
        }
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return switch (selectorKind) {
            case ITEM -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(selector);
            case TAG -> stack.is(TagKey.create(Registries.ITEM, selector));
            case MOD -> selector.getNamespace().equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
        };
    }

    public enum SelectorKind {
        ITEM,
        TAG,
        MOD
    }
}
