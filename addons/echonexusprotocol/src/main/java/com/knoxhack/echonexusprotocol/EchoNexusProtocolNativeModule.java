package com.knoxhack.echonexusprotocol;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNexusProtocolNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Nexus signal and Prime route handoff contracts.")
                .phase("register_story_contracts", "Record signal message, mission trigger, and lore contracts.")
                .phase("attach_story_events", "Record Nexus handoff signal and Prime route mission hooks.")
                .phase("ready", "Expose Nexus Protocol as a native Prime-route signal provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("signal_message", "echonexusprotocol:signal/nexus_handoff", "Nexus handoff signal contract.")
                .register("lore_surface", "echonexusprotocol:lore/nexus_handoff", "Nexus handoff lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("signal.receive", "NexusTerminalIntegration.receiveHandoff", "Receive the Prime route handoff signal.")
                .hook("story.mission.start", "NexusMissionHooks.startPrimeRoute", "Start the Prime route mission from Nexus signal state.")
                .hook("lore.index.update", "NexusIndexProvider.publishHandoff", "Publish Nexus handoff records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .startMission("echonexusprotocol:signal/nexus_handoff", "echoprimecore:mission/prime_route");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "nexus_prime_route_native_contract_active");
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
                "nexus.signal.handoff",
                "nexus.prime_route_mission",
                "nexus.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Nexus Protocol native contract registered the handoff signal and Prime route mission hooks through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echonexusprotocol";
}
