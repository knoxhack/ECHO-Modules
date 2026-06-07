package com.knoxhack.echowiki;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoWikiNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> guideSurface = EchoWikiGuideSurfaceContract.executeReferenceLookup(
                context.getOrDefault("packId", "unknown")
        );
        boolean guideSurfacePassed = EchoWikiGuideSurfaceContract.referenceLookupPassed(guideSurface);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Wiki article, collection, guide book, search, and survival codex contracts.")
                .phase("register_wiki_contracts", "Record documentation content and UI contracts before native screen execution.")
                .phase("attach_wiki_events", "Record wiki reload, screen, and optional integration hooks.")
                .phase("execute_guide_surface", "Execute guide book, article, search-result, integration-link, and ScreenCore surface lookup behavior.")
                .phase("ready", "Expose Wiki as the native Ashfall documentation surface.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("wiki_article", "echowiki:articles", "Wiki article contract.")
                .register("wiki_collection", "echowiki:collections", "Wiki collection contract.")
                .register("guide_book", "echowiki:guide_books", "Guide book contract.")
                .register("survival_codex", "echowiki:survival_codex", "Survival codex contract.")
                .register("search_index", "echowiki:search", "Wiki search index contract.")
                .register("ui_surface", "echowiki:wiki", "Wiki UI surface contract.")
                .register("integration", "echowiki:terminal", "Terminal documentation integration.")
                .register("integration", "echowiki:index", "Index documentation integration.")
                .register("integration", "echowiki:screencore", "ScreenCore UI integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("data.reload", "WikiReloaders.register", "Attach wiki content reloaders.")
                .hook("screen.open", "WikiScreenBridge.open", "Prepare wiki screen open flow.")
                .hook("integration.optional", "WikiIntegrationBridge.attach", "Prepare optional Terminal, Index, and ScreenCore integrations.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("holomap_lens_codex_wiki", "echowiki:documentation_service", "documentation",
                        "Keeps wiki article, collection, guide book, and survival codex indexes ready for native UI consumers.",
                        "wiki.articles", "wiki.collections", "wiki.guide_books", "wiki.survival_codex")
                .surfaceService("holomap_lens_codex_wiki", "echowiki:search_service", "documentation_search",
                        "Keeps the wiki search index ready for Codex, Index, and ScreenCore surfaces.",
                        "wiki.search", "codex.search", "screen.components");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "wiki_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("guideSurface", guideSurface);
        result.put("guideSurfaceExecuted", guideSurfacePassed);
        result.put("logicalRegistrationCount", 9);
        result.put("eventHookCount", 3);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "wiki.articles",
                "wiki.collections",
                "wiki.guide_books",
                "wiki.search",
                "wiki.survival_codex",
                EchoWikiGuideSurfaceContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresDocumentationBridge", true);
        result.put("requiresUiBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", guideSurfacePassed);
        result.put("transformsPerformed", false);
        result.put("summary", "Wiki native contract registered documentation and guide surfaces and executed the AdapterCore guide surface lookup service.");
        return result;
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echowiki:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echowiki:ashfall"));
    }

    private static final String MODULE_ID = "echowiki";
}
