package com.knoxhack.echoashfallprotocol.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Simple data class for Scrap Press recipes.
 * Uses lazy Suppliers to avoid early ItemStack creation (components not bound yet in setup).
 */
public record ScrapPressRecipe(
    Supplier<? extends ItemLike> inputSupplier,
    int inputCount,
    Supplier<? extends ItemLike> outputSupplier,
    int outputCount,
    int processingTime
) {
    private static final List<ScrapPressRecipe> RECIPES = new ArrayList<>();

    public ItemStack createInputStack() {
        return new ItemStack(inputSupplier.get(), inputCount);
    }

    public ItemStack createOutputStack() {
        return new ItemStack(outputSupplier.get(), outputCount);
    }

    public Item getInputItem() {
        return inputSupplier.get().asItem();
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.is(inputSupplier.get().asItem()) && stack.getCount() >= inputCount;
    }

    public static void registerRecipe(ScrapPressRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static ScrapPressRecipe findRecipe(ItemStack input) {
        if (input.isEmpty()) return null;
        for (ScrapPressRecipe recipe : RECIPES) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Register a recipe using item suppliers (lazy initialization safe).
     */
    public static void register(Supplier<? extends ItemLike> input, int inputCount,
                                Supplier<? extends ItemLike> output, int outputCount,
                                int processingTime) {
        registerRecipe(new ScrapPressRecipe(input, inputCount, output, outputCount, processingTime));
    }

    public static List<ScrapPressRecipe> getAllRecipes() {
        return new ArrayList<>(RECIPES);
    }
}
