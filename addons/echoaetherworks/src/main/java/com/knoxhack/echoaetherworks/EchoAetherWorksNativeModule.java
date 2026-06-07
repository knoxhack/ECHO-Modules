package com.knoxhack.echoaetherworks;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoAetherWorksNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover AetherWorks synchronization and machine state contracts.")
                .phase("register_story_contracts", "Record Aether sync presence and machine lore contracts.")
                .phase("attach_story_events", "Record aether sync, storage update, and lore hooks.")
                .phase("ready", "Expose AetherWorks as the native Arcane codex sync provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("presence_link", "echoaetherworks:presence/aether_sync", "AetherWorks Arcane codex sync presence contract.")
                .register("save_record", "echoaetherworks:save/aether_sync_state", "Aether sync state persistence contract.")
                .register("lore_surface", "echoaetherworks:lore/aether_sync", "Aether sync lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("presence.link", "AetherWorksApi.linkCodexSync", "Link Arcane codex state to AetherWorks machine sync.")
                .hook("aether.storage.update", "AetherStorageBlockEntity.applyCodexSync", "Apply codex sync to aether storage state.")
                .hook("lore.index.update", "AetherWorksApi.publishSyncLore", "Publish AetherWorks sync records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .linkPresence("echoaetherworks:presence/aether_sync", "syncing_arcane_codex");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "aetherworks_arcane_codex_native_contract_active");
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
                "aetherworks.presence.aether_sync",
                "aetherworks.machine_sync",
                "aetherworks.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "AetherWorks native contract registered Arcane codex aether sync hooks through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echoaetherworks";
}
