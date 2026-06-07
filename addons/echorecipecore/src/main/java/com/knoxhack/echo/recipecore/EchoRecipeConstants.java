package com.knoxhack.echo.recipecore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

public final class EchoRecipeConstants {
    public static final String MOD_ID = "echorecipecore";
    public static final String CONTRACT_VERSION = "1.0.0";

    public static final EchoFeatureId RECIPE_BACKEND = EchoFeatureId.of("recipes.backend");
    public static final EchoFeatureId RECIPE_SEARCH = EchoFeatureId.of("recipes.search");
    public static final EchoFeatureId RECIPE_USAGE_GRAPH = EchoFeatureId.of("recipes.usage_graph");
    public static final EchoFeatureId MACHINE_RECIPE_VIEWS = EchoFeatureId.of("recipes.machine_views");
    public static final EchoFeatureId INDEX_BACKEND = EchoFeatureId.of("index.recipe_backend");

    private EchoRecipeConstants() {
    }
}
