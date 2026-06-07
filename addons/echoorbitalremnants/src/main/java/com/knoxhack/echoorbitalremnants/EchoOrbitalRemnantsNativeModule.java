package com.knoxhack.echoorbitalremnants;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoOrbitalRemnantsNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Orbital blackbox data-drive and save-state contracts.")
                .phase("register_story_contracts", "Record orbital data-drive, archive unlock, and Prime route flag contracts.")
                .phase("attach_story_events", "Record data-drive read, story flag persistence, and lore hooks.")
                .phase("ready", "Expose Orbital Remnants as a native Prime-route data-drive provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("data_drive", "echoorbitalremnants:data_drive/orbital_blackbox", "Orbital blackbox data drive contract.")
                .register("save_record", "echoorbitalremnants:save/orbital_story_state", "Orbital story state persistence contract.")
                .register("lore_surface", "echoorbitalremnants:lore/orbital_blackbox", "Orbital blackbox lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("data_drive.read", "OrbitalTerminalActions.readBlackboxDrive", "Read the orbital blackbox data drive.")
                .hook("story.flag.save", "OrbitalMissionHooks.persistPrimeRouteFlag", "Persist Prime route story flag state.")
                .hook("lore.index.update", "OrbitalIndexProvider.publishBlackboxDrive", "Publish orbital blackbox records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .readDataDrive(
                        "echoorbitalremnants:data_drive/orbital_blackbox",
                        "echoblackboxprotocol:archive/core_memory",
                        "echoprimecore:story_flag/prime_route_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "orbital_prime_route_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntime.report());
        result.put("logicalRegistrationCount", 3);
        result.put("eventHookCount", 3);
        result.put("registeredFeatureContracts", List.of(
                "orbital.data_drive.blackbox",
                "orbital.story_state",
                "orbital.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Orbital Remnants native contract registered the orbital blackbox data drive and Prime route flag persistence through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echoorbitalremnants";
}
