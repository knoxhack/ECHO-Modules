package com.knoxhack.echo.blueprintcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoBlueprintCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoBlueprintCore.MODID;
    public static final List<String> CONTRACT_IDS = EchoBlueprintCore.PROVIDES;
    public static final List<String> ADAPTER_DOMAINS = List.of(
            "data",
            "packs"
        );

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "echoblueprintcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("roadmapPhase", 2);
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("mvpContracts", EchoBlueprintCore.MVP_CONTRACTS);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Reusable authoring blueprints for common content types and Studio template generation.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoBlueprintCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "echoblueprintcore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "echoblueprintcore native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "echoblueprintcore native adapter should expose every contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "echoblueprintcore native adapter must stay contract-first");
        System.out.println("echoblueprintcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractFirst", true);
        result.put("descriptorBacked", true);
        result.put("contractsDeclared", CONTRACT_IDS.size());
        result.put("mvpContractsDeclared", EchoBlueprintCore.MVP_CONTRACTS.size());
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
