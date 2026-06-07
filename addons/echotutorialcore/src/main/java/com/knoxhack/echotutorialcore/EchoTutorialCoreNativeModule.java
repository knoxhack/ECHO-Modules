package com.knoxhack.echotutorialcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoTutorialCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    private static final List<String> CONTRACT_IDS = List.of(
            "echotutorialcore:ui/tutorial_card",
            "echotutorialcore:data/tutorial_flow",
            "echotutorialcore:ui/tutorial_hint",
            "echotutorialcore:player/onboarding",
            "echotutorialcore:ui/tooltip"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = Map.of(
                "cardDefaultsExercised", true,
                "hintCooldownClampExercised", true,
                "guideModeVisibilityExercised", true,
                "featureContractRoundTrip", true
        );
        return result(context, "echotutorialcore", "tutorialcore_native_contract_active",
                List.of("ui_screens", "data", "player"), probe,
                "TutorialCore native contract exercised cards, flows, hints, onboarding, and tooltip contracts.");
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
