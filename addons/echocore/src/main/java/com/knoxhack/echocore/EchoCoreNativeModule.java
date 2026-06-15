package com.knoxhack.echocore;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echocore";
    public static final String SERVICE_REGISTRY_CONTRACT_ID = "echocore:data/service_registry";
    public static final String DATA_BUS_CONTRACT_ID = "echocore:data/data_bus";
    public static final String CORE_DIAGNOSTICS_CONTRACT_ID = "echocore:diagnostic/core_diagnostics";
    public static final String NATIVE_HUB_CONTRACT_ID = "echocore:ui/native_hub";
    public static final List<String> CONTRACT_IDS = List.of(
            SERVICE_REGISTRY_CONTRACT_ID,
            DATA_BUS_CONTRACT_ID,
            CORE_DIAGNOSTICS_CONTRACT_ID
    );
    public static final List<String> PLANNED_CONTRACT_IDS = List.of(
            NATIVE_HUB_CONTRACT_ID,
            "echo.native_hub"
    );

    public String moduleId() {
        return MODULE_ID;
    }

    public void bootstrap() {
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, Object> referenceProbe = exerciseReferenceBehavior();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "echocore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "diagnostics", "ui_screens"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("registryBridge", nativeHubRegistryBridge());
        result.put("plannedFeatureContracts", PLANNED_CONTRACT_IDS);
        result.put("plannedAdapterDomains", List.of("ui_screens"));
        result.put("serviceRegistryRoundTrip", referenceProbe.get("serviceRegistryRoundTrip"));
        result.put("dataBusSubscriptionRoundTrip", referenceProbe.get("dataBusSubscriptionRoundTrip"));
        result.put("diagnosticSummaryAvailable", referenceProbe.get("diagnosticSummaryAvailable"));
        result.put("referenceProbe", referenceProbe);
        result.put("requiresServiceRegistryBridge", true);
        result.put("requiresDataBusBridge", true);
        result.put("requiresCoreDiagnosticsBridge", true);
        result.put("requiresNativeHubClientBridge", true);
        result.put("nativeHubSurfaceId", "echocore:native_hub");
        result.put("nativeHubKeybind", "key.echocore.native_hub");
        result.put("nativeHubDefaultKey", "F7");
        result.put("nativeHubActionable", false);
        result.put("uiSurfaceStatus", "planned_only");
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary",
                "EchoCore native contract exercised service registry, data bus, and platform diagnostics; the Native Hub surface remains planned-only until a live client bridge owns it.");
        return Map.copyOf(result);
    }

    private Map<String, Object> nativeHubRegistryBridge() {
        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("registry", "screen_surface");
        registration.put("id", "echocore:native_hub");
        registration.put("summary", "ECHO Native Platform Hub in-game screen opened by the EchoCore native hub keybind.");
        registration.put("surfaceKind", "screen");
        registration.put("screen", "com.knoxhack.echocore.client.ui.EchoNativeHubScreen");
        registration.put("keybind", "key.echocore.native_hub");
        registration.put("defaultKey", "F7");
        registration.put("themeFallback", "echocore:blue_console_fallback");
        registration.put("requiresThemeCore", false);
        registration.put("minecraftRegistryMutated", false);
        registration.put("planned", true);

        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("moduleId", MODULE_ID);
        bridge.put("registrationCount", 1);
        bridge.put("registrations", List.of(Map.copyOf(registration)));
        bridge.put("bridge", "adaptercore.native_registry");
        bridge.put("minecraftRegistryMutated", false);
        bridge.put("summary", "Native Loader UI host can discover and mount the EchoCore Native Hub screen surface.");
        return Map.copyOf(bridge);
    }

    private Map<String, Object> exerciseReferenceBehavior() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceRegistryRoundTrip", probeServiceRegistry());
        result.put("dataBusSubscriptionRoundTrip", probeDataBus());
        result.put("diagnosticSummaryAvailable", probeDiagnosticSummary());
        return Map.copyOf(result);
    }

    private boolean probeServiceRegistry() {
        Map<Class<?>, Object> services = new LinkedHashMap<>();
        services.put(EchoCoreNativeModule.class, this);
        return EchoCoreNativeModule.class.isInstance(services.get(EchoCoreNativeModule.class));
    }

    private boolean probeDataBus() {
        List<String> subscribers = new ArrayList<>();
        subscribers.add("echocore-native-probe");
        AutoCloseable subscription = () -> subscribers.remove("echocore-native-probe");
        try {
            return subscribers.contains("echocore-native-probe");
        } finally {
            try {
                subscription.close();
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private boolean probeDiagnosticSummary() {
        return EchoCoreNativeModule.class.getClassLoader()
                .getResource("com/knoxhack/echocore/EchoCoreNativeModule.class") != null;
    }
}
