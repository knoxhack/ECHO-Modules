package com.knoxhack.echoterminal.api.recipe;

import com.knoxhack.echoterminal.api.TerminalApiIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record TerminalRecipeCategory(
        Identifier id,
        String title,
        ItemStack icon,
        int accentColor,
        int order) {
    public TerminalRecipeCategory {
        id = TerminalApiIds.requireLowercase(id, "Terminal recipe category");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
    }
}
