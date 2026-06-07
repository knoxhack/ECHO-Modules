package com.knoxhack.echogrimoire;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoGrimoireNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Grimoire archive and Arcane codex contracts.")
                .phase("register_story_contracts", "Record Arcane codex archive and Terminal lore contracts.")
                .phase("attach_story_events", "Record archive unlock, terminal page, and lore update hooks.")
                .phase("ready", "Expose Grimoire as a native Arcane codex archive provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("archive_entry", "echogrimoire:archive/arcane_codex", "Grimoire Arcane codex archive contract.")
                .register("lore_surface", "echogrimoire:lore/arcane_codex", "Arcane codex lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("archive.unlock", "GrimoireTerminalIntegration.unlockArcaneCodex", "Unlock Arcane codex pages.")
                .hook("terminal.page.open", "GrimoireTerminalIntegration.openCodex", "Render the codex through the terminal host.")
                .hook("lore.index.update", "GrimoireIndexBridge.publishCodex", "Publish Grimoire codex records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .readDataDrive(
                        "signalosexample:data_drive/arcane_codex_demo",
                        "echogrimoire:archive/arcane_codex",
                        "echoarcanacore:story_flag/arcane_codex_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "grimoire_arcane_codex_native_contract_active");
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
                "grimoire.archive.arcane_codex",
                "grimoire.terminal.codex",
                "grimoire.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresTerminalBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Grimoire native contract registered Arcane codex archive and terminal lore hooks through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echogrimoire";
}
