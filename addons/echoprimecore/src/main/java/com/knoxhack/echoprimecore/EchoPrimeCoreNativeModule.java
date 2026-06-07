package com.knoxhack.echoprimecore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoPrimeCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Prime route story flag, mission, and save contracts.")
                .phase("register_story_contracts", "Record Prime route mission, flag, and state persistence contracts.")
                .phase("attach_story_events", "Record mission start, flag save/load, chapter progression, and lore hooks.")
                .phase("ready", "Expose Prime Core as the native Prime-route coordinator.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("story_flag", "echoprimecore:story_flag/prime_route_unlocked", "Prime route unlocked story flag contract.")
                .register("mission", "echoprimecore:mission/prime_route", "Prime route story mission contract.")
                .register("save_record", "echoprimecore:save/prime_route_state", "Prime route story state persistence contract.")
                .register("lore_surface", "echoprimecore:lore/prime_route", "Prime route lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("story.mission.start", "PrimeIntegrationRegistry.startPrimeRoute", "Start the Prime route mission from Nexus handoff.")
                .hook("story.flag.save", "PrimeIntegrationRegistry.savePrimeRouteFlag", "Persist Prime route story flag state.")
                .hook("chapter.unlock", "PrimeIntegrationRegistry.unlockStationfallRoute", "Forward the unlocked route to Stationfall.")
                .hook("lore.index.update", "PrimeIntegrationRegistry.publishPrimeRoute", "Publish Prime route records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .startMission("echonexusprotocol:signal/nexus_handoff", "echoprimecore:mission/prime_route")
                .unlockChapter(
                        "echostationfall:chapter/stationfall_route",
                        "echoprimecore:story_flag/prime_route_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "prime_route_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntime.report());
        result.put("logicalRegistrationCount", 4);
        result.put("eventHookCount", 4);
        result.put("registeredFeatureContracts", List.of(
                "prime.story_flag.route_unlocked",
                "prime.mission.route",
                "prime.story_state",
                "prime.chapter_progression"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Prime Core native contract registered the Prime route mission, flag persistence, and Stationfall chapter progression through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echoprimecore";
}
