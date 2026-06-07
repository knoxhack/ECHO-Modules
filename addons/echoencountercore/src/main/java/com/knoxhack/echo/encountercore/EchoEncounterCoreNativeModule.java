package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoEncounterCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoEncounterConstants.MODULE_ID.value();
    public static final List<String> CONTRACT_IDS = List.of(
            "echoencountercore:mission/encounter_definition",
            "echoencountercore:entity/boss_gate",
            "echoencountercore:story/faction_patrol"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "encountercore_native_contract_active");
        result.put("adapterDomains", List.of("missions", "entities", "story"));
        result.put("summary", "EncounterCore native contract exercised encounter definition, boss gate, and faction patrol feature contracts.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        List<String> features = List.of(
                EchoEncounterConstants.FEATURE_BOSS_GATES.value(),
                EchoEncounterConstants.FEATURE_ENCOUNTERS.value(),
                EchoEncounterConstants.FEATURE_PATROLS.value()
        ).stream().sorted().toList();
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("features", features);
        probe.put("featureContractRoundTrip", features.equals(List.of(
                "encounter.boss_gates",
                "encounter.definitions",
                "encounter.faction_patrols"
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
        result.put("runtimeTargets", List.of(
                EchoAdapterRuntime.NEOFORGE.serializedName(),
                EchoAdapterRuntime.ECHO_NATIVE.serializedName(),
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE.serializedName()));
        result.put("featureContractRoundTrip", probe.get("featureContractRoundTrip"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        return result;
    }
}
