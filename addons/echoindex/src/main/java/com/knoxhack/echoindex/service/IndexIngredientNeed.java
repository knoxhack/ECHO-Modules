package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexSlotRole;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public record IndexIngredientNeed(
        IndexSlotRole role,
        String label,
        List<ItemStack> choices,
        ItemStack selected,
        int required,
        int available,
        int missing) {
    public IndexIngredientNeed {
        role = role == null ? IndexSlotRole.INFO : role;
        label = label == null ? "" : label.strip();
        choices = choices == null ? List.of() : choices.stream()
                .map(stack -> stack == null ? ItemStack.EMPTY : stack.copy())
                .filter(stack -> !stack.isEmpty())
                .toList();
        selected = selected == null ? ItemStack.EMPTY : selected.copy();
        required = Math.max(0, required);
        available = Math.max(0, available);
        missing = Math.max(0, missing);
    }

    public boolean satisfied() {
        return missing <= 0;
    }
}
