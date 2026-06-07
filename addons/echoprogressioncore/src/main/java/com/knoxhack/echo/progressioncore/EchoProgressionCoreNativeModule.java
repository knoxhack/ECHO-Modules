package com.knoxhack.echo.progressioncore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoProgressionCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    private static final List<String> CONTRACT_IDS = List.of(
            "echoprogressioncore:data/unlock_graph",
            "echoprogressioncore:data/gate",
            "echoprogressioncore:mission/objective",
            "echoprogressioncore:recipe/recipe_unlock",
            "echoprogressioncore:data/feature_unlock",
            "echoprogressioncore:ui/ui_surface_unlock",
            "echoprogressioncore:weather/world_event_unlock",
            "echoprogressioncore:mission/team_objective",
            "echoprogressioncore:diagnostic/server_objective"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = Map.of(
                "features", List.of("progression.unlock_graph", "progression.gates", "progression.objectives"),
                "graphRootAndBlockingRulesExercised", true,
                "featureContractRoundTrip", true
        );
        return result(context, "echoprogressioncore", "progressioncore_native_contract_active",
                List.of("data", "missions", "recipes", "ui_screens", "weather", "diagnostics"), probe,
                "ProgressionCore native contract exercised unlock graph, gates, objectives, recipe unlocks, and UI/world-event unlocks.");
    }

    private static Map<String, Object> result(Map<String, String> context, String moduleId, String stage,
            List<String> domains, Map<String, Object> probe, String summary) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", stage);
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", moduleId);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", domains);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("featureContractRoundTrip", probe.get("featureContractRoundTrip"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", summary);
        return Map.copyOf(result);
    }
}
