package com.knoxhack.echoashfallprotocol;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoashfallprotocol.client.drone.DroneClientState;
import com.knoxhack.echoashfallprotocol.client.hud.HudState;
import com.knoxhack.echoashfallprotocol.client.EnvironmentalVisualController;
import com.knoxhack.echoashfallprotocol.client.hud.BossHudOverlay;
import com.knoxhack.echoashfallprotocol.client.hud.EchoNativeAshfallHudOverlay;
import com.knoxhack.echoashfallprotocol.client.hud.MutationOverlayEffect;
import com.knoxhack.echoashfallprotocol.client.hud.SurvivalHudOverlay;
import com.knoxhack.echoashfallprotocol.client.screen.CrystallineSynthesizerScreen;
import com.knoxhack.echoashfallprotocol.client.screen.DeepCoreMinerScreen;
import com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallSurfaceScreen;
import com.knoxhack.echoashfallprotocol.client.screen.EchoTerminalStyle;
import com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen;
import com.knoxhack.echoashfallprotocol.client.screen.FilterWorkbenchScreen;
import com.knoxhack.echoashfallprotocol.client.screen.HandRecyclerScreen;
import com.knoxhack.echoashfallprotocol.client.screen.IsotopeRefinerScreen;
import com.knoxhack.echoashfallprotocol.client.screen.MachineStatusScreen;
import com.knoxhack.echoashfallprotocol.client.screen.MicroGeneratorScreen;
import com.knoxhack.echoashfallprotocol.client.screen.OreGrinderScreen;
import com.knoxhack.echoashfallprotocol.client.screen.RadiationCleanserScreen;
import com.knoxhack.echoashfallprotocol.client.screen.ResearchLabScreen;
import com.knoxhack.echoashfallprotocol.client.screen.ScrapPressScreen;
import com.knoxhack.echoashfallprotocol.client.screen.ThermalArrayScreen;
import com.knoxhack.echoashfallprotocol.client.screen.ThermalBurnerScreen;
import com.knoxhack.echoashfallprotocol.client.screen.WaterPurifierScreen;
import com.knoxhack.echoashfallprotocol.client.screen.WelcomeScreen;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.integration.AshfallTerminalIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallPresenceIntegration;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeClientRouteRegistrar;
import com.knoxhack.echoashfallprotocol.network.DroneCommandPacket;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import com.knoxhack.echoterminal.client.screen.EchoTerminalNativeSessionBridge;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreenProvider;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import com.knoxhack.echoterminal.client.screen.TerminalScreenTheme;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side initialization for ECHO: ASHFALL PROTOCOL.
 */
