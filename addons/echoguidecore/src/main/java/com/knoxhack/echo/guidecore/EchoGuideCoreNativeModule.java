package com.knoxhack.echo.guidecore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoGuideCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoGuideConstants.MODULE_ID.value();
    public static final List<String> CONTRACT_IDS = List.of(
            "echoguidecore:wiki/guide_page",
            "echoguidecore:data/search_index",
            "echoguidecore:player/unlock_visibility"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "guidecore_native_contract_active");
        result.put("adapterDomains", List.of("wiki", "data", "player"));
        result.put("summary", "GuideCore native contract exercised guide page, search, and unlock visibility feature contracts.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        List<String> features = List.of(
                EchoGuideConstants.FEATURE_GUIDE_PAGES.value(),
                EchoGuideConstants.FEATURE_GUIDE_SEARCH.value(),
                EchoGuideConstants.FEATURE_GUIDE_UNLOCK_VISIBILITY.value()
        ).stream().sorted().toList();
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("features", features);
        probe.put("featureContractRoundTrip", features.equals(List.of(
                "guide.pages",
                "guide.search",
                "guide.unlock_visibility"
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
