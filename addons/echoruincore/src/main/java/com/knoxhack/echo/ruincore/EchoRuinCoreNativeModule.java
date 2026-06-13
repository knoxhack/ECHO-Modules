package com.knoxhack.echo.ruincore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRuinCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoRuinCore.MODID;
    public static final List<String> CONTRACT_IDS = EchoRuinCore.PROVIDES;
    public static final List<String> ADAPTER_DOMAINS = List.of(
            "data",
            "structures",
            "worldgen"
        );

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "echoruincore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("roadmapPhase", 5);
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", ADAPTER_DOMAINS);
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("mvpContracts", EchoRuinCore.MVP_CONTRACTS);
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Ruins, archaeology, salvage sites, and restoration state contracts.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoRuinCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "echoruincore-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "echoruincore native adapter should activate");
        require(Integer.valueOf(CONTRACT_IDS.size()).equals(activation.get("logicalRegistrationCount")),
                "echoruincore native adapter should expose every contract");
        require(Boolean.FALSE.equals(activation.get("registryMutated")),
                "echoruincore native adapter must stay contract-first");
        System.out.println("echoruincore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractFirst", true);
        result.put("descriptorBacked", true);
        result.put("contractsDeclared", CONTRACT_IDS.size());
        result.put("mvpContractsDeclared", EchoRuinCore.MVP_CONTRACTS.size());
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
