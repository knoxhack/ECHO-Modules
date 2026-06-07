package com.knoxhack.echostationfall;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoStationfallNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Stationfall chapter unlock and route contracts.")
                .phase("register_story_contracts", "Record Stationfall route chapter and lore contracts.")
                .phase("attach_story_events", "Record chapter unlock, route activation, and lore hooks.")
                .phase("ready", "Expose Stationfall as a native Prime-route chapter provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("chapter", "echostationfall:chapter/stationfall_route", "Stationfall route chapter unlock contract.")
                .register("lore_surface", "echostationfall:lore/stationfall_route", "Stationfall route lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("chapter.unlock", "StationfallRouteTracker.unlockPrimeRoute", "Unlock the Stationfall chapter when Prime route flag is present.")
                .hook("story.route.activate", "StationfallPrimeIntegration.activateRoute", "Activate Stationfall route progression.")
                .hook("lore.index.update", "StationfallTerminalIntegration.publishRoute", "Publish Stationfall route records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .unlockChapter(
                        "echostationfall:chapter/stationfall_route",
                        "echoprimecore:story_flag/prime_route_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "stationfall_prime_route_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntime.report());
        result.put("logicalRegistrationCount", 2);
        result.put("eventHookCount", 3);
        result.put("registeredFeatureContracts", List.of(
                "stationfall.chapter.route",
                "stationfall.prime_route_progression",
                "stationfall.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Stationfall native contract registered the Prime route chapter unlock through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echostationfall";
}
