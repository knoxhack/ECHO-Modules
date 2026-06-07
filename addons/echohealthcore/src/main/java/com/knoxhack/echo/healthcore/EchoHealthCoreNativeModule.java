package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoHealthCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> healthReport = EchoHealthCoreRuntimeReportContract.executeReferenceReport(
                context.getOrDefault("packId", "unknown"),
                EchoHealthCoreRuntimeReportContract.REFERENCE_RUNTIME_NAME
        );
        boolean healthReportPassed = EchoHealthCoreRuntimeReportContract.referenceReportPassed(healthReport);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover runtime health, budget, and support-bundle contracts.")
                .phase("register_health_contracts", "Record health metric and module status contracts.")
                .phase("attach_health_observers", "Record crash and budget observer hooks for the native diagnostics bridge.")
                .phase("execute_health_reporter", "Execute runtime health report writer through AdapterCore.")
                .phase("ready", "Expose HealthCore as the native diagnostics health provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("health_metric", "echohealthcore:runtime_status", "Overall native runtime status.")
                .register("health_metric", "echohealthcore:module_health", "Per-module health state.")
                .register("health_metric", "echohealthcore:budget_violation", "Runtime budget violation contract.")
                .register("health_metric", "echohealthcore:crash_context", "Crash context reporting contract.")
                .register("health_metric", "echohealthcore:support_bundle", "Support bundle metadata contract.")
                .register("service", "echohealthcore:health_reporter", "Runtime health report writer.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("diagnostics.snapshot", "EchoHealthReporter", "Prepare health snapshot capture.")
                .hook("crash.boundary", "EchoCrashContext", "Prepare crash context handoff.")
                .hook("runtime.budget", "EchoPerformanceBudget", "Prepare runtime budget observation.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "healthcore_native_health_report_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("healthReport", healthReport);
        result.put("healthReportExecuted", healthReportPassed);
        result.put("logicalRegistrationCount", 6);
        result.put("eventHookCount", 3);
        result.put("registeredFeatureContracts", List.of(
                "runtime.health",
                EchoHealthCoreRuntimeReportContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresDiagnosticsBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", healthReportPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "HealthCore native contract registered diagnostics hooks and executed the AdapterCore runtime health reporter service.");
        return result;
    }

    private static final String MODULE_ID = "echohealthcore";
}
