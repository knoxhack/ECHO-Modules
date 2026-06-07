package com.knoxhack.echoindex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoIndexRecipeQueryContract {
    public static final String MODULE_ID = "echoindex";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoindex:recipe_search/index_query";
    public static final String REFERENCE_QUERY = "power cell";
    public static final String REFERENCE_RECIPE_ID = "echoashfallprotocol:power_cell";

    private EchoIndexRecipeQueryContract() {
    }

    public static Map<String, Object> executeReferenceQuery(String query) {
        String normalizedQuery = normalize(query);
        List<Map<String, Object>> recipes = sampleRecipes();
        List<Map<String, Object>> matches = recipes.stream()
                .filter(recipe -> searchableText(recipe).contains(normalizedQuery))
                .toList();
        Map<String, Object> selected = matches.isEmpty() ? Map.of() : matches.get(0);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        report.put("service", "echoindex:index_service");
        report.put("query", normalizedQuery);
        report.put("queryExecuted", true);
        report.put("matchedCount", matches.size());
        report.put("resultIds", matches.stream().map(recipe -> String.valueOf(recipe.get("id"))).toList());
        report.put("selectedRecipeId", String.valueOf(selected.getOrDefault("id", "")));
        report.put("selectedTitle", String.valueOf(selected.getOrDefault("title", "")));
        report.put("inputIds", list(selected.get("inputs")));
        report.put("outputIds", list(selected.get("outputs")));
        report.put("referenceBehavior", "recipe_query_resolves_power_cell");
        return Map.copyOf(report);
    }

    public static boolean referenceQueryPassed(Map<String, Object> report) {
        return Boolean.TRUE.equals(report.get("queryExecuted"))
                && REFERENCE_RECIPE_ID.equals(report.get("selectedRecipeId"))
                && list(report.get("inputIds")).contains("echoashfallprotocol:energy_cell")
                && list(report.get("outputIds")).contains("echoashfallprotocol:power_cell");
    }

    private static List<Map<String, Object>> sampleRecipes() {
        return List.of(
                recipe(
                        "echoashfallprotocol:power_cell",
                        "Power Cell",
                        List.of("echoashfallprotocol:energy_cell", "minecraft:copper_ingot", "minecraft:redstone"),
                        List.of("echoashfallprotocol:power_cell")
                ),
                recipe(
                        "echoashfallprotocol:clean_water",
                        "Clean Water",
                        List.of("echoashfallprotocol:dirty_water", "minecraft:charcoal"),
                        List.of("echoashfallprotocol:clean_water")
                ),
                recipe(
                        "echoashfallprotocol:gas_mask",
                        "Gas Mask",
                        List.of("minecraft:leather", "minecraft:glass", "echoashfallprotocol:filter"),
                        List.of("echoashfallprotocol:gas_mask")
                )
        );
    }

    private static Map<String, Object> recipe(
            String id,
            String title,
            List<String> inputs,
            List<String> outputs
    ) {
        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("id", id);
        recipe.put("title", title);
        recipe.put("inputs", List.copyOf(inputs));
        recipe.put("outputs", List.copyOf(outputs));
        return Map.copyOf(recipe);
    }

    private static String searchableText(Map<String, Object> recipe) {
        return normalize(recipe.get("id") + " "
                + recipe.get("title") + " "
                + String.join(" ", list(recipe.get("inputs"))) + " "
                + String.join(" ", list(recipe.get("outputs"))));
    }

    private static String normalize(Object value) {
        return String.valueOf(value).toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
