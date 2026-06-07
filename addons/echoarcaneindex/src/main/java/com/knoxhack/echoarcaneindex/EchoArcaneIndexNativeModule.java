package com.knoxhack.echoarcaneindex;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoArcaneIndexNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Arcane Index chapter and knowledge page contracts.")
                .phase("register_story_contracts", "Record Arcane codex chapter and index page contracts.")
                .phase("attach_story_events", "Record chapter unlock, index page publish, and lore hooks.")
                .phase("ready", "Expose Arcane Index as a native Arcane codex chapter provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("chapter", "echoarcaneindex:chapter/arcane_codex", "Arcane Index codex chapter contract.")
                .register("lore_surface", "echoarcaneindex:lore/arcane_codex", "Arcane codex index lore update contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("chapter.unlock", "ArcaneIndexProvider.unlockCodexChapter", "Unlock Arcane Index codex chapter.")
                .hook("index.page.publish", "ArcaneIndexProvider.publishCodexPages", "Publish Arcana knowledge pages.")
                .hook("lore.index.update", "ArcaneIndexProvider.publishCodexLore", "Publish Arcane Index records to story surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .unlockChapter(
                        "echoarcaneindex:chapter/arcane_codex",
                        "echoarcanacore:story_flag/arcane_codex_unlocked"
                );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "arcaneindex_arcane_codex_native_contract_active");
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
                "arcane_index.chapter.arcane_codex",
                "arcane_index.knowledge_pages",
                "arcane_index.lore_updates"
        ));
        result.put("requiresStoryBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "Arcane Index native contract registered Arcane codex chapter and knowledge-page hooks through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echoarcaneindex";
}
