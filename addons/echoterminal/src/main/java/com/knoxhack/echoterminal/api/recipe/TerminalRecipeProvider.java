package com.knoxhack.echoterminal.api.recipe;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface TerminalRecipeProvider {
    Identifier id();

    default String displayName() {
        String path = id() == null ? "recipes" : id().getPath();
        return path.replace('_', ' ');
    }

    List<TerminalRecipeCategory> categories(Player player);

    List<TerminalRecipeEntry> recipes(Player player);
}
