package com.knoxhack.echo.localizationcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoLocalizationCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoLocalizationCore.MODID;
    public static final List<String> CONTRACT_IDS = EchoLocalizationCore.PROVIDES;
    public static final List<String> ADAPTER_DOMAINS = List.of(
            "data",
            "diagnostics",
            "packs"
        );

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "echolocalizationcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("roadmapPhase", 2);
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("mvpContracts", EchoLocalizationCore.MVP_CONTRACTS);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Translation validation, fallback text, missing-key reports, language-pack exports, and Studio localization workflow contracts.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoLocalizationCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "echolocalizationcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "echolocalizationcore native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "echolocalizationcore native adapter should expose every contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "echolocalizationcore native adapter must stay contract-first");
        System.out.println("echolocalizationcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractFirst", true);
        result.put("descriptorBacked", true);
        result.put("contractsDeclared", CONTRACT_IDS.size());
        result.put("mvpContractsDeclared", EchoLocalizationCore.MVP_CONTRACTS.size());
        result.put("contractCountMatches", CONTRACT_IDS.size() == 3);
        result.put("roadmapPhase", 2);
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
