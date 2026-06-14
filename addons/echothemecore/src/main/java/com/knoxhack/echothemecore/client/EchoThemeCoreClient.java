package com.knoxhack.echothemecore.client;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.client.vanilla.VanillaUiSkinLayer;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientRouteActionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EchoThemeCoreClient {
    private static final AtomicBoolean NATIVE_ROUTE_REGISTERED = new AtomicBoolean(false);
    private static final Set<String> VANILLA_UI_FAILURES = ConcurrentHashMap.newKeySet();
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, "themes"));
    public static final KeyMapping NEXT_THEME_KEY = new KeyMapping(
            "key.echothemecore.next_theme",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY);
    public static final KeyMapping PREVIOUS_THEME_KEY = new KeyMapping(
            "key.echothemecore.previous_theme",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY);
    public static final KeyMapping OPEN_THEME_PICKER_KEY = new KeyMapping(
            "key.echothemecore.open_theme_picker",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY);

    public EchoThemeCoreClient() {
        ClientThemeState.bootstrap();
        ThemeCoreScreenCoreBridge.registerIfAvailable();
        registerNativeClientRoutes();
    }

    public static List<KeyMapping> keyMappings() {
        return List.of(NEXT_THEME_KEY, PREVIOUS_THEME_KEY, OPEN_THEME_PICKER_KEY);
    }

    public static KeyMapping.Category keyMappingCategory() {
        return KEY_CATEGORY;
    }

    public static void onClientTick() {
        ClientThemeState.onClientTick();
    }

    public static void registerClientCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        ThemeCoreClientCommands.register(dispatcher);
    }

    public static void onClientLoggingOut() {
        ClientThemeState.reconcileLoadedTheme();
    }

    public static void onClientResourceLoadFinished() {
        ClientThemeState.reconcileLoadedTheme();
    }

    public static void onKeyInput(KeyEvent keyEvent, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (NEXT_THEME_KEY.matches(keyEvent)) {
            ClientThemeState.cycleTheme(1);
            messageCurrentTheme("ThemeCore next theme");
        } else if (PREVIOUS_THEME_KEY.matches(keyEvent)) {
            ClientThemeState.cycleTheme(-1);
            messageCurrentTheme("ThemeCore previous theme");
        } else if (OPEN_THEME_PICKER_KEY.matches(keyEvent)) {
            openThemePicker();
        }
    }

    private static boolean openThemePicker() {
        if (ThemeCoreScreenCoreBridge.openThemePicker()) {
            return true;
        }
        messageCurrentTheme("ThemeCore theme picker unavailable");
        return false;
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
        registerRoute(registry, "echothemecore:echo_platform_theme_surface", "theme",
                "com.knoxhack.echothemecore.client.ClientThemeState",
                Map.of(
                        "theme.apply", Map.of("kind", "theme_apply", "themeId", "echothemecore:echo_platform"),
                        "theme.reconcile", Map.of("kind", "theme_reconcile"),
                        "theme.open_picker", Map.of("kind", "theme_picker")));
        registerRoute(registry, "echothemecore:echo_platform_main_menu", "main_menu",
                "com.knoxhack.echothemecore.client.vanilla.VanillaUiSkinLayer",
                Map.of(
                        "theme.main_menu.apply", Map.of("kind", "theme_surface_apply", "surface", "main_menu"),
                        "theme.main_menu.render", Map.of("kind", "theme_surface_render", "surface", "main_menu")));
        registerRoute(registry, "echothemecore:echo_platform_loading", "loading_screen",
                "com.knoxhack.echothemecore.client.replacement.ThemeCoreLoadingOverlayRenderer",
                Map.of(
                        "theme.loading_screen.apply", Map.of("kind", "theme_surface_apply", "surface", "loading_screen"),
                        "theme.loading_screen.render", Map.of("kind", "theme_surface_render", "surface", "loading_screen")));
        registerRoute(registry, "echothemecore:echo_platform_blue_console_overlay", "client_overlay",
                "com.knoxhack.echothemecore.client.vanilla.VanillaUiSkinLayer",
                Map.of("theme.client_overlay.render", Map.of("kind", "theme_surface_render", "surface", "client_overlay")));
        registry.registerActionHandler("theme", EchoThemeCoreClient::dispatchNativeClientRoute);
        registry.registerActionHandler("main_menu", EchoThemeCoreClient::dispatchNativeClientRoute);
        registry.registerActionHandler("loading_screen", EchoThemeCoreClient::dispatchNativeClientRoute);
        registry.registerActionHandler("client_overlay", EchoThemeCoreClient::dispatchNativeClientRoute);
    }

    private static void registerRoute(
            EchoNativeClientRouteRegistry registry,
            String surfaceId,
            String surfaceType,
            String implementationClass,
            Map<String, Map<String, Object>> actions
    ) {
        registry.registerRoute(
                EchoThemeCore.MODID,
                surfaceId,
                surfaceType,
                Map.of(
                        "nativeSurfaceImplementationClass", implementationClass,
                        "nativeScreenBridgeClass", implementationClass,
                        "source", "echothemecore_client_module"),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry"),
                true);
        registry.registerActions(surfaceType, actions);
    }

    private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context) {
        if (!nativeLoaderActive()) {
            return false;
        }
        String kind = text(context.action().get("kind"));
        if ("theme_picker".equals(kind) || "theme.open_picker".equals(context.actionId())) {
            return openThemePicker();
        }
        if ("theme_apply".equals(kind)
                || "theme_reconcile".equals(kind)
                || "theme_surface_apply".equals(kind)
                || "theme_surface_render".equals(kind)
                || context.actionId().startsWith("theme.")) {
            ClientThemeState.reconcileLoadedTheme();
            return true;
        }
        return false;
    }

    private static void messageCurrentTheme(String prefix) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                    prefix + ": " + ClientThemeState.currentTheme().displayName()
                            + " (" + ClientThemeState.currentThemeId() + ")"));
        }
    }

    public static void onVanillaUiScreenBackground(Screen screen, GuiGraphicsExtractor graphics) {
        runVanillaUiGuarded("screen_background", screenName(screen),
                () -> VanillaUiSkinLayer.onScreenBackground(screen, graphics));
    }

    public static void onVanillaUiScreenRender(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        runVanillaUiGuarded("screen_render", screenName(screen),
                () -> VanillaUiSkinLayer.onScreenRender(screen, graphics, mouseX, mouseY));
    }

    public static void onVanillaUiRenderGui(GuiGraphicsExtractor graphics) {
        runVanillaUiGuarded("hud_render", "in_game_hud", () -> VanillaUiSkinLayer.onRenderGui(graphics));
    }

    public static Identifier vanillaUiTooltipTexture() {
        return VanillaUiSkinLayer.tooltipTexture();
    }

    public static boolean onVanillaUiToastAdd(Toast toast) {
        String toastName = toast == null ? "<null>" : toast.getClass().getName();
        try {
            return VanillaUiSkinLayer.onToastAdd(toast);
        } catch (RuntimeException | LinkageError exception) {
            logVanillaUiFailure("toast_add", toastName, exception);
            return false;
        }
    }

    private static void runVanillaUiGuarded(String eventName, String subject, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | LinkageError exception) {
            logVanillaUiFailure(eventName, subject, exception);
        }
    }

    private static void logVanillaUiFailure(String eventName, String subject, Throwable exception) {
        String key = eventName + ":" + subject;
        if (VANILLA_UI_FAILURES.add(key)) {
            EchoThemeCore.LOGGER.warn("ThemeCore vanilla UI {} failed for {}; preserving vanilla rendering.",
                    eventName, subject, exception);
        }
    }

    private static String screenName(Object screen) {
        return screen == null ? "<none>" : screen.getClass().getName();
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static final class ClientModEvents {
        private ClientModEvents() {
        }
    }
}
