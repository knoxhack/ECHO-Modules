package com.knoxhack.echopresencelink;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoPresenceLinkNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover story presence link contracts.")
                .phase("register_presence_contracts", "Record SignalOS cache and Prime route presence state contracts.")
                .phase("attach_presence_events", "Record presence link, privacy, and story state hooks.")
                .phase("ready", "Expose Presence Link as the native story presence provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("presence_link", "echopresencelink:presence/signalos_cache", "SignalOS cache presence link contract.")
                .register("presence_link", "echopresencelink:presence/prime_route", "Prime route presence link contract.")
                .register("save_record", "echopresencelink:save/story_presence", "Story presence state persistence contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("presence.link", "PresenceLinkSignalOsIntegration.linkSignalOsCache", "Link SignalOS cache reading state to presence.")
                .hook("presence.link", "PresenceController.linkPrimeRoute", "Link Prime route tracking state to presence.")
                .hook("presence.privacy", "PresenceLinkConfig.applyPrivacy", "Apply privacy filters before publishing story presence.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .linkPresence("echopresencelink:presence/signalos_cache", "reading_signalos_archive")
                .linkPresence("echopresencelink:presence/prime_route", "tracking_prime_route");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "presence_story_native_contract_active");
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
                "presence.signalos_cache",
                "presence.prime_route",
                "presence.story_state"
        ));
        result.put("requiresPresenceBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Presence Link native contract registered SignalOS and Prime route story presence hooks through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echopresencelink";
}
