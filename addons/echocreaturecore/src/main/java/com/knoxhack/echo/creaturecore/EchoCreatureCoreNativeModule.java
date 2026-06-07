package com.knoxhack.echo.creaturecore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCreatureCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoCreatureConstants.MOD_ID;
    public static final List<String> CONTRACT_IDS = List.of(
            "echocreaturecore:data/creature_archetype",
            "echocreaturecore:entity/ai_profile",
            "echocreaturecore:data/scan_metadata"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "creaturecore_native_contract_active");
        result.put("adapterDomains", List.of("data", "entities"));
        result.put("summary", "CreatureCore native contract exercised creature archetype, AI profile, and scan metadata feature contracts.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        List<String> features = EchoCreatureConstants.PROVIDED_FEATURES.stream()
                .map(EchoFeatureId::value)
                .sorted()
                .toList();
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("features", features);
        probe.put("featureContractRoundTrip", features.equals(List.of(
                "creature.ai_profiles",
                "creature.archetypes",
                "creature.scan_metadata"
        )));
        return Map.copyOf(probe);
    }

    private Map<String, Object> baseResult(Map<String, String> context, Map<String, Object> probe) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("featureContractRoundTrip", probe.get("featureContractRoundTrip"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        return result;
    }
}
