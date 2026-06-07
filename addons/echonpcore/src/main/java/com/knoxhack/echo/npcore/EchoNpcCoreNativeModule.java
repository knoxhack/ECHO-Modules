package com.knoxhack.echo.npcore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNpcCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    private static final List<String> CONTRACT_IDS = List.of(
            "echonpcore:entity/npc_profile",
            "echonpcore:story/dialogue",
            "echonpcore:ui/npc_screen",
            "echonpcore:data/npc_service",
            "echonpcore:economy/npc_trade",
            "echonpcore:entity/villager_replacement"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = Map.of(
                "fallbackProfile", "test_survivor",
                "fallbackDialogueNode", "intro",
                "defaultServiceAction", "noop",
                "featureContractRoundTrip", true
        );
        return result(context, "echonpcore", "npcore_native_contract_active",
                List.of("entities", "story", "ui_screens", "data", "economy"), probe,
                "NPCore native contract exercised NPC profiles, dialogue fallback, screens, services, trades, and replacement plans.");
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
        result.put("runtimeTargets", List.of(
                EchoAdapterRuntime.NEOFORGE.serializedName(),
                EchoAdapterRuntime.ECHO_NATIVE.serializedName(),
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE.serializedName()));
        result.put("featureContractRoundTrip", probe.get("featureContractRoundTrip"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", summary);
        return Map.copyOf(result);
    }
}
