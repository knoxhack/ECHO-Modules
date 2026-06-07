package com.knoxhack.signalosexample;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SignalOsExampleNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover SignalOS example data-drive and archive demo contracts.")
                .phase("register_story_contracts", "Record Arcane codex demo drive and story flag hooks.")
                .phase("attach_story_events", "Record demo drive read and archive unlock events.")
                .phase("ready", "Expose SignalOS Example as a native demo data-drive provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("data_drive", "signalosexample:data_drive/arcane_codex_demo", "Arcane codex demo data drive contract.")
                .register("lore_surface", "signalosexample:lore/arcane_codex_demo", "Demo drive lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("data_drive.read", "SignalOsExample.readArcaneCodexDemo", "Read the example Arcane codex drive.")
                .hook("archive.unlock", "SignalOsExample.unlockArcaneCodex", "Unlock Grimoire archive from example data drive.")
                .hook("lore.index.update", "SignalOsExample.publishDemoLore", "Publish example drive records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .readDataDrive(
                        "signalosexample:data_drive/arcane_codex_demo",
                        "echogrimoire:archive/arcane_codex",
                        "echoarcanacore:story_flag/arcane_codex_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "signalos_example_arcane_codex_native_contract_active");
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
                "signalos_example.data_drive.arcane_codex",
                "signalos_example.archive_unlock",
                "signalos_example.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "SignalOS Example native contract registered executable demo data-drive hooks through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "signalosexample";
}
