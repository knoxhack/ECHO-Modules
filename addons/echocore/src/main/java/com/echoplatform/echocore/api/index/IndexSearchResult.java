package com.echoplatform.echocore.api.index;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record IndexSearchResult(Identifier id, String kind, String title, ItemStack icon, int score) {
    public IndexSearchResult {
        kind = kind == null ? "" : kind;
        title = title == null ? "" : title;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
    }
}
