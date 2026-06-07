package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.Config;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class IndexSearchService {
    private IndexSearchService() {
    }

    public static List<Map<String, Object>> filterItems(List<IndexRecipeCache.IndexItemData> items, IndexUiState state) {
        IndexFilterState filters = state.filters();
        String query = normalize(state.searchQuery());
        return items.stream()
                .filter(item -> itemMatches(item, query, filters))
                .sorted(itemComparator(filters.sort()))
                .limit(Math.max(36, Config.UI_MAX_RENDERED_ITEMS.get()))
                .map(IndexRecipeCache.IndexItemData::toMap)
                .toList();
    }

    public static List<Map<String, Object>> filterRecipes(List<IndexRecipeCache.IndexRecipeData> recipes, IndexUiState state) {
        IndexFilterState filters = state.filters();
        String query = normalize(state.searchQuery());
        return recipes.stream()
                .filter(recipe -> recipeMatches(recipe, query, filters))
                .sorted(recipeComparator(filters.sort()))
                .limit(Math.max(36, Config.UI_MAX_RENDERED_ITEMS.get()))
                .map(IndexRecipeCache.IndexRecipeData::toMap)
                .toList();
    }

    public static List<Map<String, Object>> filterMachines(List<IndexRecipeCache.IndexMachineData> machines, IndexUiState state) {
        IndexFilterState filters = state.filters();
        String query = normalize(state.searchQuery());
        return machines.stream()
                .filter(machine -> machineMatches(machine, query, filters))
                .sorted(machineComparator(filters.sort()))
                .limit(Math.max(36, Config.UI_MAX_RENDERED_ITEMS.get()))
                .map(IndexRecipeCache.IndexMachineData::toMap)
                .toList();
    }

    private static boolean itemMatches(IndexRecipeCache.IndexItemData item, String query, IndexFilterState filters) {
        if (!filters.mod().isBlank() && !item.modId().equals(filters.mod())) {
            return false;
        }
        if (!"all".equals(filters.category()) && !item.categoryKey().equals(filters.category())) {
            return false;
        }
        if (filters.favoritesOnly() && !item.favorite()) {
            return false;
        }
        if (!filters.showLocked() && item.locked()) {
            return false;
        }
        return tokensMatch(query, token -> itemToken(item, token));
    }

    private static boolean recipeMatches(IndexRecipeCache.IndexRecipeData recipe, String query, IndexFilterState filters) {
        if (!filters.mod().isBlank() && !recipe.modId().equals(filters.mod())) {
            return false;
        }
        if (!"all".equals(filters.recipeType()) && !recipe.type().equals(filters.recipeType())) {
            return false;
        }
        if (!filters.machine().isBlank() && !recipe.machineId().equals(filters.machine())) {
            return false;
        }
        if (filters.favoritesOnly() && !recipe.bookmarked()) {
            return false;
        }
        if (!filters.showLocked() && recipe.locked()) {
            return false;
        }
        return tokensMatch(query, token -> recipeToken(recipe, token));
    }

    private static boolean machineMatches(IndexRecipeCache.IndexMachineData machine, String query, IndexFilterState filters) {
        if (!filters.mod().isBlank() && !machine.modId().equals(filters.mod())) {
            return false;
        }
        if (!filters.machine().isBlank() && !machine.id().equals(filters.machine())) {
            return false;
        }
        if (filters.favoritesOnly() && !machine.bookmarked()) {
            return false;
        }
        return tokensMatch(query, token -> machineToken(machine, token));
    }

    private static boolean itemToken(IndexRecipeCache.IndexItemData item, String token) {
        if (token.startsWith("@")) {
            return item.modId().contains(token.substring(1)) || normalize(item.modName()).contains(token.substring(1));
        }
        if (token.startsWith("#")) {
            String tag = token.substring(1);
            return item.tags().stream().anyMatch(value -> normalize(value).contains(tag));
        }
        if (token.startsWith("$")) {
            return item.categoryKey().contains(token.substring(1));
        }
        if ("!favorites".equals(token) || "!favorite".equals(token)) {
            return item.favorite();
        }
        if (token.startsWith("type:")) {
            String type = token.substring("type:".length());
            return "item".equals(type) || item.categoryKey().contains(type);
        }
        String haystack = normalize(item.id() + " " + item.displayName() + " " + item.modName() + " "
                + item.category() + " " + String.join(" ", item.tags()));
        return haystack.contains(token);
    }

    private static boolean recipeToken(IndexRecipeCache.IndexRecipeData recipe, String token) {
        if (token.startsWith("@")) {
            return recipe.modId().contains(token.substring(1)) || normalize(recipe.modName()).contains(token.substring(1));
        }
        if (token.startsWith("$")) {
            return normalize(recipe.machineName()).contains(token.substring(1)) || recipe.machineId().contains(token.substring(1));
        }
        if ("!favorites".equals(token) || "!favorite".equals(token)) {
            return recipe.bookmarked();
        }
        if (token.startsWith("type:")) {
            String type = token.substring("type:".length());
            return "recipe".equals(type) || normalize(recipe.typeName()).contains(type) || recipe.type().contains(type);
        }
        String haystack = normalize(recipe.id() + " " + recipe.title() + " " + recipe.typeName() + " "
                + recipe.machineName() + " " + recipe.modName() + " " + recipe.inputSummary() + " "
                + recipe.outputSummary());
        return haystack.contains(token);
    }

    private static boolean machineToken(IndexRecipeCache.IndexMachineData machine, String token) {
        if (token.startsWith("@")) {
            return machine.modId().contains(token.substring(1)) || normalize(machine.modName()).contains(token.substring(1));
        }
        if (token.startsWith("$")) {
            return normalize(machine.name()).contains(token.substring(1)) || machine.id().contains(token.substring(1));
        }
        if ("!favorites".equals(token) || "!favorite".equals(token)) {
            return machine.bookmarked();
        }
        if (token.startsWith("type:")) {
            String type = token.substring("type:".length());
            return "machine".equals(type) || machine.recipeTypes().stream().anyMatch(value -> normalize(value).contains(type));
        }
        String haystack = normalize(machine.id() + " " + machine.name() + " " + machine.modName() + " "
                + String.join(" ", machine.recipeTypes()));
        return haystack.contains(token);
    }

    private static boolean tokensMatch(String query, TokenPredicate predicate) {
        if (query.isBlank()) {
            return true;
        }
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && !predicate.test(token)) {
                return false;
            }
        }
        return true;
    }

    private static Comparator<IndexRecipeCache.IndexItemData> itemComparator(String sort) {
        return switch (sort) {
            case "mod" -> Comparator.comparing(IndexRecipeCache.IndexItemData::modName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexItemData::displayName, String.CASE_INSENSITIVE_ORDER);
            case "category" -> Comparator.comparing(IndexRecipeCache.IndexItemData::category, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexItemData::displayName, String.CASE_INSENSITIVE_ORDER);
            case "favorites", "favorites_first" -> Comparator.comparing(IndexRecipeCache.IndexItemData::favorite).reversed()
                    .thenComparing(IndexRecipeCache.IndexItemData::displayName, String.CASE_INSENSITIVE_ORDER);
            case "recipe_count" -> Comparator.comparingInt(IndexRecipeCache.IndexItemData::recipeCount).reversed()
                    .thenComparing(IndexRecipeCache.IndexItemData::displayName, String.CASE_INSENSITIVE_ORDER);
            case "usage_count" -> Comparator.comparingInt(IndexRecipeCache.IndexItemData::usageCount).reversed()
                    .thenComparing(IndexRecipeCache.IndexItemData::displayName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(IndexRecipeCache.IndexItemData::displayName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexItemData::id);
        };
    }

    private static Comparator<IndexRecipeCache.IndexRecipeData> recipeComparator(String sort) {
        return switch (sort) {
            case "mod" -> Comparator.comparing(IndexRecipeCache.IndexRecipeData::modName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexRecipeData::title, String.CASE_INSENSITIVE_ORDER);
            case "category", "type" -> Comparator.comparing(IndexRecipeCache.IndexRecipeData::typeName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexRecipeData::title, String.CASE_INSENSITIVE_ORDER);
            case "favorites", "favorites_first" -> Comparator.comparing(IndexRecipeCache.IndexRecipeData::bookmarked).reversed()
                    .thenComparing(IndexRecipeCache.IndexRecipeData::title, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(IndexRecipeCache.IndexRecipeData::title, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexRecipeData::id);
        };
    }

    private static Comparator<IndexRecipeCache.IndexMachineData> machineComparator(String sort) {
        return switch (sort) {
            case "mod" -> Comparator.comparing(IndexRecipeCache.IndexMachineData::modName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexMachineData::name, String.CASE_INSENSITIVE_ORDER);
            case "recipe_count" -> Comparator.comparingInt(IndexRecipeCache.IndexMachineData::recipeCount).reversed()
                    .thenComparing(IndexRecipeCache.IndexMachineData::name, String.CASE_INSENSITIVE_ORDER);
            case "favorites", "favorites_first" -> Comparator.comparing(IndexRecipeCache.IndexMachineData::bookmarked).reversed()
                    .thenComparing(IndexRecipeCache.IndexMachineData::name, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(IndexRecipeCache.IndexMachineData::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(IndexRecipeCache.IndexMachineData::id);
        };
    }

    static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface TokenPredicate {
        boolean test(String token);
    }
}
