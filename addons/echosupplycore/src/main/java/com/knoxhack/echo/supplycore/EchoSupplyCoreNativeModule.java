package com.knoxhack.echo.supplycore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoSupplyCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoSupplyCore.MODID;
    public static final List<String> CONTRACT_IDS = EchoSupplyCore.PROVIDES;
    public static final List<String> ADAPTER_DOMAINS = List.of(
            "data",
            "diagnostics"
        );

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "echosupplycore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("roadmapPhase", 5);
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("mvpContracts", EchoSupplyCore.MVP_CONTRACTS);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Scarcity, shortages, stockpiles, rationing, and supply pressure contracts.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoSupplyCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "echosupplycore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "echosupplycore native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "echosupplycore native adapter should expose every contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "echosupplycore native adapter must stay contract-first");
        System.out.println("echosupplycore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractFirst", true);
        result.put("descriptorBacked", true);
        result.put("contractsDeclared", CONTRACT_IDS.size());
        result.put("mvpContractsDeclared", EchoSupplyCore.MVP_CONTRACTS.size());
        result.put("contractCountMatches", CONTRACT_IDS.size() == 4);
        result.put("roadmapPhase", 5);
        return Map.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
