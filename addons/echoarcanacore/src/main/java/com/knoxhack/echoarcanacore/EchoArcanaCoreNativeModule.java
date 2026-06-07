package com.knoxhack.echoarcanacore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoArcanaCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Arcana signal, story flag, mission, and save contracts.")
                .phase("register_story_contracts", "Record Aether Wake signal, Arcane codex flag, mission, and save contracts.")
                .phase("attach_story_events", "Record mission start, story flag persistence, provider sync, and lore hooks.")
                .phase("ready", "Expose Arcana Core as the native Arcane codex coordinator.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("signal_message", "echoarcanacore:signal/aether_wake", "Arcana Core Aether Wake signal contract.")
                .register("story_flag", "echoarcanacore:story_flag/arcane_codex_unlocked", "Arcane codex unlocked story flag contract.")
                .register("mission", "echoarcanacore:mission/arcane_codex_sync", "Arcane codex synchronization mission contract.")
                .register("save_record", "echoarcanacore:save/arcane_story_state", "Arcane story state persistence contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("signal.receive", "ArcanaCoreServices.receiveAetherWake", "Receive the Aether Wake story signal.")
                .hook("story.mission.start", "ArcanaCoreServices.startCodexSync", "Start Arcane codex synchronization.")
                .hook("story.flag.save", "ArcanaCoreServices.saveCodexFlag", "Persist Arcane codex flag state.")
                .hook("lore.index.update", "ArcanaCoreServices.publishCodexLore", "Publish Arcana records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .startMission("echoarcanacore:signal/aether_wake", "echoarcanacore:mission/arcane_codex_sync")
                .readDataDrive(
                        "signalosexample:data_drive/arcane_codex_demo",
                        "echogrimoire:archive/arcane_codex",
                        "echoarcanacore:story_flag/arcane_codex_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "arcanacore_arcane_codex_native_contract_active");
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
                "arcana.signal.aether_wake",
                "arcana.story_flag.arcane_codex",
                "arcana.mission.codex_sync",
                "arcana.story_state"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Arcana Core native contract registered Aether Wake mission and story flag persistence through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echoarcanacore";
}
