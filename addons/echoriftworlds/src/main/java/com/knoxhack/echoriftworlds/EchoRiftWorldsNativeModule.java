package com.knoxhack.echoriftworlds;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRiftWorldsNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> pocketLifecycle = EchoRiftWorldsPocketLifecycleContract.executeReferenceLifecycle(
                context.getOrDefault("packId", "unknown")
        );
        boolean pocketLifecyclePassed = EchoRiftWorldsPocketLifecycleContract.referenceLifecyclePassed(pocketLifecycle);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover rift crack, pocket rift, ruin, hazard, and chapter trigger contracts.")
                .phase("register_rift_contracts", "Record cache echo rift event and world state contracts.")
                .phase("attach_rift_events", "Record rift trigger, hazard, map, lens, and lore hooks.")
                .phase("execute_pocket_rift_lifecycle", "Execute pocket rift chamber, hazard, story-route, return, and cleanup lifecycle behavior.")
                .phase("ready", "Expose RiftWorlds as the native story rift provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("rift_event", "echoriftworlds:rift_event/cache_echo", "Cache Echo rift trigger contract.")
                .register("block", "echoriftworlds:rift_crack", "Rift crack block contract.")
                .register("block", "echoriftworlds:pocket_rift", "Pocket rift block contract.")
                .register("world_hazard", "echoriftworlds:hazard/rift_static", "Rift static hazard contract.")
                .register("save_record", "echoriftworlds:save/rift_state", "Rift trigger and chapter unlock state contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("rift.trigger", "RiftWorldsApi.trigger", "Trigger rift event and story flag mutation.")
                .hook("world.hazard", "RiftWorldsApi.applyHazard", "Bridge dimensional hazard pressure.")
                .hook("map.marker", "RiftWorldsArcanaProvider", "Publish rift map and Arcana provider updates.")
                .hook("lens.scan", "RiftWorldsArcanaProvider", "Publish rift Lens scan records.")
                .hook("lore.index.update", "RiftWorldsArcanaProvider", "Publish rift records to Index, Wiki, and Lore surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .triggerRift(
                        "echoriftworlds:rift_event/cache_echo",
                        "signalos:chapter/cache_handoff",
                        "echoriftworlds:story_flag/cache_echo_seen"
                );
        Map<String, Object> storyRuntimeReport = storyRuntime.report();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "riftworlds_story_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntimeReport);
        result.put("pocketLifecycle", pocketLifecycle);
        result.put("pocketLifecycleExecuted", pocketLifecyclePassed);
        result.put("storyRuntimeServiceCodeExecuted", Boolean.TRUE.equals(storyRuntimeReport.get("serviceCodeExecuted")));
        result.put("storyRuntimeHandlerExecutionCount", storyRuntimeReport.get("handlerExecutionCount"));
        result.put("logicalRegistrationCount", 5);
        result.put("eventHookCount", 5);
        result.put("registeredFeatureContracts", List.of(
                "riftworlds.rift_cracks",
                "riftworlds.pocket_rifts",
                "riftworlds.dimensional_hazards",
                "riftworlds.ruins",
                EchoRiftWorldsPocketLifecycleContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresRiftBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "RiftWorlds native contract registered rift trigger hooks and executed the AdapterCore pocket rift lifecycle service.");
        return result;
    }

    private static final String MODULE_ID = "echoriftworlds";
}
