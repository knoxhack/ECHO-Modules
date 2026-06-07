package com.knoxhack.echoterminal.api.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public record TerminalRecipeSnapshot(
        List<TerminalRecipeCategory> categories,
        List<TerminalRecipeEntry> recipes,
        Map<Identifier, TerminalRecipeCategory> categoryMap,
        Map<Item, List<TerminalRecipeEntry>> recipesByOutput,
        Map<Item, List<TerminalRecipeEntry>> usesByItem,
        Map<Identifier, Integer> recipeCountsByCategory,
        int providerCount) {
    public TerminalRecipeSnapshot {
        categories = List.copyOf(categories == null ? List.of() : categories);
        recipes = List.copyOf(recipes == null ? List.of() : recipes);
        categoryMap = Map.copyOf(categoryMap == null ? Map.of() : categoryMap);
        recipesByOutput = copyIndexed(recipesByOutput);
        usesByItem = copyIndexed(usesByItem);
        recipeCountsByCategory = Map.copyOf(recipeCountsByCategory == null ? Map.of() : recipeCountsByCategory);
        providerCount = Math.max(0, providerCount);
    }

    public static TerminalRecipeSnapshot empty() {
        return new TerminalRecipeSnapshot(List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of(), 0);
    }

    public List<TerminalRecipeEntry> recipesFor(Item item) {
        return item == null ? List.of() : recipesByOutput.getOrDefault(item, List.of());
    }

    public List<TerminalRecipeEntry> usesFor(Item item) {
        return item == null ? List.of() : usesByItem.getOrDefault(item, List.of());
    }

    public int recipeCount(Identifier categoryId) {
        return recipeCountsByCategory.getOrDefault(categoryId, 0);
    }

    private static Map<Item, List<TerminalRecipeEntry>> copyIndexed(Map<Item, List<TerminalRecipeEntry>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<Item, List<TerminalRecipeEntry>> copy = new LinkedHashMap<>();
        for (Map.Entry<Item, List<TerminalRecipeEntry>> entry : source.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
