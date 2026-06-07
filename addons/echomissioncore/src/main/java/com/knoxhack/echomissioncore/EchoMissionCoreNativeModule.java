package com.knoxhack.echomissioncore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoMissionCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> missionProgression = EchoMissionCoreObjectiveProgressContract.executeReferenceProgression(
                context.getOrDefault("packId", "unknown")
        );
        boolean missionProgressionPassed = EchoMissionCoreObjectiveProgressContract.referenceProgressionPassed(missionProgression);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover MissionCore objective, route, and runtime bus contracts.")
                .phase("register_mission_contracts", "Record mission definitions and player state contracts before native service execution.")
                .phase("attach_mission_events", "Record command, reload, and integration hooks for the native event bridge.")
                .phase("execute_objective_progression", "Execute mission start, objective progress, completion, reward, and Terminal snapshot behavior.")
                .phase("ready", "Expose MissionCore as the native mission provider for Ashfall.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("attachment", "echomissioncore:mission_player_data", "Mission player progress attachment.")
                .register("service", "echomissioncore:mission_service", "Mission objective and route service provider.")
                .register("content", "echomissioncore:built_in_missions", "Built-in mission content registry.")
                .register("content", "echomissioncore:built_in_routes", "Built-in route content registry.")
                .register("integration", "echomissioncore:worldcore", "WorldCore mission source integration.")
                .register("integration", "echomissioncore:terminal", "Terminal mission browser integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoMissionCore.commonSetup", "Attach mission service and integration providers.")
                .hook("commands.register", "MissionCoreCommands.register", "Expose mission command surface when native command bridge exists.")
                .hook("data.reload", "MissionCoreReloaders.addServerReloadListeners", "Attach mission JSON reloaders.")
                .hook("mission.runtime_bus", "MissionRuntimeBus", "Prepare mission runtime event fanout.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .executedSurfaceService(
                        "missions",
                        "echomissioncore:mission_service",
                        "mission_objective_progression",
                        "Executes deterministic mission start, objective progress, completion, reward, and Terminal snapshot state.",
                        missionProgression,
                        "missions.objectives",
                        "missions.progress",
                        "missions.rewards",
                        "missions.terminal_snapshot")
                .surfaceService(
                        "missions",
                        "echomissioncore:route_service",
                        "mission_route_projection",
                        "Keeps MissionCore route placement and prerequisite contracts visible to Terminal and HoloMap integrations.",
                        "missions.routes",
                        "terminal.mission_feed");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "missioncore_native_objective_progression_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("missionProgression", missionProgression);
        result.put("missionProgressionExecuted", missionProgressionPassed);
        result.put("logicalRegistrationCount", 6);
        result.put("eventHookCount", 4);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "missions.objectives",
                "missions.routes",
                "missions.progress",
                "missions.rewards",
                "missions.terminal_snapshot",
                EchoMissionCoreObjectiveProgressContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresMissionBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", missionProgressionPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "MissionCore native contract registered mission, route, and runtime bus hooks and executed the AdapterCore objective progression service.");
        return result;
    }

    private static final String MODULE_ID = "echomissioncore";
}
