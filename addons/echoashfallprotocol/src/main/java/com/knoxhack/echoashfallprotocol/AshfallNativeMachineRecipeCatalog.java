package com.knoxhack.echoashfallprotocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

final class AshfallNativeMachineRecipeCatalog {
    private static final String RESOURCE_PATH =
            "data/echoashfallprotocol/adaptercore/native_machine_recipes.properties";

    private AshfallNativeMachineRecipeCatalog() {
    }

    static Map<String, Object> describe() {
        Catalog catalog = loadCatalog();
        List<Recipe> pressRecipes = catalog.scrapPressRecipes();
        List<Recipe> grinderRecipes = catalog.grinderRecipes();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adapterCoreBridge", true);
        data.put("adapterSurface", "recipe.catalog.native_machine_power");
        data.put("implementationTarget", "AdapterCore native recipe catalog mirror");
        data.put("standaloneDuplicateGameplaySystem", false);
        data.put("dataBacked", catalog.resourceLoaded());
        data.put("resourcePath", RESOURCE_PATH);
        data.put("resourceLoaded", catalog.resourceLoaded());
        data.put("fallbackUsed", catalog.fallbackUsed());
        data.put("fallbackReason", catalog.fallbackReason());
        data.put("scrapPressRecipeCount", pressRecipes.size());
        data.put("oreGrinderRecipeCount", grinderRecipes.size());
        data.put("scrapPressRecipes", recipesToMaps(pressRecipes));
        data.put("oreGrinderRecipes", recipesToMaps(grinderRecipes));
        data.put("minecraftRuntimeAccessed", false);
        data.put("minecraftRegistryMutated", false);
        return data;
    }

    static Recipe scrapPressRecipe(String inputId) {
        return find(loadCatalog().scrapPressRecipes(), inputId);
    }

    static Recipe grinderRecipe(String inputId) {
        return find(loadCatalog().grinderRecipes(), inputId);
    }

