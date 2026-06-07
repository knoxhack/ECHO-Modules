package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexRecipeView;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class IndexRecipeTransferService {
    private IndexRecipeTransferService() {
    }

    public static boolean transfer(ServerPlayer player, Identifier recipeId) {
        if (player == null || recipeId == null) {
            return fail(player, "No recipe selected.");
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return fail(player, "Recipe transfer is server-only.");
        }
        Optional<RecipeHolder<CraftingRecipe>> maybeRecipe = findCraftingRecipe(level, recipeId);
        if (maybeRecipe.isEmpty()) {
            return fail(player, "Recipe is not transferable.");
        }
        if (!(player.containerMenu instanceof AbstractCraftingMenu menu)) {
            return fail(player, "Open the inventory crafting grid or a crafting table.");
        }
        if (!gridEmpty(menu)) {
            return fail(player, "Clear the crafting grid first.");
        }
        RecipeHolder<CraftingRecipe> recipe = maybeRecipe.get();
        if (!fits(menu, recipe.value())) {
            return fail(player, "Requires crafting table.");
        }
        StackedItemContents contents = new StackedItemContents();
        player.getInventory().fillStackedContents(contents);
        if (!contents.canCraft(recipe.value(), null)) {
            return fail(player, "Missing ingredients.");
        }
        RecipeBookMenu.PostPlaceAction action = menu.handlePlacement(false, false, recipe, level, player.getInventory());
        menu.broadcastChanges();
        if (action == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE || gridEmpty(menu)) {
            return fail(player, "Missing ingredients.");
        }
        player.sendSystemMessage(Component.literal("ECHO Index // Recipe transferred."));
        return true;
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findCraftingRecipe(ServerLevel level, Identifier recipeId) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
        Optional<RecipeHolder<?>> direct = level.getServer().getRecipeManager().byKey(key);
        if (direct.isPresent() && direct.get().value() instanceof CraftingRecipe crafting) {
            return Optional.of(new RecipeHolder<>(direct.get().id(), crafting));
        }
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe crafting)) {
                continue;
            }
            if (recipeId.equals(VanillaIndexRecipeProvider.transferAlias(holder))) {
                return Optional.of(new RecipeHolder<>(holder.id(), crafting));
            }
        }
        return Optional.empty();
    }

    private static boolean gridEmpty(AbstractCraftingMenu menu) {
        for (Slot slot : menu.getInputGridSlots()) {
            if (slot.hasItem()) {
                return false;
            }
        }
        return true;
    }

    private static boolean fits(AbstractCraftingMenu menu, CraftingRecipe recipe) {
        int gridSize = menu.getGridWidth() * menu.getGridHeight();
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth() <= menu.getGridWidth() && shaped.getHeight() <= menu.getGridHeight();
        }
        return recipe.placementInfo().ingredients().size() <= gridSize;
    }

    private static boolean fail(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal("ECHO Index // " + message));
        }
        return false;
    }

    public static boolean transferAllowedFor(IndexRecipeView recipe) {
        return recipe != null && IndexRecipePlanner.isCraftingRecipe(recipe)
                && !IndexRecipeSourceKind.isSourceCard(recipe);
    }
}
