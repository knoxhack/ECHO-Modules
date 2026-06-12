package com.echoplatform.echocore.api.index;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public interface IIndexRecipeService {
    boolean registerProvider(IIndexRecipeProvider provider);

    boolean registerSourceProvider(IIndexSourceProvider provider);

    List<IndexRecipeCategory> recipeCategories(Player player);

    List<IndexRecipeView> recipes(Player player);

    List<IndexRecipeView> recipesFor(Player player, Item item);

    List<IndexRecipeView> usesFor(Player player, Item item);

    Optional<IndexRecipeView> recipe(Player player, Identifier id);

    int providerCount();

    List<IndexSourceFact> sourceFacts(Player player);
}
