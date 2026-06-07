package com.knoxhack.echoashfallprotocol;

import java.util.Map;

public final class AshfallNativeMachineRecipeCatalogVerifier {
    private AshfallNativeMachineRecipeCatalogVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> catalog = AshfallNativeMachineRecipeCatalog.describe();
        require(catalog, "adapterCoreBridge", true);
        require(catalog, "standaloneDuplicateGameplaySystem", false);
        require(catalog, "dataBacked", true);
        require(catalog, "resourceLoaded", true);
        require(catalog, "fallbackUsed", false);
        require(catalog, "resourcePath", "data/echoashfallprotocol/adaptercore/native_machine_recipes.properties");
        require(catalog, "scrapPressRecipeCount", 1);
        require(catalog, "oreGrinderRecipeCount", 28);
        require(catalog, "minecraftRuntimeAccessed", false);
        require(catalog, "minecraftRegistryMutated", false);

        AshfallNativeMachineRecipeCatalog.Recipe press = AshfallNativeMachineRecipeCatalog.scrapPressRecipe("scrap_metal");
        requireRecipe(press, "scrap_metal", 9, "compressed_scrap", 1, 40, 40);
        AshfallNativeMachineRecipeCatalog.Recipe stone = AshfallNativeMachineRecipeCatalog.grinderRecipe("stone");
        requireRecipe(stone, "stone", 4, "gravel", 4, 80, 180);
        AshfallNativeMachineRecipeCatalog.Recipe toxic = AshfallNativeMachineRecipeCatalog.grinderRecipe("toxic_slagstone");
        requireRecipe(toxic, "toxic_slagstone", 2, "coal_dust", 2, 120, 350);

        Map<String, Object> execution = AshfallAdapterCoreMachinePowerRuntime.runRecipeCatalogScenario();
        require(execution, "status", "PASS");
        require(execution, "executedRecipeCount", 3);
        require(execution, "stoneGrinderByproduct", "flint");
        require(execution, "toxicGrinderByproduct", "contaminated_redstone");
        System.out.println("Ashfall native machine recipe catalog verifier PASS");
    }

    private static void requireRecipe(
            AshfallNativeMachineRecipeCatalog.Recipe recipe,
            String input,
            int inputCount,
            String output,
            int outputCount,
            int processingTicks,
            int powerCost) {
        if (!input.equals(recipe.inputId())
                || inputCount != recipe.inputCount()
                || !output.equals(recipe.outputId())
                || outputCount != recipe.outputCount()
                || processingTicks != recipe.processingTicks()
                || powerCost != recipe.powerCost()) {
            throw new IllegalStateException("Recipe mismatch for " + input + ": " + recipe);
        }
    }

    private static void require(Map<?, ?> data, String key, Object expected) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }
}
