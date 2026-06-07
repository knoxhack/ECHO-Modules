package com.knoxhack.echoterminal.api.recipe;

import com.knoxhack.echoterminal.api.TerminalApiIds;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record TerminalRecipeEntry(
        Identifier id,
        Identifier categoryId,
        String title,
        ItemStack machine,
        List<TerminalRecipeSlot> slots,
        List<TerminalRecipeNote> notes,
        int processTicks,
        boolean locked) {
    public TerminalRecipeEntry {
        id = TerminalApiIds.requireLowercase(id, "Terminal recipe");
        categoryId = TerminalApiIds.requireLowercase(categoryId, "Terminal recipe category");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        machine = machine == null ? ItemStack.EMPTY : machine.copy();
        slots = slots == null ? List.of() : List.copyOf(slots);
        notes = notes == null ? List.of() : List.copyOf(notes);
        processTicks = Math.max(0, processTicks);
    }

    public boolean outputs(Item item) {
        return contains(item, TerminalRecipeSlot.Role.OUTPUT);
    }

    public boolean uses(Item item) {
        return contains(item, TerminalRecipeSlot.Role.INPUT)
                || contains(item, TerminalRecipeSlot.Role.CATALYST)
                || contains(item, TerminalRecipeSlot.Role.MACHINE);
    }

    public boolean mentions(Item item) {
        return outputs(item) || uses(item) || contains(item, TerminalRecipeSlot.Role.INFO);
    }

    public Set<Item> itemsForRole(TerminalRecipeSlot.Role role) {
        Set<Item> items = new LinkedHashSet<>();
        if (role == TerminalRecipeSlot.Role.MACHINE && !machine.isEmpty()) {
            items.add(machine.getItem());
        }
        for (TerminalRecipeSlot slot : slots) {
            if (slot.role() != role) {
                continue;
            }
            for (ItemStack stack : slot.stacks()) {
                if (!stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            }
        }
        return Set.copyOf(items);
    }

    private boolean contains(Item item, TerminalRecipeSlot.Role role) {
        if (item == null) {
            return false;
        }
        if (role == TerminalRecipeSlot.Role.MACHINE && !machine.isEmpty() && machine.is(item)) {
            return true;
        }
        for (TerminalRecipeSlot slot : slots) {
            if (slot.role() != role) {
                continue;
            }
            for (ItemStack stack : slot.stacks()) {
                if (!stack.isEmpty() && stack.is(item)) {
                    return true;
                }
            }
        }
        return false;
    }
}
