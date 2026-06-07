package com.knoxhack.echoterminal;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.client.BuiltinTerminalTabs;
import com.knoxhack.echoterminal.client.TerminalEventHandler;
import com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud;
import com.knoxhack.echoterminal.client.mission.TerminalMissionHudController;
import com.knoxhack.echoterminal.client.screen.EchoNativeTerminalScreen;
import com.knoxhack.echoterminal.client.screen.EchoTerminalNativeSessionBridge;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.screen.TerminalClientConfigIntegration;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import com.knoxhack.echoterminal.registry.ModMenus;
import com.knoxhack.echoscreencore.api.action.EchoAction;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientRouteActionContext;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class EchoTerminalClient {
    private static final AtomicBoolean NATIVE_ROUTE_REGISTERED = new AtomicBoolean(false);
    private static final ThreadLocal<TerminalScreenCoreScreen> NATIVE_SCREEN_CORE_SCREEN = new ThreadLocal<>();
    private static final ThreadLocal<EchoAction> NATIVE_SCREEN_CORE_ACTION = new ThreadLocal<>();
    private static final ThreadLocal<EchoActionContext> NATIVE_SCREEN_CORE_ACTION_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Object> NATIVE_TERMINAL_OVERLAY_RENDER = new ThreadLocal<>();
    private static Map<String, Object> nativeRouteState = Map.of(
            "nativeRouteEnabled", false,
            "surface", "terminal",
            "lastActionId", "",
            "lastKind", "",
            "lastResult", false);
    private static final List<NativeTerminalInputBinding> NATIVE_TERMINAL_INPUT_BINDINGS = List.of(
            new NativeTerminalInputBinding("terminal.open", "key.echoterminal.open", GLFW.GLFW_KEY_M));
    private static final KeyMapping.Category KEY_CATEGORY =
            registerKeyCategory("terminal");
    public static final KeyMapping OPEN_TERMINAL_KEY = new KeyMapping(
            "key.echoterminal.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KEY_CATEGORY);

    public EchoTerminalClient() {
        this(null);
    }

    public EchoTerminalClient(Object modEventBus) {
        TerminalClientOptions.load();
        TerminalClientConfigIntegration.register();
        BuiltinTerminalTabs.register();
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoTerminalClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoTerminalClient::onClientTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoTerminalClient::onRenderGui);
        TerminalEventHandler.register();
        if (EchoRuntimeModules.isLoaded("echorendercore")) {
            registerRenderCoreScreenIntegration();
        }
        if (EchoRuntimeModules.isLoaded("echoscreencore")) {
            registerScreenCoreIntegration();
        }
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterKeyMappings);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterMenuScreens);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterRenderers);
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
        if (!EchoBackendClientBridge.keyMappingMatches(OPEN_TERMINAL_KEY, event)
                && (!nativeLoaderActive() || EchoBackendClientBridge.keyCode(event) != GLFW.GLFW_KEY_M)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen)) {
            minecraft.setScreen(null);
        } else {
            openTerminalScreen();
        }
    }

    private static void onClientTick(Object event) {
        if (nativeLoaderActive()) {
            EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
            registry.tickRoute("client_overlay", "terminal.mission_hud.tick", "post", Map.of(
                    "source", "native_loader_tick_service",
                    "forwardedFrom", "native_client_bridge",
                    "eventType", "client_tick_post",
                    "overlay", "mission_hud"
            ));
            registry.tickRoute("client_overlay", "terminal.discovery_toast.tick", "post", Map.of(
                    "source", "native_loader_tick_service",
                    "forwardedFrom", "native_client_bridge",
                    "eventType", "client_tick_post",
                    "overlay", "discovery_toast"
            ));
            return;
        }
        TerminalMissionHudController.tick();
        DiscoveryToastHud.tick();
    }

    private static void onRenderGui(Object event) {
        if (nativeLoaderActive()) {
            NATIVE_TERMINAL_OVERLAY_RENDER.set(event);
            EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
            float partialTick = EchoBackendClientBridge.guiPartialTick(event);
            try {
                registry.renderGuiLayer("client_overlay", "terminal.mission_hud.render", Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "native_client_bridge",
                        "eventType", "render_gui_post",
                        "overlay", "mission_hud",
                        "partialTick", partialTick
                ));
                registry.renderGuiLayer("client_overlay", "terminal.discovery_toast.render", Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "native_client_bridge",
                        "eventType", "render_gui_post",
                        "overlay", "discovery_toast",
                        "partialTick", partialTick
                ));
            } finally {
                NATIVE_TERMINAL_OVERLAY_RENDER.remove();
            }
            return;
        }
        GuiGraphicsExtractor graphics = EchoBackendClientBridge.guiGraphics(event);
        float partialTick = EchoBackendClientBridge.guiPartialTick(event);
        TerminalMissionHudController.render(graphics, partialTick);
        DiscoveryToastHud.render(graphics, partialTick);
    }

    private static void registerRenderCoreScreenIntegration() {
        try {
            Class.forName("com.knoxhack.echoterminal.integration.TerminalRenderCoreClientIntegration")
                    .getMethod("registerScreenVisuals")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoTerminal.LOGGER.warn("ECHO Terminal RenderCore screen integration could not be registered.", exception);
        }
    }

    private static void registerScreenCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreBridge")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoTerminal.LOGGER.warn("ECHO Terminal ScreenCore integration could not be registered.", exception);
        }
    }

    private static KeyMapping.Category registerKeyCategory(String path) {
        try {
            return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path));
        } catch (IllegalArgumentException duplicate) {
            String uniquePath = path + "_native_" + Long.toUnsignedString(System.nanoTime(), 36);
            EchoTerminal.LOGGER.debug("ECHO Terminal key category {} already exists; using {} for this client loader.",
                    path, uniquePath);
            return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoTerminal.MODID, uniquePath));
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
                EchoTerminal.MODID,
                "echoterminal:eui",
                "terminal",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen",
                        "nativeScreenBridgeClass", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreens",
                        "source", "echoterminal_native_module_route_registrar"),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry"),
                true);
        registry.registerActions(EchoTerminal.MODID, "echoterminal:eui", "terminal", Map.ofEntries(
                Map.entry("terminal.open", Map.of(
                        "kind", "terminal_screen",
                        "bridgeMethod", "create",
                        "command", EchoTerminalDashboardContract.REFERENCE_COMMAND,
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.command_deck", terminalTabAction("overview", "Command Deck")),
                Map.entry("terminal.survival_route", terminalTabAction("survival_route", "Survival Route")),
                Map.entry("terminal.mission_graph", terminalTabAction("mission_graph", "Route Sources")),
                Map.entry("terminal.route_records", terminalTabAction("route_records", "Route Records")),
                Map.entry("terminal.discovery_grid", terminalTabAction("discovery_grid", "Discovery Grid")),
                Map.entry("terminal.factions", terminalTabAction("faction_atlas", "Factions")),
                Map.entry("terminal.recipe_index", terminalTabAction("recipe_index", "Recipe Index")),
                Map.entry("terminal.archives", terminalTabAction("archives", "Field Archive")),
                Map.entry("terminal.vitals", terminalTabAction("vitals", "Vitals")),
                Map.entry("terminal.rewards", terminalTabAction("reward_inbox", "Reward Inbox")),
                Map.entry("terminal.data_core", terminalTabAction("data_core", "Data Core")),
                Map.entry("terminal.settings", terminalTabAction("settings", "Interface Settings")),
                Map.entry("signalos.terminal", Map.of(
                        "kind", "terminal_screen",
                        "bridgeMethod", "create",
                        "command", "open:signalos_dashboard",
                        "aliasSurface", "signalos",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screen.char_typed", Map.of(
                        "kind", "terminal_screen_input",
                        "inputType", "char_typed",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screen.mouse_scroll", Map.of(
                        "kind", "terminal_screen_input",
                        "inputType", "mouse_scroll",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screen.frame.render", Map.of(
                        "kind", "terminal_screen_frame_render",
                        "renderer", "echorendercore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.mouse", Map.of(
                        "kind", "terminal_screencore_mouse_input",
                        "inputType", "mouse",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.scroll", Map.of(
                        "kind", "terminal_screencore_scroll_input",
                        "inputType", "mouse_scroll",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.key", Map.of(
                        "kind", "terminal_screencore_key_input",
                        "inputType", "key_pressed",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.char", Map.of(
                        "kind", "terminal_screencore_char_input",
                        "inputType", "char_typed",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.action", Map.of(
                        "kind", "terminal_screencore_action",
                        "screenBridge", "echoscreencore",
                        "actionCatalog", "TerminalScreenCoreActionIds",
                        "liveSessionBridge", "echo-terminal-native-session"))));
        registry.registerRoute(
                EchoTerminal.MODID,
                "echoterminal:hud_overlay",
                "client_overlay",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoterminal.client.mission.TerminalMissionHudController",
                        "nativeScreenBridgeClass", "com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud",
                        "source", "echoterminal_native_module_route_registrar"),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry"),
                true);
        registry.registerActions(EchoTerminal.MODID, "echoterminal:hud_overlay", "client_overlay", Map.of(
                "terminal.mission_hud.tick", Map.of("kind", "terminal_overlay_tick", "overlay", "mission_hud"),
                "terminal.discovery_toast.tick", Map.of("kind", "terminal_overlay_tick", "overlay", "discovery_toast"),
                "terminal.mission_hud.render", Map.of("kind", "terminal_overlay_render", "overlay", "mission_hud"),
                "terminal.discovery_toast.render", Map.of("kind", "terminal_overlay_render", "overlay", "discovery_toast")));
        for (NativeTerminalInputBinding binding : NATIVE_TERMINAL_INPUT_BINDINGS) {
            registerInputBinding(registry, binding);
        }
        registry.registerActionHandler("terminal", "echoterminal:eui", EchoTerminalClient::dispatchNativeClientRoute);
        registry.registerActionHandler("client_overlay", "echoterminal:hud_overlay",
                EchoTerminalClient::dispatchNativeClientRoute);
        recordNativeRoute("terminal.native_route.enable", Map.of("kind", "route_enable"), true);
    }

    private static void registerInputBinding(
            EchoNativeClientRouteRegistry registry,
            NativeTerminalInputBinding binding
    ) {
        registry.registerInputBinding("terminal", binding.actionId(), Map.of(
                "keyMapping", binding.keyMapping(),
                "keyCode", binding.keyCode(),
                "inputType", "press",
                "action", binding.actionId(),
                "source", "echoterminal_native_input_route_registry",
                "nativeLoaderHostService", "key_input",
                "nativeInputOwner", "EchoNativeClientRouteRegistries"));
    }

    private static EchoNativeLoadStatus dispatchNativeInput(Object event) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        for (NativeTerminalInputBinding binding : NATIVE_TERMINAL_INPUT_BINDINGS) {
            if (EchoBackendClientBridge.keyMappingMatches(OPEN_TERMINAL_KEY, event)) {
                return registry.keyInput(
                        binding.keyMapping(),
                        binding.keyCode(),
                        "press",
                        nativeKeyMetadata(event, binding));
            }
        }
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    private static Map<String, Object> nativeKeyMetadata(Object event, NativeTerminalInputBinding binding) {
        return Map.of(
                "source", "native_loader_input_binding",
                "forwardedFrom", "native_client_bridge",
                "eventType", "key_input",
                "actionId", binding.actionId(),
                "keyMapping", binding.keyMapping(),
                "inputType", "press",
                "key", binding.keyCode(),
                "glfwAction", GLFW.GLFW_PRESS,
                "keyEvent", String.valueOf(event)
        );
    }

    private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context) {
        String kind = text(context.action().get("kind"));
        boolean handled;
        if ("terminal_overlay_tick".equals(kind)) {
            handled = switch (text(context.action().get("overlay"))) {
                case "mission_hud" -> {
                    TerminalMissionHudController.tick();
                    yield true;
                }
                case "discovery_toast" -> {
                    DiscoveryToastHud.tick();
                    yield true;
                }
                default -> false;
            };
        } else if ("terminal_overlay_render".equals(kind)) {
            Object event = NATIVE_TERMINAL_OVERLAY_RENDER.get();
            if (event != null) {
                float partialTick = EchoBackendClientBridge.guiPartialTick(event);
                GuiGraphicsExtractor graphics = EchoBackendClientBridge.guiGraphics(event);
                handled = switch (text(context.action().get("overlay"))) {
                    case "mission_hud" -> {
                        TerminalMissionHudController.render(graphics, partialTick);
                        yield true;
                    }
                    case "discovery_toast" -> {
                        DiscoveryToastHud.render(graphics, partialTick);
                        yield true;
                    }
                    default -> false;
                };
            } else {
                handled = false;
            }
        } else if ("terminal_screencore_mouse_input".equals(kind)) {
            TerminalScreenCoreScreen screen = NATIVE_SCREEN_CORE_SCREEN.get();
            handled = screen != null && screen.handleNativeRouteMouse(
                    textOrDefault(context.metadata().get("phase"), "click"),
                    doubleMetadata(context.metadata(), "mouseX"),
                    doubleMetadata(context.metadata(), "mouseY"),
                    intMetadata(context.metadata(), "button"),
                    doubleMetadata(context.metadata(), "dragX"),
                    doubleMetadata(context.metadata(), "dragY"));
        } else if ("terminal_screencore_scroll_input".equals(kind)) {
            TerminalScreenCoreScreen screen = NATIVE_SCREEN_CORE_SCREEN.get();
            handled = screen != null && screen.handleNativeRouteScroll(
                    doubleMetadata(context.metadata(), "mouseX"),
                    doubleMetadata(context.metadata(), "mouseY"),
                    doubleMetadata(context.metadata(), "scrollX"),
                    doubleMetadata(context.metadata(), "scrollY"));
        } else if ("terminal_screencore_key_input".equals(kind)) {
            TerminalScreenCoreScreen screen = NATIVE_SCREEN_CORE_SCREEN.get();
            handled = screen != null && screen.handleNativeRouteKey(
                    intMetadata(context.metadata(), "key"),
                    booleanMetadata(context.metadata(), "openTerminalKey"));
        } else if ("terminal_screencore_char_input".equals(kind)) {
            TerminalScreenCoreScreen screen = NATIVE_SCREEN_CORE_SCREEN.get();
            handled = screen != null && screen.handleNativeRouteChar(
                    text(context.metadata().get("character")),
                    booleanMetadata(context.metadata(), "allowedChatCharacter"));
        } else if ("terminal_screencore_action".equals(kind)) {
            EchoAction action = NATIVE_SCREEN_CORE_ACTION.get();
            EchoActionContext actionContext = NATIVE_SCREEN_CORE_ACTION_CONTEXT.get();
            handled = action != null && actionContext != null && action.run(actionContext);
        } else if ("terminal_screen_input".equals(kind)) {
            handled = dispatchNativeTerminalScreenInput(context);
        } else if ("terminal_tab".equals(kind)) {
            boolean screenAlreadyOpen = EchoTerminalScreens.isManagedTerminalScreen(Minecraft.getInstance().screen);
            boolean opened = openTerminalTab(
                    text(context.action().get("tabId")),
                    textOrDefault(context.action().get("label"), context.actionId()));
            EchoTerminalNativeSessionBridge.recordNativeOpen(
                    context.actionId(),
                    context.action(),
                    opened,
                    screenAlreadyOpen);
            handled = opened;
        } else if (!"terminal.open".equals(context.actionId()) && !"signalos.terminal".equals(context.actionId())) {
            handled = false;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            boolean screenAlreadyOpen = EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen);
            boolean opened = openTerminalScreen();
            EchoTerminalNativeSessionBridge.recordNativeOpen(
                    context.actionId(),
                    context.action(),
                    opened,
                    screenAlreadyOpen);
            handled = opened;
        }
        recordNativeRoute(context.actionId(), context.action(), handled, context.metadata());
        return handled;
    }

    private static boolean dispatchNativeTerminalScreenInput(NativeClientRouteActionContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof EchoTerminalScreen screen) {
            String inputType = text(context.action().get("inputType"));
            if ("char_typed".equals(inputType)) {
                String character = text(context.metadata().get("character"));
                if (character.isEmpty()) {
                    character = text(context.metadata().get("characterEvent"));
                }
                if (character.isEmpty()) {
                    return false;
                }
                int codePoint = character.codePointAt(0);
                return screen.handleCharTyped(new CharacterEvent(codePoint));
            }
            if ("mouse_scroll".equals(inputType)) {
                return screen.handleMouseScroll(
                        doubleMetadata(context.metadata(), "mouseX"),
                        doubleMetadata(context.metadata(), "mouseY"),
                        doubleMetadata(context.metadata(), "scrollY"));
            }
        }
        return EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen);
    }

    public static synchronized Map<String, Object> nativeRouteState() {
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
        Map<String, Object> nativeSession = EchoTerminalNativeSessionBridge.snapshot();
        Map<String, Object> next = new LinkedHashMap<>();
        next.put("nativeRouteEnabled", true);
        next.put("surface", "terminal");
        next.put("lastActionId", text(actionId));
        next.put("lastKind", text(action == null ? "" : action.get("kind")));
        next.put("lastResult", handled);
        next.put("overlay", text(action == null ? "" : action.get("overlay")));
        next.put("lastMetadata", Map.copyOf(safeMetadata));
        putIfPresent(next, "lastSource", safeMetadata.get("source"));
        putIfPresent(next, "lastEventType", safeMetadata.get("eventType"));
        putIfPresent(next, "lastService", safeMetadata.get("service"));
        putIfPresent(next, "lastFrameSource", safeMetadata.get("frameSource"));
        putIfPresent(next, "lastScreenClass", safeMetadata.get("screenClass"));
        putIfPresent(next, "lastPartialTick", safeMetadata.get("partialTick"));
        next.put("screenCoreScreenAttached", NATIVE_SCREEN_CORE_SCREEN.get() != null);
        next.put("screenCoreActionAttached", NATIVE_SCREEN_CORE_ACTION.get() != null
                && NATIVE_SCREEN_CORE_ACTION_CONTEXT.get() != null);
        next.put("overlayRenderContextAttached", NATIVE_TERMINAL_OVERLAY_RENDER.get() != null);
        next.put("nativeSession", nativeSession);
        next.put("routeDrivenTerminalModel", routeDrivenTerminalModel(
                actionId,
                action,
                handled,
                safeMetadata,
                nativeSession,
                Boolean.TRUE.equals(next.get("screenCoreScreenAttached")),
                Boolean.TRUE.equals(next.get("screenCoreActionAttached")),
                Boolean.TRUE.equals(next.get("overlayRenderContextAttached"))));
        nativeRouteState = Map.copyOf(next);
    }

    private static Map<String, Object> routeDrivenTerminalModel(
            String actionId,
            Map<String, Object> action,
            boolean handled,
            Map<String, Object> metadata,
            Map<String, Object> nativeSession,
            boolean screenCoreScreenAttached,
            boolean screenCoreActionAttached,
            boolean overlayRenderContextAttached
    ) {
        String safeActionId = text(actionId);
        String kind = text(action == null ? "" : action.get("kind"));
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "terminal_route");
        model.put("surface", "terminal");
        model.put("routeDrivenTerminalState", true);
        model.put("actionId", safeActionId);
        model.put("kind", kind);
        model.put("handled", handled);
        model.put("overlay", text(action == null ? "" : action.get("overlay")));
        model.put("terminalOpenRoute", safeActionId.equals("terminal.open")
                || safeActionId.equals("signalos.terminal")
                || "terminal_tab".equals(kind));
        model.put("screenInputRoute", "terminal_screen_input".equals(kind)
                || safeActionId.startsWith("terminal.screen."));
        model.put("screenCoreRoute", kind.startsWith("terminal_screencore")
                || safeActionId.startsWith("terminal.screencore."));
        model.put("overlayRoute", kind.startsWith("terminal_overlay")
                || safeActionId.startsWith("terminal.mission_hud.")
                || safeActionId.startsWith("terminal.discovery_toast."));
        model.put("screenCoreScreenAttached", screenCoreScreenAttached);
        model.put("screenCoreActionAttached", screenCoreActionAttached);
        model.put("overlayRenderContextAttached", overlayRenderContextAttached);
        model.put("nativeSession", Map.copyOf(nativeSession));
        model.put("routeMetadata", Map.copyOf(safeMetadata));
        model.put("routeMetadataKeys", safeMetadata.keySet().stream().sorted().toList());
        putIfPresent(model, "routeSource", safeMetadata.get("source"));
        putIfPresent(model, "routeEventType", safeMetadata.get("eventType"));
        putIfPresent(model, "routeService", safeMetadata.get("service"));
        putIfPresent(model, "routeFrameSource", safeMetadata.get("frameSource"));
        putIfPresent(model, "routeScreenClass", safeMetadata.get("screenClass"));
        putIfPresent(model, "routePartialTick", safeMetadata.get("partialTick"));
        putIfPresent(model, "routeInputType", action == null ? "" : action.get("inputType"));
        return Map.copyOf(model);
    }

    private static Map<String, Object> terminalTabAction(String tabPath, String label) {
        return Map.of(
                "kind", "terminal_tab",
                "tabId", EchoTerminal.MODID + ":" + tabPath,
                "label", label,
                "bridgeMethod", "openTab",
                "command", "open:" + tabPath,
                "liveSessionBridge", "echo-terminal-native-session");
    }

    private static boolean openTerminalScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen)) {
            return minecraft.player != null;
        }
        if (minecraft.screen != null) {
            return false;
        }
        if (nativeLoaderActive()) {
            minecraft.setScreen(new EchoNativeTerminalScreen());
            return true;
        }
        if (nativeLoaderActive()) {
            registerScreenCoreIntegration();
        }
        EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(
                "open",
                "terminal.open",
                "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                Map.of(
                        "targetScreenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                        "transitionSource", "terminal_route_open",
                        "screenBridge", "terminal_screencore"
                ));
        if (nativeLoaderActive()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED
                && lifecycleStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return false;
        }
        minecraft.setScreen(EchoTerminalScreens.create(
                new EchoTerminalMenu(0, minecraft.player.getInventory()),
                minecraft.player.getInventory(),
                Component.translatable("container.echoterminal.echo_terminal")));
        return true;
    }

    private static boolean openTerminalTab(String tabId, String label) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        Identifier parsed = Identifier.tryParse(tabId);
        if (parsed == null) {
            minecraft.player.sendSystemMessage(Component.literal("Terminal native route unavailable: invalid tab " + tabId));
            return false;
        }
        if (!EchoRuntimeModules.isLoaded("echoscreencore")) {
            minecraft.player.sendSystemMessage(Component.literal("Terminal native route unavailable: ScreenCore is not loaded."));
            return false;
        }
        try {
            Object opened = Class.forName("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreBridge")
                    .getMethod("openTab", Identifier.class)
                    .invoke(null, parsed);
            boolean success = opened instanceof Boolean value && value;
            if (success) {
                minecraft.player.sendSystemMessage(Component.literal("Terminal route // " + label));
            } else {
                minecraft.player.sendSystemMessage(Component.literal("Terminal native route unavailable: " + label));
            }
            return success;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoTerminal.LOGGER.warn("ECHO Terminal native route {} could not open ScreenCore tab {}.",
                    label, parsed, exception);
            minecraft.player.sendSystemMessage(Component.literal("Terminal native route unavailable: " + label));
            return false;
        }
    }

    private static boolean nativeLoaderActive() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
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
        event.put("source", "native_terminal_screen_transition");
        event.put("eventType", "terminal_screen_lifecycle");
        event.put("screenClass", screenClass == null ? "" : screenClass);
        event.put("screenTransitionPhase", phase == null ? "" : phase);
        if (metadata != null) {
            event.putAll(metadata);
        }
        event.put("nativeLoaderUiHostService", "screen_lifecycle");
        event.put("nativeLoaderUiHostSurface", "terminal");
        event.put("nativeLoaderUiHostAction", textOrDefault(actionId, "terminal.screen.lifecycle"));
        event.put("nativeLoaderScreenLifecycleHandoff", true);
        String safePhase = phase == null ? "" : phase;
        String safeActionId = textOrDefault(actionId, "terminal.screen.lifecycle");
        return switch (safePhase) {
            case "mount" -> EchoNativeClientRouteRegistries.get().mountSurface("terminal", safeActionId, Map.copyOf(event));
            case "open" -> EchoNativeClientRouteRegistries.get().openSurface("terminal", safeActionId, Map.copyOf(event));
            case "close" -> EchoNativeClientRouteRegistries.get().closeSurface("terminal", safeActionId, Map.copyOf(event));
            case "unmount" -> EchoNativeClientRouteRegistries.get().unmountSurface("terminal", safeActionId, Map.copyOf(event));
            default -> EchoNativeClientRouteRegistries.get().screenLifecycle(
                    "terminal",
                    safePhase,
                    safeActionId,
                    Map.copyOf(event));
        };
    }

    public static boolean dispatchNativeScreenCoreMouse(
            TerminalScreenCoreScreen screen,
            String phase,
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_screen_lifecycle");
        metadata.put("eventType", "terminal_screencore_mouse_input");
        metadata.put("screenClass", TerminalScreenCoreScreen.class.getName());
        metadata.put("phase", phase);
        metadata.put("mouseX", mouseX);
        metadata.put("mouseY", mouseY);
        metadata.put("button", button);
        metadata.put("dragX", dragX);
        metadata.put("dragY", dragY);
        return dispatchNativeScreenCore(screen, "terminal.screencore.mouse", metadata);
    }

    public static boolean dispatchNativeScreenCoreScroll(
            TerminalScreenCoreScreen screen,
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        return dispatchNativeScreenCore(screen, "terminal.screencore.scroll", Map.of(
                "source", "native_screen_lifecycle",
                "eventType", "terminal_screencore_scroll_input",
                "screenClass", TerminalScreenCoreScreen.class.getName(),
                "mouseX", mouseX,
                "mouseY", mouseY,
                "scrollX", scrollX,
                "scrollY", scrollY
        ));
    }

    public static boolean dispatchNativeScreenCoreKey(TerminalScreenCoreScreen screen, int key, boolean openTerminalKey) {
        return dispatchNativeScreenCore(screen, "terminal.screencore.key", Map.of(
                "source", "native_screen_lifecycle",
                "eventType", "terminal_screencore_key_input",
                "screenClass", TerminalScreenCoreScreen.class.getName(),
                "key", key,
                "openTerminalKey", openTerminalKey
        ));
    }

    public static boolean dispatchNativeScreenCoreChar(
            TerminalScreenCoreScreen screen,
            String character,
            boolean allowedChatCharacter
    ) {
        return dispatchNativeScreenCore(screen, "terminal.screencore.char", Map.of(
                "source", "native_screen_lifecycle",
                "eventType", "terminal_screencore_character_typed",
                "screenClass", TerminalScreenCoreScreen.class.getName(),
                "character", character,
                "allowedChatCharacter", allowedChatCharacter
        ));
    }

    public static boolean dispatchNativeScreenCoreAction(
            String screenCoreActionId,
            EchoActionContext actionContext,
            EchoAction action
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_screencore_action");
        metadata.put("eventType", "terminal_screencore_action");
        metadata.put("screenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreActions");
        metadata.put("actionCatalog", "TerminalScreenCoreActionIds");
        metadata.put("screenCoreActionId", textOrDefault(screenCoreActionId, "unknown"));
        if (actionContext != null) {
            putIfPresent(metadata, "pageId", actionContext.pageId());
            putIfPresent(metadata, "componentId", actionContext.componentId());
            putIfPresent(metadata, "action", actionContext.action());
            putIfPresent(metadata, "argument", actionContext.argument());
            putIfPresent(metadata, "actionValue", actionContext.actionValue());
            putIfPresent(metadata, "inputEvent", actionContext.inputEvent());
        }
        NATIVE_SCREEN_CORE_ACTION.set(action);
        NATIVE_SCREEN_CORE_ACTION_CONTEXT.set(actionContext);
        try {
            return EchoNativeClientRouteRegistries.get().dispatchStatus("terminal", "terminal.screencore.action", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREEN_CORE_ACTION.remove();
            NATIVE_SCREEN_CORE_ACTION_CONTEXT.remove();
        }
    }

    private static boolean dispatchNativeScreenCore(
            TerminalScreenCoreScreen screen,
            String actionId,
            Map<String, Object> metadata
    ) {
        NATIVE_SCREEN_CORE_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().dispatchStatus("terminal", actionId, metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREEN_CORE_SCREEN.remove();
        }
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            metadata.put(key, String.valueOf(value));
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String textOrDefault(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private static int intMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static double doubleMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0.0D;
        }
    }

    private static boolean booleanMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static void registerRenderCoreBlockRenderer(Object event) {
        if (!EchoRuntimeModules.isLoaded("echorendercore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echoterminal.integration.TerminalRenderCoreClientIntegration")
                    .getMethod("registerBlockRenderer", Object.class)
                    .invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoTerminal.LOGGER.warn("ECHO Terminal RenderCore block integration could not be registered.", exception);
        }
    }

    private record NativeTerminalInputBinding(String actionId, String keyMapping, int keyCode) {
    }

    public static class ClientModEvents {
        static void onRegisterKeyMappings(Object event) {
            EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
            EchoBackendClientBridge.registerKeyMapping(event, OPEN_TERMINAL_KEY);
        }

        static void onRegisterMenuScreens(Object event) {
            TerminalTabRegistry.ensureSorted();
            EchoBackendClientBridge.registerMenuScreenFactory(
                    event,
                    ModMenus.ECHO_TERMINAL.get(),
                    EchoTerminalScreens.class,
                    "create");
        }

        static void onRegisterRenderers(Object event) {
            registerRenderCoreBlockRenderer(event);
        }
    }
}