public class EchoAshfallProtocolClient {
    /** [N] opens the welcome screen. [V] cycles HUD mode. [M] is handled by ECHO Terminal. */
    private static final int KEY_M = GLFW.GLFW_KEY_M;
    private static final int KEY_N = GLFW.GLFW_KEY_N;
    private static final int KEY_V = GLFW.GLFW_KEY_V;
    private static final int KEY_G = GLFW.GLFW_KEY_G;
    private static final int KEY_H = GLFW.GLFW_KEY_H;
    private static final int KEY_R = GLFW.GLFW_KEY_R;
    private static final int KEY_U = GLFW.GLFW_KEY_U;
    private static final int KEY_B = GLFW.GLFW_KEY_B;
    private static final int KEY_J = GLFW.GLFW_KEY_J;
    private static final int KEY_K = GLFW.GLFW_KEY_K;
    private static final int KEY_LEFT_ALT = GLFW.GLFW_KEY_LEFT_ALT;
    private static final int KEY_RIGHT_ALT = GLFW.GLFW_KEY_RIGHT_ALT;
    private static final int KEY_RIGHT_BRACKET = GLFW.GLFW_KEY_RIGHT_BRACKET;
    private static final int KEY_LEFT_BRACKET = GLFW.GLFW_KEY_LEFT_BRACKET;
    private static final int KEY_BACKSLASH = GLFW.GLFW_KEY_BACKSLASH;
    private static final AtomicBoolean LOGGED_TERMINAL_FALLBACK_OPEN = new AtomicBoolean(false);
    private static final AtomicBoolean NATIVE_MAIN_MENU_PROJECTED = new AtomicBoolean(false);
    private static final Set<String> REPORTED_NATIVE_HOTKEY_CONFLICTS = ConcurrentHashMap.newKeySet();
    private static final String NATIVE_UI_KEY_CATEGORY_ID = EchoAshfallProtocol.MODID + ":native_ui";
    private static final String DRONE_KEY_CATEGORY_ID = EchoAshfallProtocol.MODID + ":drone";
    private static final KeyMapping.Category NATIVE_UI_KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "native_ui"));
    private static final KeyMapping.Category DRONE_KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drone"));
    public static final KeyMapping NATIVE_TERMINAL_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_terminal",
            InputConstants.Type.KEYSYM,
            KEY_M,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_INDEX_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_index",
            InputConstants.Type.KEYSYM,
            KEY_G,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_INDEX_RECIPE_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_index_recipe",
            InputConstants.Type.KEYSYM,
            KEY_R,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_INDEX_USAGE_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_index_usage",
            InputConstants.Type.KEYSYM,
            KEY_U,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_INDEX_BOOKMARK_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_index_bookmark",
            InputConstants.Type.KEYSYM,
            KEY_B,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_LENS_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_lens",
            InputConstants.Type.KEYSYM,
            KEY_LEFT_ALT,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_HOLOMAP_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_holomap",
            InputConstants.Type.KEYSYM,
            KEY_J,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_HOLOMAP_MINIMAP_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_holomap_minimap",
            InputConstants.Type.KEYSYM,
            KEY_K,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_HOLOMAP_ZOOM_IN_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_holomap_zoom_in",
            InputConstants.Type.KEYSYM,
            KEY_RIGHT_BRACKET,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_HOLOMAP_ZOOM_OUT_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_holomap_zoom_out",
            InputConstants.Type.KEYSYM,
            KEY_LEFT_BRACKET,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_HOLOMAP_CYCLE_CORNER_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_holomap_cycle_corner",
            InputConstants.Type.KEYSYM,
            KEY_BACKSLASH,
            NATIVE_UI_KEY_CATEGORY);
    public static final KeyMapping NATIVE_SIGNALOS_KEY = new KeyMapping(
            "key.echoashfallprotocol.native_signalos",
            InputConstants.Type.KEYSYM,
            KEY_N,
            NATIVE_UI_KEY_CATEGORY);
    private static final NativeSurfaceKeyBinding[] NATIVE_SURFACE_KEY_BINDINGS = {
            new NativeSurfaceKeyBinding(NATIVE_TERMINAL_KEY, new NativeSurfaceKeyRoute("TERMINAL", "terminal.open")),
            new NativeSurfaceKeyBinding(NATIVE_INDEX_KEY, new NativeSurfaceKeyRoute("INDEX", "index.catalog")),
            new NativeSurfaceKeyBinding(NATIVE_INDEX_RECIPE_KEY, new NativeSurfaceKeyRoute("INDEX", "index.recipe")),
            new NativeSurfaceKeyBinding(NATIVE_INDEX_USAGE_KEY, new NativeSurfaceKeyRoute("INDEX", "index.usage")),
            new NativeSurfaceKeyBinding(NATIVE_INDEX_BOOKMARK_KEY, new NativeSurfaceKeyRoute("INDEX", "index.bookmark")),
            new NativeSurfaceKeyBinding(NATIVE_LENS_KEY, new NativeSurfaceKeyRoute("LENS", "lens.deep_scan")),
            new NativeSurfaceKeyBinding(NATIVE_HOLOMAP_KEY, new NativeSurfaceKeyRoute("HOLOMAP", "holomap.open")),
            new NativeSurfaceKeyBinding(NATIVE_HOLOMAP_MINIMAP_KEY, new NativeSurfaceKeyRoute("HOLOMAP", "holomap.toggle_minimap")),
            new NativeSurfaceKeyBinding(NATIVE_HOLOMAP_ZOOM_IN_KEY, new NativeSurfaceKeyRoute("HOLOMAP", "holomap.zoom_in")),
            new NativeSurfaceKeyBinding(NATIVE_HOLOMAP_ZOOM_OUT_KEY, new NativeSurfaceKeyRoute("HOLOMAP", "holomap.zoom_out")),
            new NativeSurfaceKeyBinding(NATIVE_HOLOMAP_CYCLE_CORNER_KEY, new NativeSurfaceKeyRoute("HOLOMAP", "holomap.cycle_corner")),
            new NativeSurfaceKeyBinding(NATIVE_SIGNALOS_KEY, new NativeSurfaceKeyRoute("SIGNALOS", "signalos.terminal"))
    };
    public static final KeyMapping DRONE_RECALL_KEY = new KeyMapping(
            "key.echoashfallprotocol.drone_recall",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            DRONE_KEY_CATEGORY);
    public static final KeyMapping DRONE_SCAN_KEY = new KeyMapping(
            "key.echoashfallprotocol.drone_scan",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            DRONE_KEY_CATEGORY);
    public static final KeyMapping DRONE_SCOUT_KEY = new KeyMapping(
            "key.echoashfallprotocol.drone_scout",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            DRONE_KEY_CATEGORY);
    public static final KeyMapping DRONE_STATUS_KEY = new KeyMapping(
            "key.echoashfallprotocol.drone_status",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            DRONE_KEY_CATEGORY);
    public static final KeyMapping DRONE_ASSIST_KEY = new KeyMapping(
            "key.echoashfallprotocol.drone_assist",
            InputConstants.Type.KEYSYM,
            KEY_H,
            DRONE_KEY_CATEGORY);

    public EchoAshfallProtocolClient() {
        bootstrapClient();
    }

    public EchoAshfallProtocolClient(Object modEventBus) {
        bootstrapClient();
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.registerModListener(
                modEventBus, ClientModEvents::onRegisterLayerDefinitions);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.registerModListener(
                modEventBus, ClientModEvents::onRegisterEntityRenderers);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.registerModListener(
                modEventBus, ClientModEvents::onRegisterKeyMappings);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.registerModListener(
                modEventBus, ClientModEvents::onRegisterMenuScreens);
    }

    public static void bootstrapClient() {
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            AshfallTerminalIntegration.registerClient();
            registerTerminalNoticeSurface();
            registerAshfallTerminalScreen();
        }
        if (EchoRuntimeModules.isLoaded("echopresencelink")) {
            try {
                AshfallPresenceIntegration.registerClient();
            } catch (RuntimeException | LinkageError exception) {
                EchoAshfallProtocol.LOGGER.debug("ECHO Presence Link Ashfall provider unavailable.", exception);
            }
        }
        if (EchoRuntimeModules.isLoaded("echorendercore")) {
            registerRenderCoreStaticSurfaces();
        }
        registerNativeClientRoutes();
    }

    public static List<KeyMapping> keyMappings() {
        return List.of(
                NATIVE_TERMINAL_KEY,
                NATIVE_INDEX_KEY,
                NATIVE_INDEX_RECIPE_KEY,
                NATIVE_INDEX_USAGE_KEY,
                NATIVE_INDEX_BOOKMARK_KEY,
                NATIVE_LENS_KEY,
                NATIVE_HOLOMAP_KEY,
                NATIVE_HOLOMAP_MINIMAP_KEY,
                NATIVE_HOLOMAP_ZOOM_IN_KEY,
                NATIVE_HOLOMAP_ZOOM_OUT_KEY,
                NATIVE_HOLOMAP_CYCLE_CORNER_KEY,
                NATIVE_SIGNALOS_KEY,
                DRONE_RECALL_KEY,
                DRONE_SCAN_KEY,
                DRONE_SCOUT_KEY,
                DRONE_STATUS_KEY,
                DRONE_ASSIST_KEY);
    }

    private static void registerRenderCoreStaticSurfaces() {
        try {
            Class.forName("com.knoxhack.echoashfallprotocol.integration.AshfallRenderCoreClientIntegration")
                    .getMethod("registerStaticSurfaces")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.warn("ECHO Ashfall RenderCore static surface integration unavailable.", exception);
        }
    }

    private static void registerTerminalNoticeSurface() {
        try {
            TerminalHudNoticeSurface.claimExternalSurface(EchoAshfallProtocol.MODID);
        } catch (LinkageError exception) {
            EchoAshfallProtocol.LOGGER.debug("Terminal HUD notice surface is not available.", exception);
        }
    }

    private static void registerAshfallTerminalScreen() {
        EchoTerminalScreens.registerFallback(new EchoTerminalScreenProvider() {
            @Override
            public AbstractContainerScreen<EchoTerminalMenu> create(EchoTerminalMenu menu, Inventory playerInventory, Component title) {
                if (LOGGED_TERMINAL_FALLBACK_OPEN.compareAndSet(false, true)) {
                    EchoAshfallProtocol.LOGGER.info("Opening ECHO Terminal Ashfall legacy fallback renderer.");
                }
                return new EchoTerminalScreen(menu, playerInventory, title, ashfallTerminalTheme());
            }

            @Override
            public boolean isTerminalScreen(Screen screen) {
                return screen instanceof EchoTerminalScreen;
            }
        });
    }

    private static TerminalScreenTheme ashfallTerminalTheme() {
        return new TerminalScreenTheme(
                "ECHO-7 ASHFALL TERMINAL",
                minecraft -> {
                    if (minecraft.player == null) {
                        return "LINK OFFLINE";
                    }
                    QuestData quest = QuestData.get(minecraft.player);
                    MissionUxSummary summary = MissionUxSummary.current(minecraft.player, quest);
                    return summary.missionId().isBlank() ? "PROTOCOL SYNC PENDING" : summary.shortTitle();
                },
                "M / ESC closes | arrows cycle tabs | up/down groups | wheel/page scrolls",
                0xEE050B10,
                0xE8050B10,
                0xD8061016,
                EchoTerminalStyle.CYAN,
                0xFF244352,
                EchoTerminalStyle.TEXT,
                EchoTerminalStyle.MUTED,
                1500,
                820);
    }

    public static void onClientTick() {
        installNativeMainMenuIfReady();
        WelcomeScreen.openPendingIfReady();
        EnvironmentalVisualController.tick();
        DroneClientState.tick();
        if (!handleNativeSurfaceKeybinds()) {
            handleDroneKeybinds();
        }
    }

    private static void installNativeMainMenuIfReady() {
        if (!nativeLoaderActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TitleScreen && NATIVE_MAIN_MENU_PROJECTED.compareAndSet(false, true)) {
            minecraft.setScreen(new EchoNativeMainMenuScreen());
        }
    }

    public static void onRenderGui(GuiGraphicsExtractor graphics, Object partialTick) {
        float dt = partialTick(partialTick);
        EnvironmentalVisualController.renderOverlay(graphics);
        if (nativeLoaderActive()) {
            if (!renderNativeHudBridge(graphics, partialTick)) {
                SurvivalHudOverlay.render(graphics, dt);
            }
        } else {
            SurvivalHudOverlay.render(graphics, dt);
        }
        MutationOverlayEffect.render(graphics, dt);
    }

    private static float partialTick(Object partialTick) {
        if (partialTick == null) {
            return 0.0F;
        }
        try {
            Object value = partialTick.getClass()
                    .getMethod("getGameTimeDeltaPartialTick", boolean.class)
                    .invoke(partialTick, true);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return 0.0F;
        }
    }

    private static boolean renderNativeHudBridge(Object graphics, Object deltaTracker) {
        try {
            Class<?> bridge = Class.forName("dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge");
            bridge.getMethod("render", Object.class, Object.class).invoke(null, graphics, deltaTracker);
            Object snapshot = bridge.getMethod("snapshot").invoke(null);
            if (snapshot instanceof Map<?, ?> state) {
                return Boolean.TRUE.equals(state.get("rendered"));
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    public static void onBossBar(Object event) {
        BossHudOverlay.onBossEvent(event);
    }

    public static void onKeyInput(KeyEvent event, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int key = event.key();
        if (nativeLoaderActive() && key == KEY_N) {
            return;
        }
        if (key == KEY_N) {
            if (mc.screen == null) {
                WelcomeScreen.openNow();
            } else if (mc.screen instanceof WelcomeScreen) {
                mc.setScreen(null);
            }
        } else if (key == KEY_V && mc.screen == null) {
            HudState.cycleMode();
        }
    }

    private static boolean openNativeSurface(Minecraft minecraft, NativeSurfaceKeyRoute route) {
        return openNativeSurface(minecraft, route, Map.of());
    }

    private static boolean openNativeSurface(Minecraft minecraft, NativeSurfaceKeyBinding binding) {
        if (binding == null) {
            return false;
        }
        return openNativeSurface(minecraft, binding.route(), binding.metadata("key_pressed"));
    }

    private static boolean openNativeSurface(
            Minecraft minecraft,
            NativeSurfaceKeyRoute route,
            Map<String, Object> hotkeyMetadata
    ) {
        if (route == null || route.surface().isBlank()) {
            return false;
        }
        registerNativeClientRoutes();
        String surfaceType = nativeSurfaceType(route.surface());
        publishNativeSurfaceLifecycle(surfaceType, "key", route.action(), "keybind", hotkeyMetadata);
        if (AshfallNativeClientRouteRegistrar.dispatch(surfaceType, route.action())) {
            Map<String, Object> routeMetadata = withOutcome(hotkeyMetadata, "route_dispatch");
            publishNativeSurfaceLifecycle(surfaceType, "open", route.action(), "route_dispatch", routeMetadata);
            publishNativeSurfaceLifecycle(surfaceType, "focus", route.action(), "route_dispatch", routeMetadata);
            return true;
        }
        if (dispatchAshfallNativeSurface(surfaceType, route.action(), Map.of())) {
            Map<String, Object> bridgeMetadata = withOutcome(hotkeyMetadata, "ashfall_direct_bridge");
            publishNativeSurfaceLifecycle(surfaceType, "open", route.action(), "ashfall_direct_bridge", bridgeMetadata);
            publishNativeSurfaceLifecycle(surfaceType, "focus", route.action(), "ashfall_direct_bridge", bridgeMetadata);
            return true;
        }
        notifyNativeSurfaceUnavailable(minecraft, route);
        Map<String, Object> unavailableMetadata = new LinkedHashMap<>(withOutcome(hotkeyMetadata, "route_missing"));
        unavailableMetadata.put("fallbackSuppressed", true);
        publishNativeSurfaceLifecycle(
                surfaceType,
                "unavailable",
                route.action(),
                "route_missing",
                Map.copyOf(unavailableMetadata));
        return true;
    }

    private static void publishNativeSurfaceLifecycle(
            String surfaceType,
            String phase,
            String action,
            String source
    ) {
        publishNativeSurfaceLifecycle(surfaceType, phase, action, source, Map.of());
    }

    private static void publishNativeSurfaceLifecycle(
            String surfaceType,
            String phase,
            String action,
            String source,
            Map<String, Object> metadata
    ) {
        Map<String, Object> eventMetadata = new LinkedHashMap<>();
        eventMetadata.put("source", source);
        eventMetadata.put("nativeClientLifecycleSdk", "echo-native-client-route-registry");
        eventMetadata.put("visibleRuntimePath", true);
        if (metadata != null) {
            eventMetadata.putAll(metadata);
        }
        AshfallNativeClientRouteRegistrar.publishLifecycleEvent(
                surfaceType,
                phase,
                action,
                Map.copyOf(eventMetadata));
    }

    private static Map<String, Object> withOutcome(Map<String, Object> metadata, String outcome) {
        Map<String, Object> updated = new LinkedHashMap<>();
        if (metadata != null) {
            updated.putAll(metadata);
        }
        updated.put("outcome", outcome);
        return Map.copyOf(updated);
    }

    private static void notifyNativeSurfaceUnavailable(Minecraft minecraft, NativeSurfaceKeyRoute route) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.sendSystemMessage(
                Component.literal(nativeSurfaceLabel(route.surface()) + " native route " + route.action()
                        + " is unavailable in this Native Loader launch; no fallback shell was opened."));
    }

    private static String nativeSurfaceLabel(String surface) {
        return switch (surface) {
            case "TERMINAL", "SIGNALOS" -> "Terminal";
            case "INDEX" -> "Index";
            case "LENS" -> "Lens";
            case "HOLOMAP" -> "HoloMap";
            default -> "Requested";
        };
    }

    private static String nativeSurfaceType(String surface) {
        return switch (surface) {
            case "TERMINAL", "SIGNALOS" -> "terminal";
            case "INDEX" -> "index";
            case "LENS" -> "lens";
            case "HOLOMAP" -> "holomap";
            default -> surface == null ? "" : surface.trim().toLowerCase();
        };
    }

    private static boolean dispatchAshfallNativeSurface(
            String surfaceType,
            String action,
            Map<String, Object> actionMetadata
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        return switch (surfaceType) {
            case "terminal" -> openTerminalSurface(minecraft, action, actionMetadata);
            case "index" -> openIndexSurface(minecraft, action, actionMetadata);
            case "lens" -> openLensSurface(minecraft, action, actionMetadata);
            case "holomap" -> openHoloMapSurface(minecraft, action, actionMetadata);
            default -> false;
        };
    }

    private static boolean openLensSurface(Minecraft minecraft, String action, Map<String, Object> actionMetadata) {
        if (!EchoRuntimeModules.isLoaded("echolens")) {
            return false;
        }
        boolean scanRequested = requestLensDeepScan();
        if (lensHasVisibleTarget()) {
            recordLensNativeRoute(action, actionMetadata, scanRequested, "target_visible");
            return true;
        }
        if (scanRequested && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(
                    "Lens deep scan requested; no visible target report is mounted yet."));
        }
        minecraft.setScreen(new EchoNativeAshfallSurfaceScreen("LENS"));
        recordLensNativeRoute(action, actionMetadata, scanRequested, scanRequested ? "fallback_surface_opened" : "unavailable");
        return true;
    }

    private static void recordLensNativeRoute(
            String action,
            Map<String, Object> actionMetadata,
            boolean requested,
        String outcome
    ) {
        try {
            Class.forName("com.knoxhack.echolens.client.LensHudOverlay")
                    .getMethod("recordNativeRoute", String.class, Map.class, boolean.class, String.class)
                    .invoke(null, action, actionMetadata, requested, outcome);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.debug("ECHO Ashfall Lens native session bridge unavailable.", exception);
        }
    }

    private static boolean openIndexSurface(
            Minecraft minecraft,
            String action,
            Map<String, Object> actionMetadata
    ) {
        String kind = text(actionMetadata.get("kind"));
        if ("screen_bridge".equals(kind)) {
            boolean opened = openNoArgScreen(
                    minecraft,
                    "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                    textOrDefault(actionMetadata.get("bridgeMethod"), "open"),
                    "com.knoxhack.echoindex.client.IndexCatalogScreen");
            recordIndexNativeRoute(action, actionMetadata, opened, opened ? "screen_bridge_opened" : "unavailable");
            return opened;
        }
        if ("item_recipe".equals(kind)) {
            String mode = textOrDefault(actionMetadata.get("recipeMode"), "recipes");
            boolean recipes = !"usages".equals(mode);
            boolean opened = openHeldItemIndexRecipe(minecraft, recipes)
                    || openIndexScreenCoreMode(mode)
                    || openLegacyNoArgScreen(minecraft, "com.knoxhack.echoindex.client.IndexCatalogScreen");
            recordIndexNativeRoute(
                    action,
                    actionMetadata,
                    opened,
                    opened ? (recipes ? "recipes_opened" : "usages_opened") : "unavailable");
            return opened;
        }
        if ("screen_core_mode".equals(kind)) {
            String mode = textOrDefault(actionMetadata.get("mode"), "favorites");
            boolean opened = openIndexScreenCoreMode(mode)
                    || openLegacyNoArgScreen(minecraft, "com.knoxhack.echoindex.client.IndexCatalogScreen");
            recordIndexNativeRoute(action, actionMetadata, opened, opened ? "mode_opened:" + mode : "unavailable");
            return opened;
        }
        boolean opened = switch (action) {
            case "index.catalog" -> openNoArgScreen(
                    minecraft,
                    "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                    "open",
                    "com.knoxhack.echoindex.client.IndexCatalogScreen");
            case "index.recipe" -> openHeldItemIndexRecipe(minecraft, true)
                    || openIndexScreenCoreMode("recipes")
                    || openLegacyNoArgScreen(minecraft, "com.knoxhack.echoindex.client.IndexCatalogScreen");
            case "index.usage" -> openHeldItemIndexRecipe(minecraft, false)
                    || openIndexScreenCoreMode("usages")
                    || openLegacyNoArgScreen(minecraft, "com.knoxhack.echoindex.client.IndexCatalogScreen");
            case "index.bookmark" -> openIndexScreenCoreMode("favorites")
                    || openLegacyNoArgScreen(minecraft, "com.knoxhack.echoindex.client.IndexCatalogScreen");
            default -> false;
        };
        recordIndexNativeRoute(action, actionMetadata, opened, opened ? "opened" : "unavailable");
        return opened;
    }

    private static void recordIndexNativeRoute(
            String action,
            Map<String, Object> actionMetadata,
            boolean opened,
            String outcome
    ) {
        try {
            Class.forName("com.knoxhack.echoindex.client.IndexNativeSessionBridge")
                    .getMethod("recordNativeRoute", String.class, Map.class, boolean.class, String.class)
                    .invoke(null, action, actionMetadata, opened, outcome);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            EchoAshfallProtocol.LOGGER.debug("ECHO Ashfall Index native session bridge unavailable.", exception);
        }
    }

    private static boolean openHoloMapSurface(
            Minecraft minecraft,
            String action,
            Map<String, Object> actionMetadata
    ) {
        String kind = text(actionMetadata.get("kind"));
        if ("screen_bridge".equals(kind)) {
            return openNoArgScreen(
                    minecraft,
                    "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                    textOrDefault(actionMetadata.get("bridgeMethod"), "openFullscreen"),
                    "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen");
        }
        if ("overlay_command".equals(kind)) {
            String method = textOrDefault(actionMetadata.get("bridgeMethod"), "");
            return invokeHoloMapOverlayCommand(method, method);
        }
        return switch (action) {
            case "holomap.open" -> openNoArgScreen(
                    minecraft,
                    "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                    "openFullscreen",
                    "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen");
            case "holomap.toggle_minimap" -> invokeHoloMapOverlayCommand("toggle", "toggle");
            case "holomap.zoom_in" -> invokeHoloMapOverlayCommand("zoomIn", "zoom in");
            case "holomap.zoom_out" -> invokeHoloMapOverlayCommand("zoomOut", "zoom out");
            case "holomap.cycle_corner" -> invokeHoloMapOverlayCommand("cycleCorner", "corner");
            default -> false;
        };
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String textOrDefault(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
    }

    private static boolean openTerminalSurface(
            Minecraft minecraft,
            String action,
            Map<String, Object> actionMetadata
    ) {
        if (!EchoRuntimeModules.isLoaded("echoterminal") || minecraft.player == null) {
            return false;
        }
        try {
            boolean screenAlreadyOpen = EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen);
            minecraft.setScreen(EchoTerminalScreens.create(
                    new EchoTerminalMenu(0, minecraft.player.getInventory()),
                    minecraft.player.getInventory(),
                    Component.translatable("container.echoterminal.echo_terminal")));
            EchoTerminalNativeSessionBridge.recordNativeOpen(
                    action,
                    actionMetadata,
                    true,
                    screenAlreadyOpen);
            return true;
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean openHeldItemIndexRecipe(Minecraft minecraft, boolean recipes) {
        if (minecraft.player == null || minecraft.player.getMainHandItem().isEmpty()) {
            return false;
        }
        try {
            Class<?> screenClass = Class.forName("com.knoxhack.echoindex.client.IndexRecipeScreen");
            Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> modeClass = Class.forName("com.knoxhack.echoindex.client.IndexRecipeScreen$Mode");
            Object mode = Enum.valueOf((Class) modeClass.asSubclass(Enum.class), recipes ? "RECIPES" : "USES");
            Object screen = screenClass.getConstructor(itemStackClass, modeClass)
                    .newInstance(minecraft.player.getMainHandItem(), mode);
            if (screen instanceof Screen nativeScreen) {
                minecraft.setScreen(nativeScreen);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
        return false;
    }

    private static boolean lensHasVisibleTarget() {
        try {
            Object stack = Class.forName("com.knoxhack.echolens.client.LensHudOverlay")
                    .getMethod("currentTargetStack")
                    .invoke(null);
            return stack instanceof ItemStack itemStack && !itemStack.isEmpty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean requestLensDeepScan() {
        try {
            Object result = Class.forName("com.knoxhack.echolens.client.LensHudOverlay")
                    .getMethod("requestDeepScan")
                    .invoke(null);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean openIndexScreenCoreMode(String mode) {
        return invokeBooleanBridge("com.knoxhack.echoindex.client.IndexScreenCoreBridge", "openMode", String.class, mode);
    }

    private static boolean openNoArgScreen(
            Minecraft minecraft,
            String bridgeClassName,
            String bridgeMethodName,
            String screenClassName
    ) {
        if (invokeBooleanBridge(bridgeClassName, bridgeMethodName)) {
            return true;
        }
        try {
            Object screen = Class.forName(screenClassName).getConstructor().newInstance();
            if (screen instanceof Screen nativeScreen) {
                minecraft.setScreen(nativeScreen);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
        return false;
    }

    private static boolean openLegacyNoArgScreen(Minecraft minecraft, String screenClassName) {
        try {
            Object screen = Class.forName(screenClassName).getConstructor().newInstance();
            if (screen instanceof Screen nativeScreen) {
                minecraft.setScreen(nativeScreen);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
        return false;
    }

    private static boolean invokeBooleanBridge(String className, String methodName) {
        try {
            Object result = Class.forName(className).getMethod(methodName).invoke(null);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean invokeBooleanBridge(String className, String methodName, Class<?> parameterType, Object argument) {
        try {
            Object result = Class.forName(className).getMethod(methodName, parameterType).invoke(null, argument);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean invokeVoidBridge(String className, String methodName) {
        try {
            Class.forName(className).getMethod(methodName).invoke(null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean invokeHoloMapOverlayCommand(String methodName, String actionLabel) {
        if (!invokeVoidBridge("com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay", methodName)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(
                    "HoloMap " + actionLabel + " // " + holoMapOverlayStatusLine()));
        }
        return true;
    }

    private static String holoMapOverlayStatusLine() {
        try {
            Object result = Class.forName("com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay")
                    .getMethod("nativeOverlayStatusLine")
                    .invoke(null);
            String text = result == null ? "" : String.valueOf(result);
            return text.isBlank() ? "minimap state unavailable" : text;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return "minimap state unavailable";
        }
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    public static boolean ensureNativeClientRoutesRegisteredForNativeLoader() {
        registerNativeClientRoutes();
        for (NativeSurfaceKeyBinding binding : NATIVE_SURFACE_KEY_BINDINGS) {
            registerNativeInputBinding(binding);
        }
        return nativeLoaderActive();
    }

    private static void registerNativeClientRoutes() {
        AshfallNativeClientRouteRegistrar.register(
                nativeLoaderActive(),
                EchoAshfallProtocolClient::dispatchAshfallNativeSurface);
    }

    private record NativeSurfaceKeyRoute(String surface, String action) {
        private static final NativeSurfaceKeyRoute NONE = new NativeSurfaceKeyRoute("", "");
    }

    private record NativeSurfaceKeyBinding(KeyMapping keyMapping, NativeSurfaceKeyRoute route) {
        private Map<String, Object> metadata(String outcome) {
            return Map.ofEntries(
                    Map.entry("inputLifecycleSdk", "echo-native-client-keybinding"),
                    Map.entry("keyMapping", keyMapping.getName()),
                    Map.entry("keyCategory", NATIVE_UI_KEY_CATEGORY_ID),
                    Map.entry("defaultKeyName", defaultKeyName()),
                    Map.entry("defaultKeyCode", defaultKeyCode()),
                    Map.entry("surface", route.surface()),
                    Map.entry("action", route.action()),
                    Map.entry("outcome", outcome));
        }

        private String defaultKeyName() {
            return switch (route.action()) {
                case "terminal.open" -> "M";
                case "index.catalog" -> "G";
                case "index.recipe" -> "R";
                case "index.usage" -> "U";
                case "index.bookmark" -> "B";
                case "lens.deep_scan" -> "Left Alt";
                case "holomap.open" -> "J";
                case "holomap.toggle_minimap" -> "K";
                case "holomap.zoom_in" -> "]";
                case "holomap.zoom_out" -> "[";
                case "holomap.cycle_corner" -> "\\";
                case "signalos.terminal" -> "N";
                default -> "";
            };
        }

        private int defaultKeyCode() {
            return switch (route.action()) {
                case "terminal.open" -> KEY_M;
                case "index.catalog" -> KEY_G;
                case "index.recipe" -> KEY_R;
                case "index.usage" -> KEY_U;
                case "index.bookmark" -> KEY_B;
                case "lens.deep_scan" -> KEY_LEFT_ALT;
                case "holomap.open" -> KEY_J;
                case "holomap.toggle_minimap" -> KEY_K;
                case "holomap.zoom_in" -> KEY_RIGHT_BRACKET;
                case "holomap.zoom_out" -> KEY_LEFT_BRACKET;
                case "holomap.cycle_corner" -> KEY_BACKSLASH;
                case "signalos.terminal" -> KEY_N;
                default -> InputConstants.UNKNOWN.getValue();
            };
        }
    }

    private static boolean handleNativeSurfaceKeybinds() {
        if (!nativeLoaderActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return false;
        }
        for (NativeSurfaceKeyBinding binding : NATIVE_SURFACE_KEY_BINDINGS) {
            while (binding.keyMapping().consumeClick()) {
                if (openNativeSurface(mc, binding)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void handleDroneKeybinds() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (DRONE_RECALL_KEY.consumeClick()) {
            sendDroneCommand("recall");
        }
        while (DRONE_SCAN_KEY.consumeClick()) {
            sendDroneCommand("scan_area");
        }
        while (DRONE_SCOUT_KEY.consumeClick()) {
            sendDroneCommand("scout_ahead");
        }
        while (DRONE_STATUS_KEY.consumeClick()) {
            sendDroneCommand("status");
        }
        while (DRONE_ASSIST_KEY.consumeClick()) {
            sendDroneCommand("toggle_assist");
        }
    }

    private static void sendDroneCommand(String command) {
        EchoNetClientActions.sendServerboundAction(new DroneCommandPacket(command));
    }

    private static void auditNativeHotkeyConflicts() {
        Map<Integer, String> owners = new LinkedHashMap<>();
        boolean conflict = false;
        for (NativeSurfaceKeyBinding binding : NATIVE_SURFACE_KEY_BINDINGS) {
            conflict |= registerHotkeyOwner(
                    owners,
                    binding.defaultKeyCode(),
                    binding.defaultKeyName(),
                    NATIVE_UI_KEY_CATEGORY_ID,
                    binding.keyMapping().getName(),
                    binding.route().action());
        }
        conflict |= registerHotkeyOwner(
                owners, GLFW.GLFW_KEY_X, "X", DRONE_KEY_CATEGORY_ID,
                DRONE_RECALL_KEY.getName(), "ashfall.drone_recall");
        conflict |= registerHotkeyOwner(
                owners, GLFW.GLFW_KEY_C, "C", DRONE_KEY_CATEGORY_ID,
                DRONE_SCAN_KEY.getName(), "ashfall.drone_scan");
        conflict |= registerHotkeyOwner(
                owners, GLFW.GLFW_KEY_Y, "Y", DRONE_KEY_CATEGORY_ID,
                DRONE_SCOUT_KEY.getName(), "ashfall.drone_scout");
        conflict |= registerHotkeyOwner(
                owners, GLFW.GLFW_KEY_Z, "Z", DRONE_KEY_CATEGORY_ID,
                DRONE_STATUS_KEY.getName(), "ashfall.drone_status");
        conflict |= registerHotkeyOwner(
                owners, KEY_H, "H", DRONE_KEY_CATEGORY_ID,
                DRONE_ASSIST_KEY.getName(), "ashfall.drone_assist");
        if (!conflict) {
            publishNativeSurfaceLifecycle(
                    "HOTKEYS",
                    "input_audit",
                    "keybindings.register",
                    "keymapping_registration",
                    Map.of(
                            "inputLifecycleSdk", "echo-native-client-keybinding",
                            "outcome", "no_conflicts",
                            "nativeBindingCount", NATIVE_SURFACE_KEY_BINDINGS.length,
                            "droneBindingCount", 5));
        }
    }

    private static void registerNativeInputBinding(NativeSurfaceKeyBinding binding) {
        if (binding == null || binding.route() == null) {
            return;
        }
        String surfaceType = nativeSurfaceType(binding.route().surface());
        Map<String, Object> metadata = new LinkedHashMap<>(binding.metadata("registered"));
        metadata.put("nativeInputBindingRegistry", "echo-native-client-route-registry");
        metadata.put("visibleKeybindCategory", NATIVE_UI_KEY_CATEGORY_ID);
        metadata.put("deterministicDefaultBinding", true);
        metadata.put("conflictAudited", true);
        AshfallNativeClientRouteRegistrar.registerInputBinding(
                surfaceType,
                binding.route().action(),
                Map.copyOf(metadata));
        publishNativeSurfaceLifecycle(
                surfaceType,
                "bind",
                binding.route().action(),
                "keymapping_registration",
                Map.copyOf(metadata));
    }

    private static boolean registerHotkeyOwner(
            Map<Integer, String> owners,
            int keyCode,
            String keyName,
            String categoryId,
            String keyMapping,
            String action
    ) {
        String owner = categoryId + "/" + keyMapping + "/" + action;
        String previous = owners.putIfAbsent(keyCode, owner);
        if (previous == null) {
            return false;
        }
        String conflictKey = keyCode + ":" + previous + ":" + owner;
        if (!REPORTED_NATIVE_HOTKEY_CONFLICTS.add(conflictKey)) {
            return true;
        }
        EchoAshfallProtocol.LOGGER.warn(
                "ECHO Ashfall native hotkey conflict on {}: {} overlaps {}.",
                keyName,
                owner,
                previous);
        publishNativeSurfaceLifecycle(
                "HOTKEYS",
                "conflict",
                "keybindings.register",
                "keymapping_registration",
                Map.ofEntries(
                        Map.entry("inputLifecycleSdk", "echo-native-client-keybinding"),
                        Map.entry("outcome", "conflict"),
                        Map.entry("keyName", keyName),
                        Map.entry("keyCode", keyCode),
                        Map.entry("owner", owner),
                        Map.entry("previousOwner", previous)));
        return true;
    }

    public static class ClientModEvents {
        static void onClientSetup() {
            EchoAshfallProtocol.LOGGER.info("ECHO: ASHFALL PROTOCOL - Client systems online");
            bootstrapClient();
        }

        static void onRegisterLayerDefinitions(Object event) {
            // Shared ECHO mob model layers are registered by echocore.
        }

        static void onRegisterEntityRenderers(Object event) {
            if (EchoRuntimeModules.isLoaded("echorendercore") && registerRenderCoreEntityRenderers(event)) {
                return;
            }
            registerFallbackEntityRenderers(event);
        }

        private static void registerFallbackEntityRenderers(Object event) {
            registerEntityRenderer(event, ModEntities.RAD_ZOMBIE.get(), renderer("rad_zombie", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.SCAVENGER_BANDIT.get(), renderer("scavenger_bandit", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.IRRADIATED_WOLF.get(), renderer("irradiated_wolf", EchoMobFamily.QUADRUPED, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.ECHO_DRONE.get(), renderer("echo_drone", EchoMobFamily.DRONE, 1.0F, 0.4F));
            registerEntityRenderer(event, ModEntities.SCOUT_DRONE.get(), renderer("scout_drone", EchoMobFamily.DRONE, 1.0F, 0.4F));
            registerEntityRenderer(event, ModEntities.GLOWING_GHOUL.get(), renderer("glowing_ghoul", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.ASH_WRAITH.get(), renderer("ash_wraith", EchoMobFamily.WRAITH, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.TOXIC_SLIME.get(), renderer("toxic_slime", EchoMobFamily.SLIME, 1.0F, 0.35F));
            registerEntityRenderer(event, ModEntities.GRIDBOUND_HUSK.get(), renderer("gridbound_husk", EchoMobFamily.HUMANOID, 1.0F, 0.55F));
            registerEntityRenderer(event, ModEntities.RELAY_WARDEN.get(), renderer("relay_warden", EchoMobFamily.HEAVY_BOSS, 1.0F, 0.85F));
            registerEntityRenderer(event, ModEntities.SIGNAL_LEECH.get(), renderer("signal_leech", EchoMobFamily.CRAWLER, 1.0F, 0.35F));
            registerEntityRenderer(event, ModEntities.NEXUS_NULLIFIER.get(), renderer("nexus_nullifier", EchoMobFamily.HUMANOID, 1.0F, 0.55F));
            registerEntityRenderer(event, ModEntities.CITY_STALKER.get(), renderer("city_stalker", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.RUST_WALKER.get(), renderer("rust_walker", EchoMobFamily.HEAVY_BOSS, 1.0F, 0.7F));
            registerEntityRenderer(event, ModEntities.STEAM_WRAITH.get(), renderer("steam_wraith", EchoMobFamily.WRAITH, 1.0F, 0.4F));
            registerEntityRenderer(event, ModEntities.MUTATED_CRAWLER.get(), renderer("mutated_crawler", EchoMobFamily.CRAWLER, 1.0F, 0.3F));
            registerEntityRenderer(event, ModEntities.ECHO_COMPANION_DRONE.get(), renderer("echo_companion_drone", EchoMobFamily.DRONE, 1.0F, 0.4F));
            registerEntityRenderer(event, ModEntities.WILD_DOG.get(), renderer("wild_dog", EchoMobFamily.QUADRUPED, 1.0F, 0.45F));
            registerEntityRenderer(event, ModEntities.FERAL_HUMAN.get(), renderer("feral_human", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.CRASH_SURVIVOR.get(), renderer("crash_survivor", EchoMobFamily.SURVIVOR_NPC, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.FACTION_NPC.get(), renderer("faction_npc", EchoMobFamily.SURVIVOR_NPC, 1.0F, 0.5F));
            registerEntityRenderer(event, ModEntities.WARDEN_BOSS.get(), renderer("warden_boss", EchoMobFamily.HEAVY_BOSS, 1.0F, 1.0F));
            registerEntityRenderer(event, ModEntities.WASTELAND_SENTINEL.get(), renderer("wasteland_sentinel", EchoMobFamily.HEAVY_BOSS, 1.0F, 0.9F));
            registerEntityRenderer(event, ModEntities.CRASH_ZONE_COLOSSUS.get(), renderer("crash_zone_colossus", EchoMobFamily.HEAVY_BOSS, 1.24F, 1.12F));
            registerEntityRenderer(event, ModEntities.CRYOGENIC_OVERSEER.get(), renderer("cryogenic_overseer", EchoMobFamily.HEAVY_BOSS, 1.04F, 0.9F));
            registerEntityRenderer(event, ModEntities.INDUSTRIAL_JUGGERNAUT.get(), renderer("industrial_juggernaut", EchoMobFamily.HEAVY_BOSS, 1.16F, 1.04F));
            registerEntityRenderer(event, ModEntities.NEXUS_SCAR_AVATAR.get(), renderer("nexus_scar_avatar", EchoMobFamily.HEAVY_BOSS, 1.18F, 1.08F));
            registerEntityRenderer(event, ModEntities.RADIATION_BEHEMOTH.get(), renderer("radiation_behemoth", EchoMobFamily.HEAVY_BOSS, 1.12F, 1.0F));
            registerEntityRenderer(event, ModEntities.CITY_RUIN_STALKER.get(), renderer("city_ruin_stalker", EchoMobFamily.HEAVY_BOSS, 0.92F, 0.68F));
            registerEntityRenderer(event, ModEntities.PLAINS_WARLORD.get(), renderer("plains_warlord", EchoMobFamily.HEAVY_BOSS, 1.02F, 0.88F));
            registerEntityRenderer(event, ModEntities.TOXIC_HIVE_MATRIARCH.get(), renderer("toxic_hive_matriarch", EchoMobFamily.HEAVY_BOSS, 1.05F, 0.92F));
            registerEntityRenderer(event, ModEntities.CORRUPTION_BLOOM.get(), renderer("corruption_bloom", EchoMobFamily.HEAVY_BOSS, 1.04F, 0.86F));
            registerEntityRenderer(event, ModEntities.SEVERANCE_ENGINE.get(), renderer("severance_engine", EchoMobFamily.HEAVY_BOSS, 1.14F, 0.86F));
            registerEntityRenderer(event, ModEntities.MIRROR_COMMAND.get(), renderer("mirror_command", EchoMobFamily.HEAVY_BOSS, 1.08F, 0.86F));
        }

        private static void registerEntityRenderer(Object event, Object entityType, EntityRendererProvider<?> provider) {
            if (event == null || entityType == null || provider == null) {
                return;
            }
            try {
                for (java.lang.reflect.Method method : event.getClass().getMethods()) {
                    if ("registerEntityRenderer".equals(method.getName()) && method.getParameterCount() == 2) {
                        method.invoke(event, entityType, provider);
                        return;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                EchoAshfallProtocol.LOGGER.warn("ECHO Ashfall entity renderer registration skipped for {}.", entityType, exception);
            }
        }

        private static boolean registerRenderCoreEntityRenderers(Object event) {
            try {
                Class.forName("com.knoxhack.echoashfallprotocol.integration.AshfallRenderCoreClientIntegration")
                        .getMethod("registerEntityRenderers", Object.class)
                        .invoke(null, event);
                return true;
            } catch (ReflectiveOperationException | LinkageError exception) {
                EchoAshfallProtocol.LOGGER.warn("ECHO Ashfall RenderCore entity renderer integration unavailable; using generated fallback renderers.", exception);
                return false;
            }
        }

        private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
                float scale, float shadow) {
            return context -> new EchoMobFamilyRenderer<>(context, EchoAshfallProtocol.MODID, entityName, family, scale, shadow);
        }

        static void onRegisterKeyMappings(Object event) {
            invokeEvent(event, "registerCategory", NATIVE_UI_KEY_CATEGORY);
            for (NativeSurfaceKeyBinding binding : NATIVE_SURFACE_KEY_BINDINGS) {
                invokeEvent(event, "register", binding.keyMapping());
                registerNativeInputBinding(binding);
            }
            invokeEvent(event, "registerCategory", DRONE_KEY_CATEGORY);
            for (KeyMapping keyMapping : List.of(
                    DRONE_RECALL_KEY,
                    DRONE_SCAN_KEY,
                    DRONE_SCOUT_KEY,
                    DRONE_STATUS_KEY,
                    DRONE_ASSIST_KEY)) {
                invokeEvent(event, "register", keyMapping);
            }
            EchoAshfallProtocolClient.auditNativeHotkeyConflicts();
        }

        static void onRegisterMenuScreens(Object event) {
            registerMenuScreen(event, ModMenuTypes.RESEARCH_LAB.get(), ResearchLabScreen.class);
            registerMenuScreen(event, ModMenuTypes.HAND_RECYCLER.get(), HandRecyclerScreen.class);
            registerMenuScreen(event, ModMenuTypes.THERMAL_BURNER.get(), ThermalBurnerScreen.class);
            registerMenuScreen(event, ModMenuTypes.WATER_PURIFIER.get(), WaterPurifierScreen.class);
            registerMenuScreen(event, ModMenuTypes.MICRO_GENERATOR.get(), MicroGeneratorScreen.class);
            registerMenuScreen(event, ModMenuTypes.FILTER_WORKBENCH.get(), FilterWorkbenchScreen.class);
            registerMenuScreen(event, ModMenuTypes.SCRAP_PRESS.get(), ScrapPressScreen.class);
            registerMenuScreen(event, ModMenuTypes.MACHINE_STATUS.get(), MachineStatusScreen.class);
            registerMenuScreen(event, ModMenuTypes.THERMAL_ARRAY.get(), ThermalArrayScreen.class);
            registerMenuScreen(event, ModMenuTypes.ORE_GRINDER.get(), OreGrinderScreen.class);
            registerMenuScreen(event, ModMenuTypes.ISOTOPE_REFINER.get(), IsotopeRefinerScreen.class);
            registerMenuScreen(event, ModMenuTypes.CRYSTALLINE_SYNTHESIZER.get(), CrystallineSynthesizerScreen.class);
            registerMenuScreen(event, ModMenuTypes.DEEP_CORE_MINER.get(), DeepCoreMinerScreen.class);
            registerMenuScreen(event, ModMenuTypes.RADIATION_CLEANSER.get(), RadiationCleanserScreen.class);
        }

        private static void registerMenuScreen(Object event, Object menuType, Class<?> screenClass) {
            if (event == null || menuType == null || screenClass == null) {
                return;
            }
            try {
                for (java.lang.reflect.Method method : event.getClass().getMethods()) {
                    if (!"register".equals(method.getName()) || method.getParameterCount() != 2) {
                        continue;
                    }
                    Class<?> constructorType = method.getParameterTypes()[1];
                    Object constructor = Proxy.newProxyInstance(
                            constructorType.getClassLoader(),
                            new Class<?>[] { constructorType },
                            (proxy, invoked, args) -> {
                                if (invoked.getDeclaringClass() == Object.class) {
                                    return switch (invoked.getName()) {
                                        case "toString" -> "AshfallScreenConstructor[" + screenClass.getName() + "]";
                                        case "hashCode" -> System.identityHashCode(proxy);
                                        case "equals" -> proxy == (args == null ? null : args[0]);
                                        default -> null;
                                    };
                                }
                                for (java.lang.reflect.Constructor<?> ctor : screenClass.getConstructors()) {
                                    if (ctor.getParameterCount() == 3) {
                                        return ctor.newInstance(args);
                                    }
                                }
                                throw new IllegalStateException("No three-argument screen constructor for "
                                        + screenClass.getName());
                            });
                    method.invoke(event, menuType, constructor);
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                EchoAshfallProtocol.LOGGER.warn("ECHO Ashfall menu screen registration skipped for {}.",
                        screenClass.getName(), exception);
            }
        }

        private static void invokeEvent(Object event, String methodName, Object argument) {
            if (event == null || argument == null) {
                return;
            }
            try {
                for (java.lang.reflect.Method method : event.getClass().getMethods()) {
                    if (methodName.equals(method.getName()) && method.getParameterCount() == 1) {
                        method.invoke(event, argument);
                        return;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                EchoAshfallProtocol.LOGGER.warn("ECHO Ashfall client event adapter {} skipped for {}.",
                        methodName, argument, exception);
            }
        }
    }
}
