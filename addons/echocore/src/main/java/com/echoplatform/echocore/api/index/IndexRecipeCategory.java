package com.echoplatform.echocore.api.index;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record IndexRecipeCategory(Identifier id, String title, ItemStack icon, int accentColor, int sortOrder) {
    public IndexRecipeCategory {
        title = title == null ? "" : title;
        icon = icon == null ? ItemStack.EMPTY : icon;
    }

    public int order() {
        return sortOrder;
    }
}
