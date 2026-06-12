package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface IIndexRecipeProvider {
    Identifier id();

    default List<IndexRecipeCategory> recipeCategories(Player player) {
        return List.of();
    }

    default List<IndexRecipeView> recipes(Player player) {
        return List.of();
    }
}
