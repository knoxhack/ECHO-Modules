package com.knoxhack.echorecovery;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRecoveryNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> recoveryPlan = EchoRecoveryFieldPlanContract.executeReferencePlan(
                context.getOrDefault("packId", "unknown")
        );
        boolean recoveryPlanPassed = EchoRecoveryFieldPlanContract.referencePlanPassed(recoveryPlan);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover recovery, grave, compass, and safe-mode contracts.")
                .phase("register_recovery_content", "Record recovery blocks, items, data components, menus, and sounds.")
                .phase("attach_recovery_events", "Record death, command, reload, and server lifecycle hooks.")
                .phase("execute_field_recovery_plan", "Execute grave snapshot, item-rule, compass, and safe-mode recovery plan behavior.")
                .phase("ready", "Expose Recovery as the native field recovery provider for Ashfall.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("block", "echorecovery:grave", "Player grave block contract.")
                .register("block", "echorecovery:field_cache", "Field cache recovery block contract.")
                .register("item", "echorecovery:recovery_compass", "Recovery compass item contract.")
                .register("item", "echorecovery:grave_key", "Grave key item contract.")
                .register("data_component", "echorecovery:recovery_metadata", "Recovery metadata component.")
                .register("menu", "echorecovery:grave", "Grave UI menu contract.")
                .register("sound", "echorecovery:recovery_ping", "Recovery feedback sound contract.")
                .register("service", "echorecovery:recovery_service", "ECHO recovery service provider.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoRecovery.commonSetup", "Attach recovery service and optional integrations.")
                .hook("player.death", "DeathHandler", "Prepare grave and recovery capture.")
                .hook("commands.register", "GravesCommand.register", "Expose recovery commands when native command bridge exists.")
                .hook("data.reload", "RecoveryReloaders.addServerReloadListeners", "Attach recovery JSON reloaders.")
                .hook("server.started", "RecoveryWorldData.getOrCreate", "Prepare recovery world data.")
                .hook("server.stopping", "EchoRecovery.onServerStopping", "Prepare recovery shutdown boundary.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("player_recovery", "echorecovery:recovery_service", "player_recovery",
                        "Keeps the grave, compass, safe-mode, and recovery-plan runtime state ready for native player death hooks.",
                        "recovery.graves", "recovery.compass", "recovery.safe_modes", "recovery.plans")
                .surfaceService("player_recovery", "echorecovery:field_cache_service", "field_cache",
                        "Keeps field cache and recovery stop contracts visible to HoloMap and Ashfall route systems.",
                        "recovery.field_caches", "holomap.layers");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "recovery_native_field_plan_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("recoveryPlan", recoveryPlan);
        result.put("recoveryPlanExecuted", recoveryPlanPassed);
        result.put("logicalRegistrationCount", 8);
        result.put("eventHookCount", 6);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "recovery.commands",
                "recovery.compass",
                "recovery.field_caches",
                "recovery.graves",
                "recovery.plans",
                "recovery.rules",
                "recovery.safe_modes",
                EchoRecoveryFieldPlanContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresRecoveryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", recoveryPlanPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "Recovery native contract registered grave, cache, compass, and death hooks and executed the AdapterCore field recovery plan service.");
        return result;
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echorecovery:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echorecovery:field_cache_service"));
    }

    private static final String MODULE_ID = "echorecovery";
}
