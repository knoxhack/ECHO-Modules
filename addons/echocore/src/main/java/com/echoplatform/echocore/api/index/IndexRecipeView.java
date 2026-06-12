package com.echoplatform.echocore.api.index;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record IndexRecipeView(
        Identifier id,
        Identifier categoryId,
        String title,
        ItemStack machine,
        List<IndexRecipeSlot> slots,
        List<String> notes,
        int processTicks,
        boolean locked,
        String sourceModId) {
    public IndexRecipeView {
        title = title == null ? "" : title;
        machine = machine == null ? ItemStack.EMPTY : machine;
        slots = slots == null ? List.of() : List.copyOf(slots);
        notes = notes == null ? List.of() : List.copyOf(notes);
        sourceModId = sourceModId == null ? "" : sourceModId;
    }

    public boolean source() {
        return false;
    }

    public Set<Item> itemsForRole(IndexSlotRole role) {
        return slots.stream()
                .filter(slot -> slot.role() == role)
                .flatMap(slot -> slot.stacks().stream())
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::getItem)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
