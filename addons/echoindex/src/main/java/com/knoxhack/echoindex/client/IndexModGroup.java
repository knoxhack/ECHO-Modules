package com.knoxhack.echoindex.client;

import java.util.List;
import net.minecraft.world.item.ItemStack;

record IndexModGroup(
        String modId,
        String displayName,
        ItemStack iconItem,
        int itemCount,
        List<ItemStack> visibleItems,
        List<ItemStack> allItems,
        boolean isCollapsed,
        boolean isPinned,
        boolean isHidden,
        String version,
        Integer accentColor) {
    IndexModGroup {
        modId = modId == null || modId.isBlank() ? "unknown" : modId;
        displayName = displayName == null || displayName.isBlank() ? modId : displayName;
        iconItem = iconItem == null ? ItemStack.EMPTY : iconItem.copy();
        visibleItems = copyItems(visibleItems);
        allItems = copyItems(allItems);
        itemCount = Math.max(0, itemCount);
        version = version == null ? "" : version;
    }

    IndexModGroup withState(List<ItemStack> visible, boolean collapsed, boolean pinned, boolean hidden) {
        return new IndexModGroup(modId, displayName, iconItem, itemCount, visible, allItems,
                collapsed, pinned, hidden, version, accentColor);
    }

    private static List<ItemStack> copyItems(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }
}
