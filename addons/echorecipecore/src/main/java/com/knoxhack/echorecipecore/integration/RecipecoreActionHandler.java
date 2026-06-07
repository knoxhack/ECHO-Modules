package com.knoxhack.echorecipecore.integration;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RecipecoreActionHandler extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echorecipecore:action_host";
    private static final String ACTION_RECIPE_UNLOCK = "recipecore.recipe_unlock";
    private static final String ACTION_RECIPE_CRAFT = "recipecore.recipe_craft";
    private static final RecipecoreActionHandler HOST = new RecipecoreActionHandler();

    private RecipecoreActionHandler() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.WorldState",
                        "EchoNativeRuntimeHost.Events",
                        "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                        ACTION_RECIPE_UNLOCK,
                        ACTION_RECIPE_CRAFT),
                Set.of(),
                true,
                false,
                true));

        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_RECIPE_UNLOCK, RecipecoreActionHandler::dispatchRecipeUnlock);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_RECIPE_CRAFT, RecipecoreActionHandler::dispatchRecipeCraft);
    }

    private static EchoRuntimeActionOutcome dispatchRecipeUnlock(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_unlock");

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("recipe", action.inputPayload().getOrDefault("recipe", "unknown"));
        after.put("phase", "after_unlock");

        NativeResult result = NativeResult.mutated("Recipe unlocked.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }

    private static EchoRuntimeActionOutcome dispatchRecipeCraft(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_recipe_craft");

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("recipe", action.inputPayload().getOrDefault("recipe", "unknown"));
        after.put("output", action.inputPayload().getOrDefault("output", "unknown"));
        after.put("phase", "after_recipe_craft");

        NativeResult result = NativeResult.mutated("Recipe crafted.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }
}
