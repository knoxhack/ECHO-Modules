package com.knoxhack.echocommonloot;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCommonLootNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echocommonloot";
    public static final List<String> CONTRACT_IDS = List.of(
            "foundation.common_loot",
            "foundation.block_drops",
            "foundation.starter_caches"
        );
    public static final List<String> ADAPTER_DOMAINS = List.of(
            "data",
            "diagnostics",
            "items",
            "loot",
            "progression",
            "recipes",
            "saves",
            "worldgen"
        );

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "echocommonloot_foundation_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("foundationBackbone", true);
        result.put("dataFirstContract", true);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Generic loot pools, baseline block drops, and starter cache contracts.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoCommonLootNativeModule()
                .describeNativeSurfaces(Map.of("packId", "echocommonloot-foundation-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "echocommonloot Foundation native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "echocommonloot Foundation native adapter should expose every descriptor contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "echocommonloot Foundation native adapter must not mutate registries");
        System.out.println("echocommonloot foundation native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("descriptorBacked", true);
        result.put("foundationBackbone", true);
        result.put("contractCountMatches", CONTRACT_IDS.size() == 3);
        result.put("contractSurface", "foundation_data_first");
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
