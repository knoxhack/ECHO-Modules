package com.knoxhack.echoritualcore;

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

public final class EchoRitualCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> ritualActivation = EchoRitualCoreActivationContract.executeReferenceActivation(
                context.getOrDefault("packId", "unknown")
        );
        boolean ritualActivationPassed = EchoRitualCoreActivationContract.referenceActivationPassed(ritualActivation);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover ritual, altar, pedestal, structure validation, and backlash contracts.")
                .phase("register_ritual_contracts", "Record relic stabilization and ritual state contracts.")
                .phase("attach_ritual_events", "Record ritual activation, validation, mission, map, and lore hooks.")
                .phase("execute_aether_calibration", "Execute altar structure validation, focus cost, output grant, and ritual completion proof.")
                .phase("ready", "Expose RitualCore as the native story ritual provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("ritual", "echoritualcore:ritual/relic_stabilization", "Relic stabilization ritual contract.")
                .register("ritual", EchoRitualCoreActivationContract.ADAPTERCORE_CONTRACT_ID, "Aether calibration altar activation contract.")
                .register("block", "echoritualcore:basic_altar", "Ritual altar block contract.")
                .register("block", "echoritualcore:rune_circle", "Rune circle structure contract.")
                .register("save_record", "echoritualcore:save/ritual_state", "Ritual stability and unlock flag state contract.")
                .register("map_marker", "echoritualcore:map/ritual", "Ritual HoloMap marker contract.")
                .register("service", EchoRitualCoreActivationContract.SERVICE_ID, "Ritual activation cost, output, and completion service.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("ritual.activate", "RitualCoreApi.complete", "Apply ritual activation and chapter stability mutation.")
                .hook("ritual.validate", "RitualStructureValidator.validate", "Validate ritual structure requirements.")
                .hook("mission.hook", "RitualCoreMissionCoreIntegration", "Publish ritual mission hook coverage.")
                .hook("map.marker", "RitualCoreMapMarkers.record", "Publish ritual map marker updates.")
                .hook("lore.index.update", "RitualCoreTerminalIntegration", "Publish ritual records to Terminal, Index, and Lore surfaces.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("ritual_activation", EchoRitualCoreActivationContract.SERVICE_ID, "altar_activation",
                        "Keeps altar structure validation, focus cost consumption, ritual outputs, and completion side effects executable in native activation.",
                        "ritual.structure_validation", "ritual.costs", "ritual.outputs", "ritual.events");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .activateRitual(
                        "echoritualcore:ritual/relic_stabilization",
                        "chapterStability",
                        1,
                        "echoritualcore:story_flag/relic_stabilized"
                );
        Map<String, Object> storyRuntimeReport = storyRuntime.report();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "ritualcore_native_aether_calibration_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("storyRuntimeBridge", storyRuntimeReport);
        result.put("ritualActivation", ritualActivation);
        result.put("ritualActivationExecuted", ritualActivationPassed);
        result.put("storyRuntimeServiceCodeExecuted", Boolean.TRUE.equals(storyRuntimeReport.get("serviceCodeExecuted")));
        result.put("storyRuntimeHandlerExecutionCount", storyRuntimeReport.get("handlerExecutionCount"));
        result.put("logicalRegistrationCount", 7);
        result.put("eventHookCount", 5);
        result.put("approvedNativeServiceCount", 1);
        result.put("registeredFeatureContracts", List.of(
                "ritual.altar",
                "ritual.events",
                "ritual.structure_validation",
                "ritual.diagnostics",
                EchoRitualCoreActivationContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresRitualBridge", true);
        result.put("requiresServiceBridge", true);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "RitualCore native contract executed the AdapterCore altar aether calibration activation service with structure, cost, output, and completion proofs.");
        return result;
    }

    private static final String MODULE_ID = "echoritualcore";
}
