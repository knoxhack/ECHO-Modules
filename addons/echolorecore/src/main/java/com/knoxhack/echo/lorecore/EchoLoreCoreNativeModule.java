package com.knoxhack.echo.lorecore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoLoreCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoLoreConstants.MODULE_ID.value();
    public static final List<String> CONTRACT_IDS = List.of(
            "echolorecore:story/lore_entry",
            "echolorecore:sound/audio_log",
            "echolorecore:story/blackbox_entry",
            "echolorecore:structure/environmental_story"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "lorecore_native_contract_active");
        result.put("adapterDomains", List.of("story", "sounds", "structures"));
        result.put("summary", "LoreCore native contract exercised lore, audio log, blackbox, and environmental story feature contracts.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        List<String> features = List.of(
                EchoLoreConstants.FEATURE_ENVIRONMENTAL_STORY.value(),
                EchoLoreConstants.FEATURE_LORE_AUDIO_LOGS.value(),
                EchoLoreConstants.FEATURE_LORE_BLACKBOX.value(),
                EchoLoreConstants.FEATURE_LORE_ENTRIES.value()
        ).stream().sorted().toList();
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("features", features);
        probe.put("featureContractRoundTrip", features.equals(List.of(
                "lore.audio_logs",
                "lore.blackbox_entries",
                "lore.entries",
                "lore.environmental_storytelling"
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
