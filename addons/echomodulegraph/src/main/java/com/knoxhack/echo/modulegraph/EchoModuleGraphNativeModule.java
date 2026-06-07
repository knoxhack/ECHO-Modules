package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.metadatacore.EchoMetadataDependency;
import com.knoxhack.echo.platformcore.EchoModuleId;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoModuleGraphNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoModuleGraphConstants.MOD_ID;
    public static final String MODULE_GRAPH_CONTRACT_ID = "echomodulegraph:data/module_graph";
    public static final String GRAPH_VALIDATION_CONTRACT_ID = "echomodulegraph:diagnostic/graph_validation";
    public static final List<String> CONTRACT_IDS = List.of(
            MODULE_GRAPH_CONTRACT_ID,
            GRAPH_VALIDATION_CONTRACT_ID
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "modulegraph_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("moduleGraphRoundTrip", referenceProbe.get("moduleGraphRoundTrip"));
        result.put("graphValidationRoundTrip", referenceProbe.get("graphValidationRoundTrip"));
        result.put("referenceProbe", referenceProbe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "ModuleGraph native contract exercised module indexing, dependency ordering, duplicate detection, and validation diagnostics.");
        return Map.copyOf(result);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoModuleGraphNativeModule()
                .describeNativeSurfaces(Map.of("packId", "modulegraph-smoke"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "ModuleGraph native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("moduleGraphRoundTrip")),
                "ModuleGraph native adapter should exercise graph behavior");
        require(Boolean.TRUE.equals(activation.get("graphValidationRoundTrip")),
                "ModuleGraph native adapter should exercise validation behavior");
        System.out.println("modulegraph native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        EchoModuleId coreId = EchoModuleId.of("echocore");
        EchoModuleId graphId = EchoModuleId.of("echomodulegraph");
        EchoModuleId missingId = EchoModuleId.of("echomissingcore");
        EchoScannedModule core = scanned(coreId, "Echo Core", List.of(), List.of());
        EchoScannedModule graph = scanned(graphId, "Module Graph",
                List.of(EchoMetadataDependency.required(coreId, ">=1.0.0", "core graph dependency")),
                List.of(EchoMetadataDependency.optional(missingId, ">=1.0.0", "optional missing graph dependency")));
        EchoScannedModule duplicateCore = scanned(coreId, "Echo Core Duplicate", List.of(), List.of());

        EchoModuleGraph cleanGraph = EchoModuleGraph.of(List.of(core, graph));
        EchoModuleGraph duplicateGraph = EchoModuleGraph.of(List.of(core, graph, duplicateCore));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleGraphRoundTrip", cleanGraph.modules().size() == 2
                && cleanGraph.nodes().size() == 2
                && cleanGraph.loadPlan().loadOrder().equals(List.of(coreId, graphId))
                && cleanGraph.loadPlan().degradedModules().contains(graphId)
                && cleanGraph.duplicateModuleIds().isEmpty());
        result.put("graphValidationRoundTrip", !duplicateGraph.duplicateModuleIds().isEmpty()
                && !duplicateGraph.valid()
                && duplicateGraph.diagnostics().stream().anyMatch(diagnostic -> diagnostic.blocking()));
        result.put("loadOrder", cleanGraph.loadPlan().loadOrder().stream().map(EchoModuleId::value).toList());
        result.put("degradedModules", cleanGraph.loadPlan().degradedModules().stream().map(EchoModuleId::value).sorted().toList());
        result.put("duplicateModuleIds", duplicateGraph.duplicateModuleIds().stream().map(EchoModuleId::value).sorted().toList());
        result.put("diagnosticCount", duplicateGraph.diagnostics().size());
        return Map.copyOf(result);
    }

    private static EchoScannedModule scanned(
            EchoModuleId moduleId,
            String displayName,
            List<EchoMetadataDependency> required,
            List<EchoMetadataDependency> optional
    ) {
        return new EchoScannedModule(
                moduleId,
                displayName,
                null,
                null,
                null,
                "addons/" + moduleId.value(),
                true,
                true,
                true,
                Set.of(),
                Set.of(),
                Set.of(),
                required,
                optional,
                List.of(),
                Map.of()
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
