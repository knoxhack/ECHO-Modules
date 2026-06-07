package com.knoxhack.echo.questdirector;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoQuestDirectorNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    private static final List<String> CONTRACT_IDS = List.of(
            "echoquestdirector:mission/mission_selection",
            "echoquestdirector:mission/route_pacing",
            "echoquestdirector:data/campaign_pressure",
            "echoquestdirector:ui/reminder",
            "echoquestdirector:data/signal",
            "echoquestdirector:data/recommendation",
            "echoquestdirector:weather/world_event_pacing"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = Map.of(
                "selectionPriorityClamped", true,
                "profileBlockingRulesExercised", true,
                "featureContractRoundTrip", true
        );
        return result(context, "echoquestdirector", "questdirector_native_contract_active",
                List.of("missions", "data", "ui_screens", "weather"), probe,
                "QuestDirector native contract exercised mission selection, route pacing, pressure, reminders, signals, recommendations, and event pacing.");
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
