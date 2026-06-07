package com.knoxhack.echocursecore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoCurseCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> curseStateResolution = EchoCurseCoreStateContract.executeReferenceStateResolution(
                context.getOrDefault("packId", "unknown")
        );
        boolean curseStateResolutionPassed = EchoCurseCoreStateContract.referenceStateResolutionPassed(
                curseStateResolution
        );
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover curse definition, stage, cleansing, diagnostics, and persistence contracts.")
                .phase("register_curse_contracts", "Record Echo Rot curse and curse state contracts.")
                .phase("attach_curse_events", "Record curse apply, cleanse, sync, mission, and lore hooks.")
                .phase("execute_curse_state_resolution", "Execute persistent player curse, cleansing, contract debt, and tick-effect behavior.")
                .phase("ready", "Expose CurseCore as the native story curse provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("curse", "echocursecore:curse/echo_rot", "Echo Rot curse contract.")
                .register("curse", "echocursecore:curse/blood_debt", "Blood Debt curse contract.")
                .register("ui_surface", "echocursecore:curse_contract", "Curse contract UI surface.")
                .register("network", "echocursecore:curse_hud_sync", "Curse HUD sync contract.")
                .register("save_record", "echocursecore:save/curse_state", "Persistent curse state contract.")
                .register("diagnostic", "echocursecore:curse_diagnostic", "Curse diagnostic contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("curse.apply", "CurseCoreApi.apply", "Apply curse gameplay mutation.")
                .hook("curse.cleanse", "CurseCoreApi.cleanse", "Cleanse curse state through ritual bridge.")
                .hook("network.sync", "CurseCoreNetwork.syncHud", "Bridge curse HUD sync payload.")
                .hook("mission.hook", "CurseCoreMissionCoreIntegration", "Publish curse mission hook coverage.")
                .hook("lore.index.update", "CurseCoreTerminalIntegration", "Publish curse records to Terminal, Index, and Lore surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .applyCurse("echocursecore:curse/echo_rot", "signalClarity", -1);
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("player", "echocursecore:curse_state_service", "curse_state_resolution",
                        "Executes persistent player curse state, contract debt, cleansing plan, and tick effect resolution.",
                        "curse.state", "curse.contracts", "curse.cleansing", "curse.effects");
        Map<String, Object> storyRuntimeReport = storyRuntime.report();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "cursecore_native_state_resolution_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntimeReport);
        result.put("serviceBridge", services.describe());
        result.put("curseStateResolution", curseStateResolution);
        result.put("curseStateResolved", curseStateResolutionPassed);
        result.put("storyRuntimeServiceCodeExecuted", Boolean.TRUE.equals(storyRuntimeReport.get("serviceCodeExecuted")));
        result.put("storyRuntimeHandlerExecutionCount", storyRuntimeReport.get("handlerExecutionCount"));
        result.put("logicalRegistrationCount", 6);
        result.put("eventHookCount", 5);
        result.put("approvedNativeServiceCount", 1);
        result.put("registeredFeatureContracts", List.of(
                "curse.contracts",
                "curse.persistence",
                "curse.stages",
                "curse.cleansing",
                "curse.diagnostics",
                "curse.effects",
                EchoCurseCoreStateContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresCurseBridge", true);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "CurseCore native contract executed persistent curse state, cleansing plan, contract debt, and tick-effect resolution through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echocursecore";
}
