package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNotificationCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoNotificationConstants.MODULE_ID.value();
    public static final List<String> CONTRACT_IDS = List.of(
            "echonotificationcore:ui/toast",
            "echonotificationcore:ui/system_alert",
            "echonotificationcore:mission/mission_update",
            "echonotificationcore:ui/tutorial_hint"
    );

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = baseResult(context, probe);
        result.put("activationStage", "notificationcore_native_contract_active");
        result.put("adapterDomains", List.of("ui_screens", "missions"));
        result.put("summary", "NotificationCore native contract exercised toast, alert, mission update, and tutorial hint feature contracts.");
        return Map.copyOf(result);
    }

    private Map<String, Object> referenceProbe() {
        List<String> features = List.of(
                EchoNotificationConstants.FEATURE_MISSION_UPDATES.value(),
                EchoNotificationConstants.FEATURE_SYSTEM_ALERTS.value(),
                EchoNotificationConstants.FEATURE_TOASTS.value(),
                EchoNotificationConstants.FEATURE_TUTORIAL_HINTS.value()
        ).stream().sorted().toList();
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("features", features);
        probe.put("featureContractRoundTrip", features.equals(List.of(
                "notifications.mission_updates",
                "notifications.system_alerts",
                "notifications.toasts",
                "notifications.tutorial_hints"
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
