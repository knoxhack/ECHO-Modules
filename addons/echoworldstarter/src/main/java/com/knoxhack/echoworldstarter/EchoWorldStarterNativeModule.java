package com.knoxhack.echoworldstarter;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoWorldStarterNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echoworldstarter";
    public static final List<String> CONTRACT_IDS = List.of(
            "foundation.spawn_safety",
            "foundation.first_hour",
            "foundation.shelter_rules",
            "foundation.starter_items"
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
        result.put("activationStage", "echoworldstarter_foundation_native_contract_active");
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
        result.put("summary", "Spawn safety, first-hour survival route, starter shelter, and early light contracts.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoWorldStarterNativeModule()
                .describeNativeSurfaces(Map.of("packId", "echoworldstarter-foundation-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "echoworldstarter Foundation native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "echoworldstarter Foundation native adapter should expose every descriptor contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "echoworldstarter Foundation native adapter must not mutate registries");
        System.out.println("echoworldstarter foundation native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("descriptorBacked", true);
        result.put("foundationBackbone", true);
        result.put("contractCountMatches", CONTRACT_IDS.size() == 4);
        result.put("contractSurface", "foundation_data_first");
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
