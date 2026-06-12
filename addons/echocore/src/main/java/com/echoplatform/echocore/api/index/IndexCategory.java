package com.echoplatform.echocore.api.index;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record IndexCategory(
        Identifier id,
        String title,
        String summary,
        ItemStack icon,
        int order,
        String sourceModule) {
    public IndexCategory {
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        sourceModule = sourceModule == null ? "" : sourceModule;
    }

    public String titleKey() {
        return title;
    }

    public String summaryKey() {
        return summary;
    }

    public int sortOrder() {
        return order;
    }
}