    private static Catalog loadCatalog() {
        ClassLoader classLoader = AshfallNativeMachineRecipeCatalog.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                return fallbackCatalog("missing resource " + RESOURCE_PATH);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return new Catalog(
                    parseRecipeGroup(properties, "scrap_press"),
                    parseRecipeGroup(properties, "ore_grinder"),
                    true,
                    false,
                    "");
        } catch (IOException exception) {
            return fallbackCatalog("resource read failed: " + exception.getClass().getSimpleName());
        } catch (IllegalArgumentException exception) {
            return fallbackCatalog("resource parse failed: " + exception.getMessage());
        }
    }

    private static List<Recipe> parseRecipeGroup(Properties properties, String group) {
        int count = parseRequiredInt(properties, group + ".count");
        List<Recipe> recipes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String key = group + "." + index;
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("missing recipe key " + key);
            }
            recipes.add(parseRecipe(key, value));
        }
        return List.copyOf(recipes);
    }

    private static Recipe parseRecipe(String key, String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 10) {
            throw new IllegalArgumentException(key + " expected 10 fields but found " + parts.length);
        }
        return new Recipe(
                requireText(key, "inputId", parts[0]),
                parseInt(key, "inputCount", parts[1]),
                requireText(key, "outputId", parts[2]),
                parseInt(key, "outputCount", parts[3]),
                parts[4].trim(),
                parseInt(key, "byproductCount", parts[5]),
                parseFloat(key, "byproductChance", parts[6]),
                parseInt(key, "processingTicks", parts[7]),
                parseInt(key, "powerCost", parts[8]),
                Boolean.parseBoolean(parts[9].trim()));
    }

    private static int parseRequiredInt(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing count key " + key);
        }
        return parseInt(key, "count", value);
    }

    private static String requireText(String key, String field, String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(key + " has blank " + field);
        }
        return trimmed;
    }

    private static int parseInt(String key, String field, String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " has invalid " + field + ": " + value, exception);
        }
    }

    private static float parseFloat(String key, String field, String value) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " has invalid " + field + ": " + value, exception);
        }
    }

    private static Catalog fallbackCatalog(String reason) {
        return new Catalog(scrapPressRecipes(), grinderRecipes(), false, true, reason);
    }

    private static List<Recipe> scrapPressRecipes() {
        return List.of(new Recipe("scrap_metal", 9, "compressed_scrap", 1, "", 0, 0.0f, 40, 40, false));
    }

    private static List<Recipe> grinderRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(fragment("iron_shard", "iron_ingot"));
        recipes.add(fragment("copper_shard", "copper_ingot"));
        recipes.add(fragment("coal_dust", "coal"));
        recipes.add(fragment("gold_trace", "gold_nugget"));
        recipes.add(fragment("gold_cluster", "gold_ingot"));
        recipes.add(fragment("uranium_shard", "raw_iron"));
        recipes.add(recipe("stone", 4, "gravel", 4, "flint", 1, 0.10f, 80, 180));
        recipes.add(recipe("cobblestone", 4, "gravel", 4, "flint", 1, 0.10f, 80, 180));
        recipes.add(recipe("deepslate", 3, "coal_dust", 2, "iron_shard", 1, 0.20f, 100, 240));
        recipes.add(recipe("cobbled_deepslate", 3, "coal_dust", 2, "iron_shard", 1, 0.20f, 100, 240));
        recipes.add(recipe("wasteland_stone", 3, "iron_shard", 2, "coal_dust", 1, 0.25f, 100, 250));
        recipes.add(recipe("wasteland_trace_rubble", 2, "iron_shard", 2, "copper_shard", 1, 0.30f, 90, 260));
        recipes.add(recipe("scrap_ore", 2, "scrap_metal", 3, "iron_shard", 1, 0.35f, 90, 240));
        recipes.add(recipe("rubble", 3, "gravel", 4, "scrap_metal", 1, 0.15f, 80, 200));
        recipes.add(recipe("scattered_bones", 1, "ashbone_shard", 3, "bone_meal", 1, 0.25f, 80, 180));
        recipes.add(recipe("concrete_rubble", 3, "gravel", 4, "scrap_metal", 1, 0.20f, 90, 220));
        recipes.add(recipe("concrete_chunk", 2, "gravel", 4, "scrap_metal", 1, 0.30f, 100, 240));
        recipes.add(recipe("industrial_aggregate", 2, "copper_shard", 2, "scrap_wire", 1, 0.30f, 110, 300));
        recipes.add(recipe("oil_stained_concrete", 2, "scrap_plastic", 2, "coal_dust", 1, 0.35f, 110, 300));
        recipes.add(recipe("crash_slag", 2, "scrap_metal", 2, "iron_shard", 1, 0.35f, 110, 320));
        recipes.add(recipe("ash_stone", 3, "coal_dust", 2, "ash", 1, 0.35f, 100, 240));
        recipes.add(recipe("deep_ash", 2, "sand", 2, "coal_dust", 1, 0.25f, 90, 220));
        recipes.add(recipe("toxic_slagstone", 2, "coal_dust", 2, "charged_ash_circuit", 1, 0.25f, 120, 350));
        recipes.add(recipe("irradiated_crust", 3, "uranium_shard", 1, "fallout_dust", 1, 0.35f, 120, 360));
        recipes.add(recipe("irradiated_shale", 2, "uranium_shard", 1, "crystal_dust", 1, 0.30f, 140, 420));
        recipes.add(recipe("cryogenic_fractured_stone", 2, "crystal_dust", 1, "scrap_circuit", 1, 0.25f, 140, 400));
        recipes.add(recipe("nexus_cracked_soil", 3, "crystal_dust", 2, "gem_fragment", 1, 0.15f, 160, 500));
        recipes.add(recipe("nexus_scar_stone", 2, "gem_fragment", 1, "crystal_dust", 1, 0.35f, 180, 650));
        return List.copyOf(recipes);
    }

    private static Recipe fragment(String inputId, String outputId) {
        return new Recipe(inputId, 4, outputId, 4, "crystal_dust", 1, 0.15f, 80, 200, true);
    }

    private static Recipe recipe(String inputId, int inputCount, String outputId, int outputCount,
                                 String byproductId, int byproductCount, float byproductChance,
                                 int processingTicks, int powerCost) {
        return new Recipe(inputId, inputCount, outputId, outputCount, byproductId, byproductCount,
                byproductChance, processingTicks, powerCost, false);
    }

    private static Recipe find(List<Recipe> recipes, String inputId) {
        for (Recipe recipe : recipes) {
            if (recipe.inputId().equals(inputId)) {
                return recipe;
            }
        }
        throw new IllegalArgumentException("No native Ashfall machine recipe for input " + inputId);
    }

    private static List<Map<String, Object>> recipesToMaps(List<Recipe> recipes) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Recipe recipe : recipes) {
            maps.add(recipe.toMap());
        }
        return List.copyOf(maps);
    }

    private record Catalog(
            List<Recipe> scrapPressRecipes,
            List<Recipe> grinderRecipes,
            boolean resourceLoaded,
            boolean fallbackUsed,
            String fallbackReason
    ) {
    }

    record Recipe(
            String inputId,
            int inputCount,
            String outputId,
            int outputCount,
            String byproductId,
            int byproductCount,
            float byproductChance,
            int processingTicks,
            int powerCost,
            boolean partialBatch
    ) {
        int powerPerTick() {
            return Math.max(1, powerCost / Math.max(1, processingTicks));
        }

        Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("input", inputId);
            data.put("inputCount", inputCount);
            data.put("output", outputId);
            data.put("outputCount", outputCount);
            data.put("byproduct", byproductId);
            data.put("byproductCount", byproductCount);
            data.put("byproductChance", byproductChance);
            data.put("processingTicks", processingTicks);
            data.put("powerCost", powerCost);
            data.put("powerPerTick", powerPerTick());
            data.put("partialBatch", partialBatch);
            return data;
        }
    }
}
