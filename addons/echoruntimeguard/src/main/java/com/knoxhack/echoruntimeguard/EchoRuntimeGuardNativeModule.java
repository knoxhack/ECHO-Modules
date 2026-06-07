package com.knoxhack.echoruntimeguard;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRuntimeGuardNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    public static final String MODULE_ID = "echoruntimeguard";
    public static final String RUNTIME_HEALTH_CONTRACT_ID = "echoruntimeguard:diagnostic/runtime_health";
    public static final String RUNTIME_METRICS_CONTRACT_ID = "echoruntimeguard:data/runtime_metrics";
    public static final String NETWORK_BUDGET_CONTRACT_ID = "echoruntimeguard:network/runtime_budget";
    public static final String ECHO_PERF_COMMAND_CONTRACT_ID = "echoruntimeguard:command/echo_perf";
    public static final List<String> CONTRACT_IDS = List.of(
            RUNTIME_HEALTH_CONTRACT_ID,
            RUNTIME_METRICS_CONTRACT_ID,
            NETWORK_BUDGET_CONTRACT_ID,
            ECHO_PERF_COMMAND_CONTRACT_ID
    );

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover RuntimeGuard budget, metrics, diagnostics, and command contracts.")
                .phase("register_runtime_guard_contracts", "Expose runtime health, metrics, network budget, and command contracts.")
                .phase("attach_runtime_hooks", "Attach tick, network, diagnostics, and command hook declarations.")
                .phase("ready", "Expose RuntimeGuard as an AdapterCore runtime pressure guard provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("diagnostic", RUNTIME_HEALTH_CONTRACT_ID, "Runtime health diagnostic contract.")
                .register("data_component", RUNTIME_METRICS_CONTRACT_ID, "Runtime metrics snapshot contract.")
                .register("network_hook", NETWORK_BUDGET_CONTRACT_ID, "Network budget gate contract.")
                .register("command", ECHO_PERF_COMMAND_CONTRACT_ID, "RuntimeGuard /echo_perf command contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("server.tick", "RuntimeBudgetCoreService.tick", "Update RuntimeGuard pressure budgets.")
                .hook("network.payload_send", "NetworkBudgetService.trySend", "Apply RuntimeGuard network budget decisions.")
                .hook("diagnostics.collect", "RuntimeGuardDiagnostics.diagnostics", "Publish RuntimeGuard diagnostic blockers.")
                .hook("commands.register", "RuntimeGuardCommands.register", "Expose RuntimeGuard command contract.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "runtimeguard_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("eventHookCount", 4);
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("requiresRuntimeBudgetBridge", true);
        result.put("requiresDiagnosticsBridge", true);
        result.put("requiresNetworkBridge", true);
        result.put("requiresCommandBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "RuntimeGuard native contract registered executable budget, diagnostics, networking, and command hooks through AdapterCore.");
        return result;
    }
}
