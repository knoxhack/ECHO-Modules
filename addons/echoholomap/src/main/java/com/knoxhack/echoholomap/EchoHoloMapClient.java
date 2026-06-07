package com.knoxhack.echoholomap;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoholomap.client.BuiltinHoloMapClientChunkActionProvider;
import com.knoxhack.echoholomap.client.HoloMapClientChunkActions;
import com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay;
import com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen;
import com.knoxhack.echoholomap.client.HoloMapUiController;
import com.knoxhack.echoholomap.platform.HoloMapModuleAccess;
import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientRouteActionContext;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EchoHoloMapClient {
    private static final AtomicBoolean NATIVE_ROUTE_REGISTERED = new AtomicBoolean(false);
    private static final ThreadLocal<HoloMapFullScreenMapScreen> NATIVE_FULLSCREEN_SCREEN = new ThreadLocal<>();
    private static final ThreadLocal<NativeScreenCoreFullscreenTarget> NATIVE_SCREENCORE_FULLSCREEN =
            new ThreadLocal<>();
    private static final ThreadLocal<NativeScreenCoreActionRunner> NATIVE_SCREENCORE_ACTIONS =
            new ThreadLocal<>();
    private static final ThreadLocal<NativeMiniMapRenderContext> NATIVE_MINIMAP_RENDER =
            new ThreadLocal<>();
    private static Map<String, Object> nativeRouteState = Map.of(
            "nativeRouteEnabled", false,
            "surface", "holomap",
            "lastActionId", "",
            "lastKind", "",
            "lastResult", false);
    private static final Identifier MINIMAP_LAYER =
            Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "minimap");
    private static final KeyMapping.Category KEY_CATEGORY =
            registerKeyCategory("holomap");
    public static final KeyMapping TOGGLE_MINIMAP_KEY = new KeyMapping(
            "key.echoholomap.toggle_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KEY_CATEGORY);
    public static final KeyMapping OPEN_MAP_KEY = new KeyMapping(
            "key.echoholomap.open_map",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            KEY_CATEGORY);
    public static final KeyMapping MINIMAP_ZOOM_IN_KEY = new KeyMapping(
            "key.echoholomap.minimap_zoom_in",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            KEY_CATEGORY);
    public static final KeyMapping MINIMAP_ZOOM_OUT_KEY = new KeyMapping(
            "key.echoholomap.minimap_zoom_out",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,
            KEY_CATEGORY);
    public static final KeyMapping MINIMAP_CYCLE_CORNER_KEY = new KeyMapping(
            "key.echoholomap.minimap_cycle_corner",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            KEY_CATEGORY);
    private static final Map<Integer, NativeHoloMapInputBinding> NATIVE_INPUT_BINDINGS = Map.ofEntries(
            Map.entry(GLFW.GLFW_KEY_J, new NativeHoloMapInputBinding(
                    "holomap.open",
                    "key.echoholomap.open_map",
                    GLFW.GLFW_KEY_J)),
            Map.entry(GLFW.GLFW_KEY_K, new NativeHoloMapInputBinding(
                    "holomap.toggle_minimap",
                    "key.echoholomap.toggle_minimap",
                    GLFW.GLFW_KEY_K)),
            Map.entry(GLFW.GLFW_KEY_RIGHT_BRACKET, new NativeHoloMapInputBinding(
                    "holomap.zoom_in",
                    "key.echoholomap.minimap_zoom_in",
                    GLFW.GLFW_KEY_RIGHT_BRACKET)),
            Map.entry(GLFW.GLFW_KEY_LEFT_BRACKET, new NativeHoloMapInputBinding(
                    "holomap.zoom_out",
                    "key.echoholomap.minimap_zoom_out",
                    GLFW.GLFW_KEY_LEFT_BRACKET)),
            Map.entry(GLFW.GLFW_KEY_BACKSLASH, new NativeHoloMapInputBinding(
                    "holomap.cycle_corner",
                    "key.echoholomap.minimap_cycle_corner",
                    GLFW.GLFW_KEY_BACKSLASH)));

    public EchoHoloMapClient() {
        HoloMapClientChunkActions.register(BuiltinHoloMapClientChunkActionProvider.INSTANCE);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoHoloMapClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(ClientModEvents::onRegisterKeyMappings);
        EchoBackendLifecycleBridge.registerGameEventHandler(ClientModEvents::registerGuiLayers);
        if (HoloMapModuleAccess.isLoaded("echoterminal")) {
            registerTerminalClientIntegration();
        }
        if (HoloMapModuleAccess.isLoaded("echoscreencore")) {
            registerScreenCoreIntegration();
        }
        registerNativeClientRoutes();
    }

    private static void onKeyInput(Object event) {
        if (!EchoBackendClientBridge.keyActionEquals(event, GLFW.GLFW_PRESS)) {
            return;
        }
        if (nativeLoaderActive()) {
            EchoNativeLoadStatus status = dispatchNativeInput(event);
            if (status == EchoNativeLoadStatus.MUTATED) {
                return;
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        int keyCode = EchoBackendClientBridge.keyCode(event);
        if (EchoBackendClientBridge.keyMappingMatches(TOGGLE_MINIMAP_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_K)) {
            toggleMiniMap();
        } else if (EchoBackendClientBridge.keyMappingMatches(OPEN_MAP_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_J)) {
            openHoloMapScreen();
        } else if (EchoBackendClientBridge.keyMappingMatches(MINIMAP_ZOOM_IN_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET)) {
            zoomMiniMapIn();
        } else if (EchoBackendClientBridge.keyMappingMatches(MINIMAP_ZOOM_OUT_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_LEFT_BRACKET)) {
            zoomMiniMapOut();
        } else if (EchoBackendClientBridge.keyMappingMatches(MINIMAP_CYCLE_CORNER_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_BACKSLASH)) {
            cycleMiniMapCorner();
        }
    }

    private static void renderMiniMapLayer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (nativeLoaderActive()) {
            NATIVE_MINIMAP_RENDER.set(new NativeMiniMapRenderContext(graphics, deltaTracker));
            try {
                EchoNativeClientRouteRegistries.get().renderGuiLayer("holomap", "holomap.minimap.render", Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "adaptercore_compatibility_adapter",
                        "eventType", "gui_layer_render",
                        "layerId", MINIMAP_LAYER.toString()
                ));
            } finally {
                NATIVE_MINIMAP_RENDER.remove();
            }
            return;
        }
        HoloMapMiniMapOverlay.render(graphics, deltaTracker);
    }

    private static void registerTerminalClientIntegration() {
        try {
            Class.forName("com.knoxhack.echoholomap.integration.HoloMapTerminalClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            EchoHoloMap.LOGGER.warn("ECHO HoloMap terminal client integration could not be registered.", exception);
        }
    }

    private static void registerScreenCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoHoloMap.LOGGER.warn("ECHO HoloMap ScreenCore integration could not be registered.", exception);
        }
    }

    private static KeyMapping.Category registerKeyCategory(String path) {
        try {
            return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, path));
        } catch (IllegalArgumentException duplicate) {
            String uniquePath = path + "_native_" + Long.toUnsignedString(System.nanoTime(), 36);
            EchoHoloMap.LOGGER.debug("ECHO HoloMap key category {} already exists; using {} for this client loader.",
                    path, uniquePath);
            return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, uniquePath));
        }
    }

    private static boolean openScreenCoreMap() {
        if (!HoloMapModuleAccess.isLoaded("echoscreencore")) {
            return false;
        }
        try {
            Object result = Class.forName("com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration")
                    .getMethod("openFullscreen")
                    .invoke(null);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoHoloMap.LOGGER.warn("ECHO HoloMap ScreenCore fullscreen could not be opened.", exception);
            return false;
        }
    }

    public static boolean ensureNativeClientRoutesRegisteredForNativeLoader() {
        registerNativeClientRoutes();
        return NATIVE_ROUTE_REGISTERED.get();
    }

    private static void registerNativeClientRoutes() {
        if (!nativeLoaderActive() || !NATIVE_ROUTE_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        if (registry == EchoNativeClientRouteRegistry.NOOP) {
            NATIVE_ROUTE_REGISTERED.set(false);
            return;
        }
        registry.registerRoute(
                EchoHoloMap.MODID,
                "echoholomap:minimap",
                "holomap",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                        "source", "echoholomap_native_module_route_registrar"),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry"),
                true);
        registry.registerActions(EchoHoloMap.MODID, "echoholomap:minimap", "holomap", Map.of(
                "holomap.minimap.render", Map.of("kind", "overlay_render"),
                "holomap.toggle_minimap", Map.of("kind", "overlay_command", "bridgeMethod", "toggle"),
                "holomap.zoom_in", Map.of("kind", "overlay_command", "bridgeMethod", "zoomIn"),
                "holomap.zoom_out", Map.of("kind", "overlay_command", "bridgeMethod", "zoomOut"),
                "holomap.cycle_corner", Map.of("kind", "overlay_command", "bridgeMethod", "cycleCorner")));
        registry.registerRoute(
                EchoHoloMap.MODID,
                "echoholomap:fullscreen_map",
                "holomap",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                        "nativeScreenBridgeClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "source", "echoholomap_native_module_route_registrar"),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry"),
                true);
        registry.registerActions(EchoHoloMap.MODID, "echoholomap:fullscreen_map", "holomap", Map.ofEntries(
                Map.entry("holomap.open", Map.of("kind", "screen_bridge", "bridgeMethod", "openFullscreen")),
                Map.entry("holomap.fullscreen.key", Map.of("kind", "fullscreen_key_input")),
                Map.entry("holomap.fullscreen.mouse", Map.of("kind", "fullscreen_mouse_input")),
                Map.entry("holomap.fullscreen.scroll", Map.of("kind", "fullscreen_scroll_input")),
                Map.entry("holomap.sync", Map.of("kind", "fullscreen_command", "bridgeMethod", "sync")),
                Map.entry("holomap.center", Map.of("kind", "fullscreen_command", "bridgeMethod", "center")),
                Map.entry("holomap.toggle_markers", Map.of("kind", "fullscreen_command", "bridgeMethod", "toggleMarkers")),
                Map.entry("holomap.cycle_fields", Map.of("kind", "fullscreen_command", "bridgeMethod", "cycleFields")),
                Map.entry("holomap.toggle_waypoints", Map.of("kind", "fullscreen_command", "bridgeMethod", "toggleWaypoints")),
                Map.entry("holomap.select_entry", Map.of("kind", "fullscreen_command", "bridgeMethod", "selectEntry")),
                Map.entry("holomap.close", Map.of("kind", "fullscreen_command", "bridgeMethod", "close"))));
        NATIVE_INPUT_BINDINGS.values().forEach(binding -> registerInputBinding(
                registry,
                binding.actionId(),
                binding.keyMapping(),
                binding.keyCode()));
        registry.registerActionHandler("holomap", "echoholomap:minimap",
                context -> dispatchNativeRouteSurface("echoholomap:minimap", context));
        registry.registerActionHandler("holomap", "echoholomap:fullscreen_map",
                context -> dispatchNativeRouteSurface("echoholomap:fullscreen_map", context));
        recordNativeRoute("holomap.native_route.enable", Map.of("kind", "route_enable"), true);
    }

    private static void registerInputBinding(
            EchoNativeClientRouteRegistry registry,
            String actionId,
            String keyMapping,
            int keyCode
    ) {
        registry.registerInputBinding("holomap", actionId, Map.of(
                "keyMapping", keyMapping,
                "keyCode", keyCode,
                "inputType", "press",
                "action", actionId,
                "source", "echoholomap_native_input_route_registry",
                "nativeLoaderHostService", "key_input",
                "nativeInputOwner", "EchoNativeClientRouteRegistries"));
    }

    private static EchoNativeLoadStatus dispatchNativeInput(Object event) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        int keyCode = key(event);
        NativeHoloMapInputBinding binding = NATIVE_INPUT_BINDINGS.get(keyCode);
        if (binding == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return registry.keyInput(
                binding.keyMapping(),
                binding.keyCode(),
                "press",
                nativeKeyMetadata(event, binding));
    }

    private static Map<String, Object> nativeKeyMetadata(Object event, NativeHoloMapInputBinding binding) {
        return Map.of(
                "source", "native_loader_input_binding",
                "forwardedFrom", "adaptercore_compatibility_adapter",
                "eventType", "key_input",
                "actionId", binding.actionId(),
                "keyMapping", binding.keyMapping(),
                "inputType", "press",
                "key", key(event),
                "glfwAction", action(event),
                "keyEvent", String.valueOf(keyEvent(event))
        );
    }

    private static boolean dispatchNativeRouteSurface(
            String expectedSurfaceId,
            NativeClientRouteActionContext context
    ) {
        if (!text(expectedSurfaceId).equals(text(context.route().get("surfaceId")))) {
            return false;
        }
        return dispatchNativeClientRoute(context);
    }

    private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context) {
        String kind = text(context.action().get("kind"));
        boolean handled;
        if ("screen_bridge".equals(kind)) {
            handled = openHoloMapScreen();
        } else if ("fullscreen_key_input".equals(kind)) {
            handled = dispatchNativeFullscreenKey(context);
        } else if ("fullscreen_mouse_input".equals(kind)) {
            handled = dispatchNativeFullscreenMouse(context);
        } else if ("fullscreen_scroll_input".equals(kind)) {
            handled = dispatchNativeFullscreenScroll(context);
        } else if ("fullscreen_command".equals(kind)) {
            handled = dispatchNativeFullscreenCommand(text(context.action().get("bridgeMethod")), context.metadata());
        } else if ("overlay_render".equals(kind)) {
            NativeMiniMapRenderContext renderContext = NATIVE_MINIMAP_RENDER.get();
            if (renderContext != null) {
                HoloMapMiniMapOverlay.render(renderContext.graphics(), renderContext.deltaTracker());
                handled = true;
            } else {
                handled = false;
            }
        } else if ("overlay_command".equals(kind)) {
            handled = dispatchMiniMapCommand(text(context.action().get("bridgeMethod")));
        } else {
            handled = switch (context.actionId()) {
                case "holomap.open" -> openHoloMapScreen();
                case "holomap.fullscreen.key" -> dispatchNativeFullscreenKey(context);
                case "holomap.fullscreen.mouse" -> dispatchNativeFullscreenMouse(context);
                case "holomap.fullscreen.scroll" -> dispatchNativeFullscreenScroll(context);
                case "holomap.sync" -> dispatchNativeFullscreenCommand("sync");
                case "holomap.center" -> dispatchNativeFullscreenCommand("center");
                case "holomap.toggle_markers" -> dispatchNativeFullscreenCommand("toggleMarkers");
                case "holomap.cycle_fields" -> dispatchNativeFullscreenCommand("cycleFields");
                case "holomap.toggle_waypoints" -> dispatchNativeFullscreenCommand("toggleWaypoints");
                case "holomap.select_entry" -> dispatchNativeFullscreenCommand("selectEntry", context.metadata());
                case "holomap.close" -> dispatchNativeFullscreenCommand("close");
                case "holomap.minimap.render" -> {
                    NativeMiniMapRenderContext renderContext = NATIVE_MINIMAP_RENDER.get();
                    if (renderContext != null) {
                        HoloMapMiniMapOverlay.render(renderContext.graphics(), renderContext.deltaTracker());
                        yield true;
                    }
                    yield false;
                }
                case "holomap.toggle_minimap" -> toggleMiniMap();
                case "holomap.zoom_in" -> zoomMiniMapIn();
                case "holomap.zoom_out" -> zoomMiniMapOut();
                case "holomap.cycle_corner" -> cycleMiniMapCorner();
                default -> false;
            };
        }
        recordNativeRoute(context.actionId(), context.action(), handled, context.metadata());
        return handled;
    }

    public static synchronized Map<String, Object> nativeRouteState() {
        return Map.copyOf(nativeRouteState);
    }

    public static synchronized Map<String, Object> recordNativeOverlayMutation(
            String actionId,
            String kind,
            boolean handled,
            Map<String, Object> metadata
    ) {
        recordNativeRoute(actionId, Map.of("kind", textOrDefault(kind, "overlay_command")), handled, metadata);
        return Map.copyOf(nativeRouteState);
    }

    private static synchronized void recordNativeRoute(String actionId, Map<String, Object> action, boolean handled) {
        recordNativeRoute(actionId, action, handled, Map.of());
    }

    private static synchronized void recordNativeRoute(
            String actionId,
            Map<String, Object> action,
            boolean handled,
            Map<String, Object> metadata
    ) {
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        Map<String, Object> next = new LinkedHashMap<>();
        next.put("nativeRouteEnabled", true);
        next.put("surface", "holomap");
        next.put("lastActionId", text(actionId));
        next.put("lastKind", text(action == null ? "" : action.get("kind")));
        next.put("lastResult", handled);
        next.put("lastMetadata", Map.copyOf(safeMetadata));
        putIfPresent(next, "lastSource", safeMetadata.get("source"));
        putIfPresent(next, "lastEventType", safeMetadata.get("eventType"));
        putIfPresent(next, "lastService", safeMetadata.get("service"));
        putIfPresent(next, "lastFrameSource", safeMetadata.get("frameSource"));
        putIfPresent(next, "lastScreenClass", safeMetadata.get("screenClass"));
        putIfPresent(next, "lastPartialTick", safeMetadata.get("partialTick"));
        putIfPresent(next, "lastMouseX", safeMetadata.get("mouseX"));
        putIfPresent(next, "lastMouseY", safeMetadata.get("mouseY"));
        Map<String, Object> minimap = HoloMapMiniMapOverlay.nativeOverlayState();
        Map<String, Object> fullscreen = HoloMapUiController.fullscreen().nativeRouteState();
        next.put("minimap", minimap);
        next.put("fullscreen", fullscreen);
        next.put("routeDrivenMapModel", routeDrivenMapModel(actionId, action, handled, safeMetadata, minimap, fullscreen));
        nativeRouteState = Map.copyOf(next);
    }

    private static Map<String, Object> routeDrivenMapModel(
            String actionId,
            Map<String, Object> action,
            boolean handled,
            Map<String, Object> metadata,
            Map<String, Object> minimap,
            Map<String, Object> fullscreen
    ) {
        String safeActionId = text(actionId);
        String kind = text(action == null ? "" : action.get("kind"));
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "holomap_route");
        model.put("surface", "holomap");
        model.put("routeDrivenMapState", true);
        model.put("actionId", safeActionId);
        model.put("kind", kind);
        model.put("handled", handled);
        model.put("minimapRoute", safeActionId.startsWith("holomap.minimap")
                || safeActionId.equals("holomap.toggle_minimap")
                || safeActionId.equals("holomap.zoom_in")
                || safeActionId.equals("holomap.zoom_out")
                || safeActionId.equals("holomap.cycle_corner")
                || "overlay_render".equals(kind)
                || "overlay_command".equals(kind));
        model.put("fullscreenRoute", safeActionId.startsWith("holomap.fullscreen")
                || safeActionId.equals("holomap.sync")
                || safeActionId.equals("holomap.center")
                || safeActionId.equals("holomap.toggle_markers")
                || safeActionId.equals("holomap.cycle_fields")
                || safeActionId.equals("holomap.toggle_waypoints")
                || safeActionId.equals("holomap.close")
                || "fullscreen_key_input".equals(kind)
                || "fullscreen_mouse_input".equals(kind)
                || "fullscreen_scroll_input".equals(kind)
                || "fullscreen_command".equals(kind));
        model.put("screenBridgeRoute", "screen_bridge".equals(kind) || safeActionId.equals("holomap.open"));
        model.put("minimap", Map.copyOf(minimap));
        model.put("fullscreen", Map.copyOf(fullscreen));
        model.put("routeMetadata", Map.copyOf(safeMetadata));
        model.put("routeMetadataKeys", safeMetadata.keySet().stream().sorted().toList());
        putIfPresent(model, "routeSource", safeMetadata.get("source"));
        putIfPresent(model, "routeEventType", safeMetadata.get("eventType"));
        putIfPresent(model, "routeService", safeMetadata.get("service"));
        putIfPresent(model, "routeFrameSource", safeMetadata.get("frameSource"));
        putIfPresent(model, "routeScreenClass", safeMetadata.get("screenClass"));
        putIfPresent(model, "routePartialTick", safeMetadata.get("partialTick"));
        putIfPresent(model, "routeMouseX", safeMetadata.get("mouseX"));
        putIfPresent(model, "routeMouseY", safeMetadata.get("mouseY"));
        return Map.copyOf(model);
    }

    private static void putIfPresent(Map<String, Object> state, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            state.put(key, value);
        }
    }

    private static boolean dispatchNativeFullscreenCommand(String command) {
        return dispatchNativeFullscreenCommand(command, Map.of());
    }

    private static boolean dispatchNativeFullscreenCommand(String command, Map<String, Object> metadata) {
        HoloMapUiController controller = HoloMapUiController.fullscreen();
        switch (command) {
            case "sync" -> controller.requestSync(true);
            case "center" -> controller.centerOnPlayer();
            case "toggleMarkers" -> controller.toggleMarkers();
            case "cycleFields" -> controller.cycleFieldMode();
            case "toggleWaypoints" -> controller.toggleWaypoints();
            case "selectEntry" -> {
                String entryId = text(metadata == null ? "" : metadata.get("entryId"));
                if (entryId.isBlank()) {
                    return false;
                }
                Identifier id = Identifier.tryParse(entryId);
                if (id == null || !controller.selectEntry(id)) {
                    return false;
                }
            }
            case "close" -> {
                NativeScreenCoreActionRunner actions = NATIVE_SCREENCORE_ACTIONS.get();
                if (actions != null) {
                    return actions.run("holomap.close", "screen_core_route_command");
                }
                EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(
                        "close",
                        "holomap.fullscreen.close",
                        "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                        Map.of("transitionSource", "fullscreen_route_command"));
                if (nativeLoaderActive() && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                    return false;
                }
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            default -> {
                return false;
            }
        }
        try {
            Class.forName("com.knoxhack.echoscreencore.api.EchoScreens")
                    .getMethod("invalidateData")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // ScreenCore may not be present when the classic fullscreen screen handles the same route.
        }
        return true;
    }

    private static boolean dispatchNativeFullscreenKey(NativeClientRouteActionContext context) {
        int key = intMetadata(context.metadata(), "key");
        HoloMapFullScreenMapScreen screen = NATIVE_FULLSCREEN_SCREEN.get();
        if (screen != null) {
            return screen.handleNativeRouteKey(key);
        }
        NativeScreenCoreFullscreenTarget screenCore = NATIVE_SCREENCORE_FULLSCREEN.get();
        return screenCore != null && screenCore.handleNativeRouteKey(key, NATIVE_SCREENCORE_ACTIONS.get());
    }

    private static boolean dispatchNativeFullscreenMouse(NativeClientRouteActionContext context) {
        String phase = text(context.metadata().get("phase"));
        double mouseX = doubleMetadata(context.metadata(), "mouseX");
        double mouseY = doubleMetadata(context.metadata(), "mouseY");
        int button = intMetadata(context.metadata(), "button");
        int modifiers = intMetadata(context.metadata(), "modifiers");
        boolean doubleClick = booleanMetadata(context.metadata(), "doubleClick");
        double dragX = doubleMetadata(context.metadata(), "dragX");
        double dragY = doubleMetadata(context.metadata(), "dragY");
        HoloMapFullScreenMapScreen screen = NATIVE_FULLSCREEN_SCREEN.get();
        if (screen != null) {
            return screen.handleNativeRouteMouse(phase, mouseX, mouseY, button, modifiers, doubleClick, dragX, dragY);
        }
        NativeScreenCoreFullscreenTarget screenCore = NATIVE_SCREENCORE_FULLSCREEN.get();
        return screenCore != null && screenCore.handleNativeRouteMouse(
                phase, mouseX, mouseY, button, modifiers, doubleClick, dragX, dragY, NATIVE_SCREENCORE_ACTIONS.get());
    }

    private static boolean dispatchNativeFullscreenScroll(NativeClientRouteActionContext context) {
        double mouseX = doubleMetadata(context.metadata(), "mouseX");
        double mouseY = doubleMetadata(context.metadata(), "mouseY");
        double scrollX = doubleMetadata(context.metadata(), "scrollX");
        double scrollY = doubleMetadata(context.metadata(), "scrollY");
        HoloMapFullScreenMapScreen screen = NATIVE_FULLSCREEN_SCREEN.get();
        if (screen != null) {
            return screen.handleNativeRouteScroll(mouseX, mouseY, scrollX, scrollY);
        }
        NativeScreenCoreFullscreenTarget screenCore = NATIVE_SCREENCORE_FULLSCREEN.get();
        return screenCore != null && screenCore.handleNativeRouteScroll(mouseX, mouseY, scrollX, scrollY);
    }

    public static boolean dispatchNativeFullscreenKey(HoloMapFullScreenMapScreen screen, int key) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_FULLSCREEN_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().overlayInput("holomap", "holomap.fullscreen.key", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "fullscreen_key_pressed",
                    "screenClass", screen.getClass().getName(),
                    "key", key
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_FULLSCREEN_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeFullscreenMouse(
            HoloMapFullScreenMapScreen screen,
            String phase,
            double mouseX,
            double mouseY,
            int button,
            int modifiers,
            boolean doubleClick,
            double dragX,
            double dragY
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_FULLSCREEN_SCREEN.set(screen);
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "native_screen_lifecycle");
            metadata.put("eventType", "fullscreen_mouse_input");
            metadata.put("screenClass", screen.getClass().getName());
            metadata.put("phase", phase);
            metadata.put("mouseX", mouseX);
            metadata.put("mouseY", mouseY);
            metadata.put("button", button);
            metadata.put("modifiers", modifiers);
            metadata.put("doubleClick", doubleClick);
            metadata.put("dragX", dragX);
            metadata.put("dragY", dragY);
            return EchoNativeClientRouteRegistries.get().mouseInput("holomap", "holomap.fullscreen.mouse", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_FULLSCREEN_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeFullscreenScroll(
            HoloMapFullScreenMapScreen screen,
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_FULLSCREEN_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().mouseInput("holomap", "holomap.fullscreen.scroll", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "fullscreen_scroll_input",
                    "screenClass", screen.getClass().getName(),
                    "mouseX", mouseX,
                    "mouseY", mouseY,
                    "scrollX", scrollX,
                    "scrollY", scrollY
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_FULLSCREEN_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeScreenCoreFullscreenKey(
            NativeScreenCoreFullscreenTarget screen,
            NativeScreenCoreActionRunner actions,
            int key,
            int canvasWidth,
            int canvasHeight
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_SCREENCORE_FULLSCREEN.set(screen);
        NATIVE_SCREENCORE_ACTIONS.set(actions);
        try {
            return EchoNativeClientRouteRegistries.get().overlayInput("holomap", "holomap.fullscreen.key", Map.of(
                    "source", "native_screencore_lifecycle",
                    "eventType", "fullscreen_screencore_key_pressed",
                    "screenClass", screen.nativeRouteScreenClass(),
                    "key", key,
                    "canvasWidth", canvasWidth,
                    "canvasHeight", canvasHeight
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREENCORE_ACTIONS.remove();
            NATIVE_SCREENCORE_FULLSCREEN.remove();
        }
    }

    public static boolean dispatchNativeScreenCoreFullscreenMouse(
            NativeScreenCoreFullscreenTarget screen,
            NativeScreenCoreActionRunner actions,
            String phase,
            double mouseX,
            double mouseY,
            int button,
            int modifiers,
            boolean doubleClick,
            double dragX,
            double dragY,
            int canvasX,
            int canvasY,
            int canvasWidth,
            int canvasHeight
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_SCREENCORE_FULLSCREEN.set(screen);
        NATIVE_SCREENCORE_ACTIONS.set(actions);
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "native_screencore_lifecycle");
            metadata.put("eventType", "fullscreen_screencore_mouse_input");
            metadata.put("screenClass", screen.nativeRouteScreenClass());
            metadata.put("phase", phase);
            metadata.put("mouseX", mouseX);
            metadata.put("mouseY", mouseY);
            metadata.put("button", button);
            metadata.put("modifiers", modifiers);
            metadata.put("doubleClick", doubleClick);
            metadata.put("dragX", dragX);
            metadata.put("dragY", dragY);
            metadata.put("canvasX", canvasX);
            metadata.put("canvasY", canvasY);
            metadata.put("canvasWidth", canvasWidth);
            metadata.put("canvasHeight", canvasHeight);
            return EchoNativeClientRouteRegistries.get().mouseInput("holomap", "holomap.fullscreen.mouse", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREENCORE_ACTIONS.remove();
            NATIVE_SCREENCORE_FULLSCREEN.remove();
        }
    }

    public static boolean dispatchNativeScreenCoreFullscreenScroll(
            NativeScreenCoreFullscreenTarget screen,
            double mouseX,
            double mouseY,
            double scrollY,
            int canvasX,
            int canvasY,
            int canvasWidth,
            int canvasHeight
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_SCREENCORE_FULLSCREEN.set(screen);
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "native_screencore_lifecycle");
            metadata.put("eventType", "fullscreen_screencore_scroll_input");
            metadata.put("screenClass", screen.nativeRouteScreenClass());
            metadata.put("mouseX", mouseX);
            metadata.put("mouseY", mouseY);
            metadata.put("scrollX", 0.0D);
            metadata.put("scrollY", scrollY);
            metadata.put("canvasX", canvasX);
            metadata.put("canvasY", canvasY);
            metadata.put("canvasWidth", canvasWidth);
            metadata.put("canvasHeight", canvasHeight);
            return EchoNativeClientRouteRegistries.get().mouseInput("holomap", "holomap.fullscreen.scroll", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREENCORE_FULLSCREEN.remove();
        }
    }

    public static boolean dispatchNativeScreenCoreFullscreenCommand(
            String actionId,
            String command,
            NativeScreenCoreActionRunner actions
    ) {
        return dispatchNativeScreenCoreFullscreenCommand(actionId, command, actions, Map.of());
    }

    public static boolean dispatchNativeScreenCoreFullscreenCommand(
            String actionId,
            String command,
            NativeScreenCoreActionRunner actions,
            Map<String, Object> metadata
    ) {
        if (!nativeLoaderActive()) {
            return false;
        }
        NATIVE_SCREENCORE_ACTIONS.set(actions);
        try {
            Map<String, Object> routeMetadata = new LinkedHashMap<>();
            routeMetadata.put("source", "native_screencore_lifecycle");
            routeMetadata.put("eventType", "fullscreen_screencore_command");
            routeMetadata.put("screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration");
            routeMetadata.put("command", command);
            if (metadata != null) {
                routeMetadata.putAll(metadata);
            }
            return EchoNativeClientRouteRegistries.get().dispatchStatus("holomap", actionId, Map.copyOf(routeMetadata))
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREENCORE_ACTIONS.remove();
        }
    }

    public interface NativeScreenCoreFullscreenTarget {
        String nativeRouteScreenClass();

        boolean handleNativeRouteMouse(
                String phase,
                double mouseX,
                double mouseY,
                int button,
                int modifiers,
                boolean doubleClick,
                double dragX,
                double dragY,
                NativeScreenCoreActionRunner actions
        );

        boolean handleNativeRouteScroll(double mouseX, double mouseY, double scrollX, double scrollY);

        boolean handleNativeRouteKey(int key, NativeScreenCoreActionRunner actions);
    }

    @FunctionalInterface
    public interface NativeScreenCoreActionRunner {
        boolean run(String actionId, String reason);
    }

    private record NativeMiniMapRenderContext(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    }

    private record NativeHoloMapInputBinding(String actionId, String keyMapping, int keyCode) {
    }

    public static boolean nativeLoaderClientActiveForScreens() {
        return nativeLoaderActive();
    }

    public static EchoNativeLoadStatus publishNativeScreenLifecycle(
            String phase,
            String actionId,
            String screenClass,
            Map<String, Object> metadata
    ) {
        if (!nativeLoaderActive()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("source", "native_holomap_screen_transition");
        event.put("eventType", "holomap_screen_lifecycle");
        event.put("screenClass", screenClass == null ? "" : screenClass);
        event.put("screenTransitionPhase", phase == null ? "" : phase);
        if (metadata != null) {
            event.putAll(metadata);
        }
        event.put("nativeLoaderUiHostService", "screen_lifecycle");
        event.put("nativeLoaderUiHostSurface", "holomap");
        event.put("nativeLoaderUiHostAction", textOrDefault(actionId, "holomap.screen.lifecycle"));
        event.put("nativeLoaderScreenLifecycleHandoff", true);
        String safePhase = phase == null ? "" : phase;
        String safeActionId = textOrDefault(actionId, "holomap.screen.lifecycle");
        return switch (safePhase) {
            case "mount" -> EchoNativeClientRouteRegistries.get().mountSurface("holomap", safeActionId, Map.copyOf(event));
            case "open" -> EchoNativeClientRouteRegistries.get().openSurface("holomap", safeActionId, Map.copyOf(event));
            case "close" -> EchoNativeClientRouteRegistries.get().closeSurface("holomap", safeActionId, Map.copyOf(event));
            case "unmount" -> EchoNativeClientRouteRegistries.get().unmountSurface("holomap", safeActionId, Map.copyOf(event));
            default -> EchoNativeClientRouteRegistries.get().screenLifecycle(
                    "holomap",
                    safePhase,
                    safeActionId,
                    Map.copyOf(event));
        };
    }

    private static boolean openHoloMapScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (minecraft.screen instanceof HoloMapFullScreenMapScreen) {
            return true;
        }
        if (minecraft.screen != null) {
            return false;
        }
        if (openScreenCoreMap()) {
            publishNativeScreenLifecycle(
                    "open",
                    "holomap.open",
                    "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                    Map.of(
                            "targetScreenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                            "transitionSource", "holomap_route_open",
                            "screenBridge", "echoscreencore"));
            return true;
        }
        EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(
                "open",
                "holomap.open",
                HoloMapFullScreenMapScreen.class.getName(),
                Map.of(
                        "targetScreenClass", HoloMapFullScreenMapScreen.class.getName(),
                        "transitionSource", "holomap_route_open",
                        "screenBridge", "classic_fullscreen"));
        if (nativeLoaderActive()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED
                && lifecycleStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return false;
        }
        minecraft.setScreen(new HoloMapFullScreenMapScreen());
        return true;
    }

    private static String textOrDefault(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private static boolean dispatchMiniMapCommand(String command) {
        return switch (command) {
            case "toggle" -> toggleMiniMap();
            case "zoomIn" -> zoomMiniMapIn();
            case "zoomOut" -> zoomMiniMapOut();
            case "cycleCorner" -> cycleMiniMapCorner();
            default -> false;
        };
    }

    private static boolean toggleMiniMap() {
        if (!nativeClientWorldReady()) {
            return false;
        }
        HoloMapMiniMapOverlay.toggle();
        notifyMiniMapState("toggle");
        return true;
    }

    private static boolean zoomMiniMapIn() {
        if (!nativeClientWorldReady()) {
            return false;
        }
        HoloMapMiniMapOverlay.zoomIn();
        notifyMiniMapState("zoom in");
        return true;
    }

    private static boolean zoomMiniMapOut() {
        if (!nativeClientWorldReady()) {
            return false;
        }
        HoloMapMiniMapOverlay.zoomOut();
        notifyMiniMapState("zoom out");
        return true;
    }

    private static boolean cycleMiniMapCorner() {
        if (!nativeClientWorldReady()) {
            return false;
        }
        HoloMapMiniMapOverlay.cycleCorner();
        notifyMiniMapState("corner");
        return true;
    }

    private static void notifyMiniMapState(String action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(
                    "HoloMap " + action + " // " + HoloMapMiniMapOverlay.nativeOverlayStatusLine()));
        }
    }

    private static boolean nativeClientWorldReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.screen == null;
    }

    private static int intMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? Integer.MIN_VALUE : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static double doubleMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? Double.NaN : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private static boolean booleanMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean nativeLoaderActive() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static final class ClientModEvents {
        private ClientModEvents() {
        }

        static void onRegisterKeyMappings(Object event) {
            EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
            EchoBackendClientBridge.registerKeyMapping(event, TOGGLE_MINIMAP_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, OPEN_MAP_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, MINIMAP_ZOOM_IN_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, MINIMAP_ZOOM_OUT_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, MINIMAP_CYCLE_CORNER_KEY);
        }

        static void registerGuiLayers(Object event) {
            EchoBackendClientBridge.registerGuiLayerAboveAir(event, MINIMAP_LAYER, EchoHoloMapClient::renderMiniMapLayer);
        }
    }

    private static int key(Object event) {
        Object key = invokeNoArg(event, "getKey");
        return key instanceof Number number ? number.intValue() : Integer.MIN_VALUE;
    }

    private static int action(Object event) {
        Object action = invokeNoArg(event, "getAction");
        return action instanceof Number number ? number.intValue() : Integer.MIN_VALUE;
    }

    private static Object keyEvent(Object event) {
        return invokeNoArg(event, "getKeyEvent");
    }

    private static Object invokeNoArg(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
