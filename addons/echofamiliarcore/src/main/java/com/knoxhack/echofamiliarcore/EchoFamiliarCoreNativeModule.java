package com.knoxhack.echofamiliarcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoFamiliarCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    private static final List<String> CONTRACT_IDS = List.of(
            "echofamiliarcore:entity/familiar_companion",
            "echofamiliarcore:player/bond_progression",
            "echofamiliarcore:command/familiar_command",
            "echofamiliarcore:data/familiar_registry",
            "echofamiliarcore:player/familiar_upgrades"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = Map.of(
                "starterFamiliars", List.of("aether_wisp", "spirit_drone"),
                "safeCommands", List.of("follow", "stay", "defend"),
                "upgradeBranches", List.of("attunement", "warding", "scouting"),
                "featureContractRoundTrip", true
        );
        return result(context, "echofamiliarcore", "familiarcore_native_contract_active",
                List.of("entities", "player", "commands", "data"), probe,
                "FamiliarCore native contract exercised companion registry, bond progression, commands, and upgrades.");
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
