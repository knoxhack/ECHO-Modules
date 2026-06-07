package com.knoxhack.echo.inputcore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoInputCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoInputConstants.MOD_ID;
    public static final List<String> CONTRACT_IDS = List.of(
            "echoinputcore:input/context",
            "echoinputcore:input/keybind_registry",
            "echoinputcore:ui/radial_menu",
            "echoinputcore:input/controller_ready"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> routePriority = EchoInputRoutePriorityContract.executeReferenceRoutePriority(
                context.getOrDefault("packId", "unknown")
        );
        boolean routePriorityPassed = EchoInputRoutePriorityContract.referenceRoutePriorityPassed(routePriority);
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "inputcore_native_route_priority_active");
        result.put("adapterDomains", List.of("input", "ui_screens"));
        result.put("routePriority", routePriority);
        result.put("routePriorityExecuted", routePriorityPassed);
        result.put("serviceCodeExecuted", routePriorityPassed);
        result.put("summary", "InputCore native contract exercised terminal-focus route priority through the AdapterCore input context contract.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        List<String> features = List.of(
                EchoInputConstants.FEATURE_CONTROLLER_READY,
                EchoInputConstants.FEATURE_INPUT_CONTEXTS,
                EchoInputConstants.FEATURE_KEYBIND_REGISTRY,
                EchoInputConstants.FEATURE_RADIAL_MENUS
        ).stream().sorted().toList();
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("features", features);
        probe.put("featureContractRoundTrip", features.equals(List.of(
                "input.contexts",
                "input.controller_ready",
                "input.keybind_registry",
                "input.radial_menus"
        )));
        return Map.copyOf(probe);
    }

    private Map<String, Object> baseResult(Map<String, String> context, Map<String, Object> probe) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", false);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("featureContractRoundTrip", probe.get("featureContractRoundTrip"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        return result;
    }
}
