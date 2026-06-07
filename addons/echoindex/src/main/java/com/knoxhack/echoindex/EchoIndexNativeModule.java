package com.knoxhack.echoindex;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoIndexNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    private static final String EVENT_HOST_SERVICE_ID = "echo_native.event_host";
    private static final String LIFECYCLE_HOST_SERVICE_ID = "echo_native.lifecycle_host";

    private Map<String, Object> nativeLoaderActivation;

    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct");
        context.attribute("nativeEntrypointDelegateClass", getClass().getName());
        context.attribute("echoIndexNativeLifecycle", "direct_native_loader");
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        Map<String, Object> activation = activation(context);
        context.registerService(
                "module.echoindex.native_entrypoint",
                this,
                "lifecycle",
                "diagnostics",
                "adaptercore");
        context.registerService(
                "adaptercore.echoindex.contract",
                new NativeService("adaptercore", activation),
                "adaptercore",
                "recipes",
                "ui_screens",
                "ui_overlays",
                "inventory");
        context.registerService(
                "service.echoindex.index_service",
                new NativeService("query_service", object(activation.get("queryService"))),
                "adaptercore",
                "recipes",
                "search");
        context.registerService(
                "service.echoindex.inventory_overlay",
                new NativeService("inventory_overlay", object(activation.get("inventoryOverlay"))),
                "adaptercore",
                "inventory",
                "ui_overlays",
                "hud");
        registerFeatureContracts(context, activation);
        registerEventHooks(context, activation);
        registerLifecyclePhases(context, activation);
        context.attribute("echoIndexNativeActivation", activation);
        context.attribute("echoIndexQueryServiceExecuted", activation.get("queryServiceExecuted"));
        context.attribute("echoIndexInventoryOverlayReady", activation.get("inventoryOverlayReady"));
        context.recordMutation(
                "lifecycle",
                "direct_native_entrypoint",
                "echoindex:index",
                EchoNativeLoadStatus.REGISTERED);
        context.recordMutation(
                "ui_overlays",
                "inventory_overlay_registered",
                "echoindex:inventory_overlay",
                EchoNativeLoadStatus.REGISTERED);
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerContent(context, activation(context));
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        registerNativeClientRoutesFromModule(context);
        context.attribute("echoIndexNativeReady", true);
        context.recordMutation(
                "lifecycle",
                "module_ready",
                "echoindex:index",
                EchoNativeLoadStatus.REGISTERED);
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Index recipe, usage graph, search, and UI backend contracts.")
                .phase("register_index_contracts", "Record recipe backend and index surface contracts before native UI execution.")
                .phase("register_inventory_overlay", "Expose the in-inventory Index side drawer render and input contract.")
                .phase("attach_index_events", "Record reload, command, screen, and optional integration hooks.")
                .phase("ready", "Expose Index as the native Ashfall recipe and reference surface.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("index", "echoindex:index", "Index recipe browser UI contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                        "nativeScreenBridgeClass", "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                        "actions", Map.ofEntries(
                                Map.entry("index.catalog", Map.of(
                                        "kind", "screen_bridge",
                                        "bridgeMethod", "open")),
                                Map.entry("index.recipe", Map.of(
                                        "kind", "item_recipe",
                                        "recipeMode", "recipes")),
                                Map.entry("index.usage", Map.of(
                                        "kind", "item_recipe",
                                        "recipeMode", "usages")),
                                Map.entry("index.bookmark", Map.of(
                                        "kind", "screen_core_mode",
                                        "mode", "favorites")),
                                Map.entry("index.hotkey_screen_render", Map.of("kind", "hotkey_screen_render")),
                                Map.entry("index.hotkey_key_pressed", Map.of("kind", "hotkey_key_pressed")),
                                Map.entry("index.client.login", Map.of(
                                        "kind", "client_lifecycle",
                                        "reason", "client login")),
                                Map.entry("index.client.logout", Map.of(
                                        "kind", "client_lifecycle",
                                        "reason", "client logout")),
                                Map.entry("index.client.resources_reloaded", Map.of(
                                        "kind", "client_lifecycle",
                                        "reason", "client resources reloaded",
                                        "invalidateScreenCoreIndex", true)),
                                Map.entry("index.recipe_screen.mouse", Map.of("kind", "recipe_screen_mouse_input")),
                                Map.entry("index.recipe_screen.scroll", Map.of("kind", "recipe_screen_scroll_input")),
                                Map.entry("index.recipe_screen.key", Map.of("kind", "recipe_screen_key_input")),
                                Map.entry("index.recipe_screen.char", Map.of("kind", "recipe_screen_char_input")),
                                Map.entry("index.catalog_screen.mouse", Map.of("kind", "catalog_screen_mouse_input")),
                                Map.entry("index.catalog_screen.scroll", Map.of("kind", "catalog_screen_scroll_input")),
                                Map.entry("index.catalog_screen.key", Map.of("kind", "catalog_screen_key_input")),
                                Map.entry("index.catalog_screen.char", Map.of("kind", "catalog_screen_char_input")),
                                Map.entry("index.screencore.action", Map.of(
                                        "kind", "index_screencore_action",
                                        "screenBridge", "echoscreencore",
                                        "actionCatalog", "IndexActions"))
                        )))
                .register("client_overlay", "echoindex:inventory_overlay", "Index inventory side-drawer overlay contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoindex.client.IndexOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                        "actions", Map.of(
                                "index.inventory_overlay_render", Map.of("kind", "overlay_render"),
                                "index.inventory_overlay_input", Map.of("kind", "overlay_input"),
                                "index.open_recipes_for_item", Map.of("kind", "item_recipe", "recipeMode", "recipes"),
                                "index.open_usages_for_item", Map.of("kind", "item_recipe", "recipeMode", "usages"),
                                "index.track_item", Map.of("kind", "item_recipe", "recipeMode", "track"),
                                "index.toggle_favorite", Map.of("kind", "screen_core_mode", "mode", "favorites")
                        )))
                .register("recipe_backend", "echoindex:first_party_backend", "First-party recipe backend contract.")
                .register("recipe_category", "echoindex:ashfall_categories", "Ashfall recipe category contract.")
                .register("search_index", "echoindex:recipe_search", "Index search provider contract.")
                .register("service", "echoindex:index_service", "Index query service contract.")
                .register("service", "echoindex:inventory_overlay", "Index inventory overlay render and input service contract.")
                .register("integration", "echoindex:terminal", "Terminal Index panel integration.")
                .register("integration", "echoindex:lens", "Lens-to-Index lookup integration.")
                .register("integration", "echoindex:wiki", "Wiki reference integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoIndex.commonSetup", "Attach Index backend services.")
                .hook("data.reload", "IndexRecipeReloaders.register", "Attach recipe and usage graph reloaders.")
                .hook("commands.register", "IndexCommands.register", "Expose Index commands when native command bridge exists.")
                .hook("screen.open", "IndexScreenBridge.open", "Prepare Index screen open flow.")
                .hook("screen.inventory.render", "IndexOverlay.onRender", "Render the Index overlay beside container inventory screens.")
                .hook("screen.inventory.input", "IndexOverlay.inputHandlers", "Route mouse, scroll, key, and character input to the Index overlay.")
                .hook("integration.optional", "IndexIntegrationBridge.attach", "Prepare optional Terminal, Lens, and Wiki integrations.");
        Map<String, Object> queryService = EchoIndexRecipeQueryContract.executeReferenceQuery(
                EchoIndexRecipeQueryContract.REFERENCE_QUERY);
        boolean queryPassed = EchoIndexRecipeQueryContract.referenceQueryPassed(queryService);
        Map<String, Object> inventoryOverlay = inventoryOverlayContract(queryService);
        boolean inventoryOverlayReady = Boolean.TRUE.equals(inventoryOverlay.get("visible"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "index_native_query_service_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", 10);
        result.put("eventHookCount", 7);
        result.put("registeredFeatureContracts", List.of(
                "index.recipes",
                "index.inventory_overlay",
                EchoIndexRecipeQueryContract.ADAPTERCORE_CONTRACT_ID,
                "echoindex:inventory_overlay"
        ));
        result.put("queryService", queryService);
        result.put("inventoryOverlay", inventoryOverlay);
        result.put("queryServiceExecuted", queryPassed);
        result.put("inventoryOverlayReady", inventoryOverlayReady);
        result.put("requiresRecipeBridge", true);
        result.put("requiresUiBridge", true);
        result.put("requiresInventoryOverlayBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("nativeClientRouteRegistrarClass", "com.knoxhack.echoindex.EchoIndexClient");
        result.put("serviceCodeExecuted", queryPassed);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "index_inventory_overlay_projection");
        result.put("summary", "Index native contract registered recipe search plus the inventory overlay surface and executed the AdapterCore query service.");
        return result;
    }

    private static void registerNativeClientRoutesFromModule(EchoNativeModuleLoadContext context) {
        try {
            Object registered = Class.forName("com.knoxhack.echoindex.EchoIndexClient")
                    .getMethod("ensureNativeClientRoutesRegisteredForNativeLoader")
                    .invoke(null);
            boolean mutated = Boolean.TRUE.equals(registered);
            context.attribute("nativeClientRouteRegistrarClass", "com.knoxhack.echoindex.EchoIndexClient");
            context.attribute("nativeClientRoutesRegistered", mutated);
            if (mutated) {
                context.recordMutation("client_routes", "register", "echoindex:native_client_routes",
                        EchoNativeLoadStatus.MUTATED);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeClientRoutesRegistered", false);
            context.attribute("nativeClientRouteRegistrarFailure", exception.getClass().getSimpleName());
        }
    }

    private static Map<String, Object> inventoryOverlayContract(Map<String, Object> queryService) {
        Map<String, Object> overlay = new LinkedHashMap<>();
        overlay.put("surfaceId", "echoindex:inventory_overlay");
        overlay.put("parentSurfaceId", "minecraft:container_screen");
        overlay.put("adapterCoreContract", "index.inventory_overlay");
        overlay.put("service", "echoindex:inventory_overlay");
        overlay.put("focusedControl", "index:overlay_search");
        overlay.put("renderHook", "IndexOverlay.onRender");
        overlay.put("inputHooks", List.of(
                "IndexOverlay.onMouseClicked",
                "IndexOverlay.onMouseDragged",
                "IndexOverlay.onMouseReleased",
                "IndexOverlay.onMouseScrolled",
                "IndexOverlay.onKeyPressed",
                "IndexOverlay.onCharTyped"
        ));
        overlay.put("actions", List.of(
                "index.inventory_overlay_render",
                "index.inventory_overlay_input",
                "index.open_recipes_for_item",
                "index.open_usages_for_item",
                "index.track_item",
                "index.toggle_favorite"
        ));
        overlay.put("recipeRows", list(queryService.get("resultIds")));
        overlay.put("visible", true);
        overlay.put("referenceBehavior", "inventory_screen_renders_index_side_drawer");
        return Map.copyOf(overlay);
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        if (nativeLoaderActivation == null) {
            nativeLoaderActivation = describeNativeSurfaces(nativeLoaderContext(context));
        }
        return nativeLoaderActivation;
    }

    private static Map<String, String> nativeLoaderContext(EchoNativeModuleLoadContext context) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("moduleId", context.descriptor().id());
        data.put("moduleName", context.descriptor().name());
        data.put("moduleVersion", context.descriptor().version());
        data.put("runtime", "echo_native");
        data.put("loader", "echo-native-loader");
        data.put("packId", string(context.attributes().getOrDefault("packId", "native-loader")));
        data.put("descriptorPath", context.descriptor().descriptorPath() == null
                ? ""
                : context.descriptor().descriptorPath().toString());
        return Map.copyOf(data);
    }

    private static void registerFeatureContracts(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        for (Object contract : list(data.get("registeredFeatureContracts"))) {
            String id = string(contract);
            if (!id.isBlank()) {
                context.registerService(
                        "feature." + normalized(id),
                        new NativeService("feature_contract", Map.of("id", id)),
                        "features",
                        "contracts",
                        "adaptercore");
            }
        }
    }

    private static void registerEventHooks(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("eventBridge"));
        Object eventHost = nativeHost(context, EVENT_HOST_SERVICE_ID);
        int nativeSubscriptionCount = 0;
        for (Map<String, Object> hook : objectList(bridge.get("hooks"))) {
            String event = string(hook.get("event"));
            String handler = string(hook.get("handler"));
            if (!event.isBlank() && !handler.isBlank()) {
                Map<String, Object> evidence = new LinkedHashMap<>(hook);
                if (subscribeNativeEventHook(context, eventHost, event, handler, hook)) {
                    nativeSubscriptionCount++;
                    evidence.put("nativeEventHostSubscribed", true);
                    context.recordMutation(
                            "events",
                            "echoindex_native_event_handler_subscribed",
                            event + "#" + handler,
                            EchoNativeLoadStatus.MUTATED);
                } else {
                    evidence.put("nativeEventHostSubscribed", false);
                }
                context.registerService(
                        "event." + normalized(event) + "." + normalized(handler),
                        new NativeService("event_hook", Map.copyOf(evidence)),
                        "events",
                        normalized(event));
            }
        }
        if (nativeSubscriptionCount > 0) {
            context.attribute("echoIndexNativeEventHostSubscriptionCount", nativeSubscriptionCount);
        }
    }

    private static void registerLifecyclePhases(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("lifecycleBridge"));
        Object lifecycleHost = nativeHost(context, LIFECYCLE_HOST_SERVICE_ID);
        int nativeLifecycleRecordCount = 0;
        for (Map<String, Object> phase : objectList(bridge.get("phases"))) {
            String id = string(phase.get("id"));
            if (!id.isBlank()) {
                Map<String, Object> evidence = new LinkedHashMap<>(phase);
                if (recordNativeLifecyclePhase(context, lifecycleHost, id, phase)) {
                    nativeLifecycleRecordCount++;
                    evidence.put("nativeLifecycleHostRecorded", true);
                    context.recordMutation(
                            "lifecycle",
                            "echoindex_native_lifecycle_phase_recorded",
                            id,
                            EchoNativeLoadStatus.MUTATED);
                } else {
                    evidence.put("nativeLifecycleHostRecorded", false);
                }
                context.registerService(
                        "lifecycle.echoindex." + normalized(id),
                        new NativeService("lifecycle_phase", Map.copyOf(evidence)),
                        "lifecycle");
            }
        }
        if (nativeLifecycleRecordCount > 0) {
            context.attribute("echoIndexNativeLifecycleHostRecordCount", nativeLifecycleRecordCount);
        }
    }

    private static Object nativeHost(EchoNativeModuleLoadContext context, String serviceId) {
        return context.serviceRegistry()
                .service("echocore", serviceId)
                .or(() -> context.serviceRegistry().service(serviceId))
                .orElse(null);
    }

    private static boolean subscribeNativeEventHook(
            EchoNativeModuleLoadContext context,
            Object eventHost,
            String event,
            String handler,
            Map<String, Object> hook
    ) {
        if (eventHost == null) {
            return false;
        }
        try {
            Method subscribe = eventHost.getClass().getMethod(
                    "subscribeDeclaredHook",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            subscribe.invoke(eventHost, context.descriptor().id(), event, handler, Map.copyOf(hook));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            context.attribute("echoIndexNativeEventHostSubscriptionError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean recordNativeLifecyclePhase(
            EchoNativeModuleLoadContext context,
            Object lifecycleHost,
            String phaseId,
            Map<String, Object> phase
    ) {
        if (lifecycleHost == null) {
            return false;
        }
        try {
            Method record = lifecycleHost.getClass().getMethod(
                    "recordDeclaredLifecyclePhase",
                    String.class,
                    String.class,
                    Map.class
            );
            record.invoke(lifecycleHost, context.descriptor().id(), phaseId, Map.copyOf(phase));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            context.attribute("echoIndexNativeLifecycleHostRecordError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    private static List<Object> list(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = object(item);
            if (!object.isEmpty()) {
                result.add(object);
            }
        }
        return List.copyOf(result);
    }

    private static String normalized(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('.');
                previousSeparator = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '.') {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static final String MODULE_ID = "echoindex";

    private record NativeService(String kind, Map<String, Object> evidence) {
        private NativeService {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }
}
