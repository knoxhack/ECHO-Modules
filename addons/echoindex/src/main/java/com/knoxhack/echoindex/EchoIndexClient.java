package com.knoxhack.echoindex;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoindex.client.IndexCatalogScreen;
import com.knoxhack.echoindex.client.IndexHotkeys;
import com.knoxhack.echoindex.client.IndexNativeSessionBridge;
import com.knoxhack.echoindex.client.IndexOverlay;
import com.knoxhack.echoindex.client.IndexRecipeScreen;
import com.knoxhack.echoindex.client.IndexTooltipComponents;
import com.knoxhack.echoindex.content.IndexSourceReloadListener;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class EchoIndexClient {
    private static final AtomicBoolean NATIVE_ROUTE_REGISTERED = new AtomicBoolean(false);
    private static final ThreadLocal<IndexCatalogScreen> NATIVE_CATALOG_SCREEN = new ThreadLocal<>();
    private static final ThreadLocal<IndexRecipeScreen> NATIVE_RECIPE_SCREEN = new ThreadLocal<>();
    private static final ThreadLocal<NativeIndexHotkeyScreenContext> NATIVE_INDEX_HOTKEY_SCREEN = new ThreadLocal<>();
    private static final ThreadLocal<Object> NATIVE_INDEX_OVERLAY_RENDER = new ThreadLocal<>();
    private static final ThreadLocal<EchoAction> NATIVE_SCREEN_CORE_ACTION = new ThreadLocal<>();
    private static final ThreadLocal<EchoActionContext> NATIVE_SCREEN_CORE_ACTION_CONTEXT = new ThreadLocal<>();
    private static final KeyMapping.Category KEY_CATEGORY =
            registerKeyCategory("index");
    public static final KeyMapping SHOW_RECIPE_KEY = new KeyMapping(
            "key.echoindex.recipe",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY);
    public static final KeyMapping SHOW_USAGE_KEY = new KeyMapping(
            "key.echoindex.usage",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            KEY_CATEGORY);
    public static final KeyMapping BOOKMARK_KEY = new KeyMapping(
            "key.echoindex.bookmark",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY);
    public static final KeyMapping OPEN_INDEX_KEY = new KeyMapping(
            "key.echoindex.catalog",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY);
    private static final List<NativeIndexInputBinding> NATIVE_INDEX_INPUT_BINDINGS = List.of(
            new NativeIndexInputBinding("index.catalog", "key.echoindex.catalog", GLFW.GLFW_KEY_G),
            new NativeIndexInputBinding("index.recipe", "key.echoindex.recipe", GLFW.GLFW_KEY_R),
            new NativeIndexInputBinding("index.usage", "key.echoindex.usage", GLFW.GLFW_KEY_U),
            new NativeIndexInputBinding("index.bookmark", "key.echoindex.bookmark", GLFW.GLFW_KEY_B));

    public EchoIndexClient() {
        this(null);
    }

    public EchoIndexClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexHotkeyScreenRendered);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexHotkeyKeyPressed);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayRender);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayMouseClicked);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayMouseDragged);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayMouseReleased);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayMouseScrolled);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayKeyPressed);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onIndexOverlayCharTyped);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onClientLoggingIn);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onClientLoggingOut);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoIndexClient::onClientResourceLoadFinished);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterKeyMappings);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterClientTooltipComponents);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onAddClientReloadListeners);
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalClientIntegration();
        }
        if (EchoRuntimeModules.isLoaded("echoscreencore")) {
            registerScreenCoreIntegration();
        }
        registerNativeClientRoutes();
    }

    private static void onIndexHotkeyScreenRendered(Object event) {
        if (nativeLoaderActive()) {
            NATIVE_INDEX_HOTKEY_SCREEN.set(new NativeIndexHotkeyScreenContext(
                    EchoBackendClientBridge.screen(event),
                    EchoBackendClientBridge.screenMouseX(event),
                    EchoBackendClientBridge.screenMouseY(event)));
            try {
                EchoNativeClientRouteRegistries.get().renderGuiLayer("index", "index.hotkey_screen_render", Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "native_client_bridge",
                        "eventType", "screen_render_post",
                        "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName(),
                        "mouseX", EchoBackendClientBridge.screenMouseX(event),
                        "mouseY", EchoBackendClientBridge.screenMouseY(event),
                        "partialTick", EchoBackendClientBridge.guiPartialTick(event)
                ));
            } finally {
                NATIVE_INDEX_HOTKEY_SCREEN.remove();
            }
            return;
        }
        IndexHotkeys.onScreenRendered(event);
    }

    private static void onIndexHotkeyKeyPressed(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = EchoNativeClientRouteRegistries.get().overlayInput("index", "index.hotkey_key_pressed", Map.of(
                    "source", "native_loader_input_binding",
                    "forwardedFrom", "native_client_bridge",
                    "eventType", "hotkey_key_pressed",
                    "key", EchoBackendClientBridge.keyCode(event),
                    "keyEvent", String.valueOf(EchoBackendClientBridge.keyEvent(event)),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            )) == EchoNativeLoadStatus.MUTATED;
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexHotkeys.onKeyPressed(event);
    }

    private static void onIndexOverlayRender(Object event) {
        if (nativeLoaderActive()) {
            EchoNativeLoadStatus status;
            NATIVE_INDEX_OVERLAY_RENDER.set(event);
            try {
                status = EchoNativeClientRouteRegistries.get().renderGuiLayer("client_overlay", "index.inventory_overlay_render", Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "native_client_bridge",
                        "eventType", "container_foreground_render",
                        "mouseX", EchoBackendClientBridge.screenMouseX(event),
                        "mouseY", EchoBackendClientBridge.screenMouseY(event),
                        "screenClass", EchoBackendClientBridge.containerScreen(event) == null ? "" : EchoBackendClientBridge.containerScreen(event).getClass().getName()
                ));
            } finally {
                NATIVE_INDEX_OVERLAY_RENDER.remove();
            }
            if (status != EchoNativeLoadStatus.MUTATED) {
                IndexOverlay.onRender(event);
            }
            return;
        }
        IndexOverlay.onRender(event);
    }

    private static void onIndexOverlayMouseClicked(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = dispatchNativeOverlayInput("mouse_clicked", Map.of(
                    "button", EchoBackendClientBridge.mouseButton(event),
                    "modifiers", EchoBackendClientBridge.mouseModifiers(event),
                    "mouseX", EchoBackendClientBridge.mouseX(event),
                    "mouseY", EchoBackendClientBridge.mouseY(event),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            ));
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexOverlay.onMouseClicked(event);
    }

    private static void onIndexOverlayMouseDragged(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = dispatchNativeOverlayInput("mouse_dragged", Map.of(
                    "mouseX", EchoBackendClientBridge.mouseX(event),
                    "mouseY", EchoBackendClientBridge.mouseY(event),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            ));
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexOverlay.onMouseDragged(event);
    }

    private static void onIndexOverlayMouseReleased(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = dispatchNativeOverlayInput("mouse_released", Map.of(
                    "button", EchoBackendClientBridge.mouseButton(event),
                    "mouseX", EchoBackendClientBridge.mouseX(event),
                    "mouseY", EchoBackendClientBridge.mouseY(event),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            ));
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexOverlay.onMouseReleased(event);
    }

    private static void onIndexOverlayMouseScrolled(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = dispatchNativeOverlayInput("mouse_scrolled", Map.of(
                    "mouseX", EchoBackendClientBridge.mouseX(event),
                    "mouseY", EchoBackendClientBridge.mouseY(event),
                    "scrollDeltaY", EchoBackendClientBridge.scrollDeltaY(event),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            ));
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexOverlay.onMouseScrolled(event);
    }

    private static void onIndexOverlayKeyPressed(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = dispatchNativeOverlayInput("key_pressed", Map.of(
                    "key", EchoBackendClientBridge.keyCode(event),
                    "keyEvent", String.valueOf(EchoBackendClientBridge.keyEvent(event)),
                    "recipeKey", EchoBackendClientBridge.keyMappingMatches(SHOW_RECIPE_KEY, event),
                    "usageKey", EchoBackendClientBridge.keyMappingMatches(SHOW_USAGE_KEY, event),
                    "bookmarkKey", EchoBackendClientBridge.keyMappingMatches(BOOKMARK_KEY, event),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            ));
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexOverlay.onKeyPressed(event);
    }

    private static void onIndexOverlayCharTyped(Object event) {
        if (nativeLoaderActive()) {
            boolean handled = dispatchNativeOverlayInput("character_typed", Map.of(
                    "characterEvent", String.valueOf(EchoBackendClientBridge.characterEvent(event)),
                    "character", EchoBackendClientBridge.characterText(event),
                    "allowedChatCharacter", EchoBackendClientBridge.allowedChatCharacter(event),
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName()
            ));
            if (handled) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        IndexOverlay.onCharTyped(event);
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
        if (EchoBackendClientBridge.keyMappingMatches(SHOW_RECIPE_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_R)) {
            openHeldItemIndexRecipe(true);
        } else if (EchoBackendClientBridge.keyMappingMatches(SHOW_USAGE_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_U)) {
            openHeldItemIndexRecipe(false);
        } else if (EchoBackendClientBridge.keyMappingMatches(OPEN_INDEX_KEY, event)
                || (nativeLoaderActive() && keyCode == GLFW.GLFW_KEY_G)) {
            openIndexCatalog();
        }
    }

    private static void onClientLoggingIn(Object event) {
        if (nativeLoaderActive()) {
            dispatchNativeClientLifecycle("index.client.login", "client login", Map.of(
                    "eventType", "client_login",
                    "eventClass", event.getClass().getName()
            ));
            return;
        }
        IndexService.INSTANCE.invalidateRecipes("client login");
    }

    private static void onClientLoggingOut(Object event) {
        if (nativeLoaderActive()) {
            dispatchNativeClientLifecycle("index.client.logout", "client logout", Map.of(
                    "eventType", "client_logout",
                    "eventClass", event.getClass().getName()
            ));
            return;
        }
        IndexService.INSTANCE.invalidateRecipes("client logout");
    }

    private static void onClientResourceLoadFinished(Object event) {
        if (nativeLoaderActive()) {
            dispatchNativeClientLifecycle("index.client.resources_reloaded", "client resources reloaded", Map.of(
                    "eventType", "client_resources_reloaded",
                    "eventClass", event.getClass().getName(),
                    "invalidateScreenCoreIndex", true
            ));
            return;
        }
        IndexService.INSTANCE.invalidateRecipes("client resources reloaded");
        invalidateScreenCoreIndex();
    }

    private static void registerTerminalClientIntegration() {
        try {
            Class.forName("com.knoxhack.echoindex.integration.IndexTerminalClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            EchoIndex.LOGGER.warn("ECHO: Index terminal client integration could not be registered.", exception);
        }
    }

    private static void registerScreenCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoindex.client.IndexScreenCoreBridge")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoIndex.LOGGER.warn("ECHO: Index ScreenCore integration could not be registered.", exception);
        }
    }

    private static boolean openScreenCoreIndex() {
        if (!Config.UI_USE_SCREENCORE.get() || !EchoRuntimeModules.isLoaded("echoscreencore")) {
            return false;
        }
        try {
            Object opened = Class.forName("com.knoxhack.echoindex.client.IndexScreenCoreBridge")
                    .getMethod("open")
                    .invoke(null);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoIndex.LOGGER.warn("ECHO: ScreenCore Index failed to open; falling back to legacy Index.", exception);
            return false;
        }
    }

    private static boolean openScreenCoreIndexMode(String mode) {
        if (!Config.UI_USE_SCREENCORE.get() || !EchoRuntimeModules.isLoaded("echoscreencore")) {
            return false;
        }
        try {
            Object opened = Class.forName("com.knoxhack.echoindex.client.IndexScreenCoreBridge")
                    .getMethod("openMode", String.class)
                    .invoke(null, mode);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoIndex.LOGGER.warn("ECHO: ScreenCore Index mode {} failed; falling back to legacy Index.", mode, exception);
            return false;
        }
    }

    private static boolean openIndexCatalog() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (minecraft.screen instanceof IndexCatalogScreen || minecraft.screen instanceof IndexRecipeScreen) {
            return true;
        }
        if (minecraft.screen != null) {
            return false;
        }
        if (!openScreenCoreIndex()) {
            EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(
                    "open",
                    "index.catalog",
                    IndexCatalogScreen.class.getName(),
                    Map.of(
                            "targetScreenClass", IndexCatalogScreen.class.getName(),
                            "transitionSource", "index_route_catalog_fallback",
                            "screenBridge", "classic_index"
                    ));
            if (nativeLoaderActive()
                    && lifecycleStatus != EchoNativeLoadStatus.MUTATED
                    && lifecycleStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                return false;
            }
            minecraft.setScreen(new IndexCatalogScreen());
        }
        return true;
    }

    private static boolean openHeldItemIndexRecipe(boolean recipes) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        return openItemIndexRecipe(stack, recipes);
    }

    private static boolean openRouteItemIndexRecipe(Map<String, Object> metadata, boolean recipes) {
        ItemStack stack = routeItemStack(metadata);
        if (stack.isEmpty()) {
            return false;
        }
        return openItemIndexRecipe(stack, recipes);
    }

    private static boolean trackRouteItemInIndex(Map<String, Object> metadata) {
        Identifier itemId = routeItemId(metadata);
        if (itemId == null) {
            return false;
        }
        return EchoNetClientActions.trySendServerboundAction(
                new IndexActionPacket(IndexActionPacket.Action.BOOKMARK, itemId));
    }

    private static ItemStack routeItemStack(Map<String, Object> metadata) {
        Identifier itemId = routeItemId(metadata);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, intMetadata(metadata, "itemCount")));
    }

    private static Identifier routeItemId(Map<String, Object> metadata) {
        String rawItemId = text(metadata == null ? "" : metadata.get("itemId"));
        if (rawItemId.isBlank()) {
            return null;
        }
        Identifier itemId = Identifier.tryParse(rawItemId);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }
        return itemId;
    }

    private static boolean openItemIndexRecipe(ItemStack stack, boolean recipes) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (minecraft.screen instanceof IndexRecipeScreen) {
            return true;
        }
        if (minecraft.screen != null) {
            return false;
        }
        if (stack.isEmpty()) {
            return false;
        }
        IndexRecipeScreen.Mode mode = recipes ? IndexRecipeScreen.Mode.RECIPES : IndexRecipeScreen.Mode.USES;
        EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(
                "open",
                recipes ? "index.recipe" : "index.usage",
                IndexRecipeScreen.class.getName(),
                Map.of(
                        "targetScreenClass", IndexRecipeScreen.class.getName(),
                        "transitionSource", "index_route_item_recipe",
                        "recipeMode", mode.name(),
                        "itemId", IndexService.itemId(stack.getItem()).toString()
                ));
        if (nativeLoaderActive()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED
                && lifecycleStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return false;
        }
        minecraft.setScreen(new IndexRecipeScreen(stack.copy(),
                mode));
        return true;
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
                EchoIndex.MODID,
                "echoindex:index",
                "index",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                        "nativeScreenBridgeClass", "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                        "source", "echoindex_native_module_route_registrar"),
                nativeRouteEvidence(),
                true);
        registry.registerActions(EchoIndex.MODID, "echoindex:index", "index", Map.ofEntries(
                Map.entry("index.catalog", Map.of("kind", "screen_bridge", "bridgeMethod", "open")),
                Map.entry("index.recipe", Map.of("kind", "item_recipe", "recipeMode", "recipes")),
                Map.entry("index.usage", Map.of("kind", "item_recipe", "recipeMode", "usages")),
                Map.entry("index.bookmark", Map.of("kind", "screen_core_mode", "mode", "favorites")),
                Map.entry("index.hotkey_screen_render", Map.of("kind", "hotkey_screen_render")),
                Map.entry("index.hotkey_key_pressed", Map.of("kind", "hotkey_key_pressed")),
                Map.entry("index.client.login", Map.of("kind", "client_lifecycle", "reason", "client login")),
                Map.entry("index.client.logout", Map.of("kind", "client_lifecycle", "reason", "client logout")),
                Map.entry("index.client.resources_reloaded", Map.of(
                        "kind", "client_lifecycle",
                        "reason", "client resources reloaded",
                        "invalidateScreenCoreIndex", true)),
                Map.entry("index.recipe_screen.mouse", Map.of("kind", "recipe_screen_mouse_input")),
                Map.entry("index.recipe_screen.scroll", Map.of("kind", "recipe_screen_scroll_input")),
                Map.entry("index.recipe_screen.key", Map.of("kind", "recipe_screen_key_input")),
                Map.entry("index.recipe_screen.char", Map.of("kind", "recipe_screen_char_input"))));
        registry.registerActions(EchoIndex.MODID, "echoindex:index", "index", Map.of(
                "index.catalog_screen.mouse", Map.of("kind", "catalog_screen_mouse_input"),
                "index.catalog_screen.scroll", Map.of("kind", "catalog_screen_scroll_input"),
                "index.catalog_screen.key", Map.of("kind", "catalog_screen_key_input"),
                "index.catalog_screen.char", Map.of("kind", "catalog_screen_char_input"),
                "index.screencore.action", Map.of(
                        "kind", "index_screencore_action",
                        "screenBridge", "echoscreencore",
                        "actionCatalog", "IndexActions")));
        for (NativeIndexInputBinding binding : NATIVE_INDEX_INPUT_BINDINGS) {
            registerInputBinding(registry, binding);
        }
        registry.registerActionHandler("index", "echoindex:index", EchoIndexClient::dispatchNativeClientRoute);
        registry.registerRoute(
                EchoIndex.MODID,
                "echoindex:inventory_overlay",
                "client_overlay",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoindex.client.IndexOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                        "source", "echoindex_native_module_route_registrar"),
                nativeRouteEvidence(),
                true);
        registry.registerActions(EchoIndex.MODID, "echoindex:inventory_overlay", "client_overlay", Map.of(
                "index.inventory_overlay_render", Map.of("kind", "overlay_render"),
                "index.inventory_overlay_input", Map.of("kind", "overlay_input"),
                "index.open_recipes_for_item", Map.of("kind", "item_recipe", "recipeMode", "recipes"),
                "index.open_usages_for_item", Map.of("kind", "item_recipe", "recipeMode", "usages"),
                "index.track_item", Map.of("kind", "item_recipe", "recipeMode", "track"),
                "index.toggle_favorite", Map.of("kind", "screen_core_mode", "mode", "favorites")));
        registry.registerActionHandler("client_overlay", "echoindex:inventory_overlay",
                EchoIndexClient::dispatchNativeClientRoute);
    }

    private static void registerInputBinding(
            EchoNativeClientRouteRegistry registry,
            NativeIndexInputBinding binding
    ) {
        registry.registerInputBinding("index", binding.actionId(), Map.of(
                "keyMapping", binding.keyMapping(),
                "keyCode", binding.keyCode(),
                "inputType", "press",
                "action", binding.actionId(),
                "source", "echoindex_native_input_route_registry",
                "nativeLoaderHostService", "key_input",
                "nativeInputOwner", "EchoNativeClientRouteRegistries"));
    }

    private static EchoNativeLoadStatus dispatchNativeInput(Object event) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        int keyCode = EchoBackendClientBridge.keyCode(event);
        for (NativeIndexInputBinding binding : NATIVE_INDEX_INPUT_BINDINGS) {
            if (binding.keyCode() == keyCode) {
                return registry.keyInput(
                        binding.keyMapping(),
                        keyCode,
                        "press",
                        nativeKeyMetadata(event, binding.actionId(), binding.keyMapping()));
            }
        }
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    private static Map<String, Object> nativeKeyMetadata(Object event, String actionId, String keyMapping) {
        return Map.of(
                "source", "native_loader_input_binding",
                "forwardedFrom", "native_client_bridge",
                "eventType", "key_input",
                "actionId", actionId == null ? "" : actionId,
                "keyMapping", keyMapping == null ? "" : keyMapping,
                "inputType", "press",
                "key", EchoBackendClientBridge.keyCode(event),
                "glfwAction", GLFW.GLFW_PRESS,
                "keyEvent", String.valueOf(event)
        );
    }

    private static boolean dispatchNativeOverlayInput(String eventType, Map<String, Object> eventMetadata) {
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("source", "native_loader_overlay_input");
        metadata.put("forwardedFrom", "native_client_bridge");
        metadata.put("eventType", eventType);
        metadata.put("eventMetadata", eventMetadata == null ? Map.of() : Map.copyOf(eventMetadata));
        return EchoNativeClientRouteRegistries.get().overlayInput(
                "client_overlay",
                "index.inventory_overlay_input",
                Map.copyOf(metadata)) == EchoNativeLoadStatus.MUTATED;
    }

    private static EchoNativeLoadStatus dispatchNativeClientLifecycle(
            String actionId,
            String reason,
            Map<String, Object> eventMetadata
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_client_lifecycle");
        metadata.put("forwardedFrom", "native_client_bridge");
        metadata.put("eventType", eventMetadata == null ? "" : eventMetadata.getOrDefault("eventType", ""));
        metadata.put("reason", reason == null ? "" : reason);
        metadata.put("eventMetadata", eventMetadata == null ? Map.of() : Map.copyOf(eventMetadata));
        return EchoNativeClientRouteRegistries.get().dispatchStatus("index", actionId, Map.copyOf(metadata));
    }

    private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        String kind = text(context.action().get("kind"));
        if ("client_lifecycle".equals(kind)) {
            String reason = textOrDefault(context.action().get("reason"), text(context.metadata().get("reason")));
            IndexService.INSTANCE.invalidateRecipes(reason);
            boolean screenCoreInvalidated = booleanMetadata(context.action(), "invalidateScreenCoreIndex")
                    || booleanMetadata(context.metadata(), "invalidateScreenCoreIndex");
            if (screenCoreInvalidated) {
                invalidateScreenCoreIndex();
            }
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    true,
                    screenCoreInvalidated ? "client_lifecycle_cache_and_screencore_invalidated"
                            : "client_lifecycle_cache_invalidated",
                    context.metadata());
            return true;
        }
        if ("hotkey_screen_render".equals(kind)) {
            NativeIndexHotkeyScreenContext screenContext = NATIVE_INDEX_HOTKEY_SCREEN.get();
            boolean updated = screenContext != null
                    && IndexHotkeys.recordNativeScreenRender(
                    screenContext.screen(),
                    screenContext.mouseX(),
                    screenContext.mouseY());
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    updated,
                    updated ? "hotkey_screen_state_updated" : "hotkey_screen_state_unavailable",
                    context.metadata());
            return updated;
        }
        if ("hotkey_key_pressed".equals(kind)) {
            boolean opened = IndexHotkeys.handleNativeKey(intMetadata(context.metadata(), "key"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    opened,
                    opened ? "hovered_stack_opened" : "hovered_stack_unavailable",
                    context.metadata());
            return opened;
        }
        if ("recipe_screen_mouse_input".equals(kind)) {
            IndexRecipeScreen screen = NATIVE_RECIPE_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteMouse(
                    text(context.metadata().get("phase")),
                    doubleMetadata(context.metadata(), "mouseX"),
                    doubleMetadata(context.metadata(), "mouseY"),
                    intMetadata(context.metadata(), "button"),
                    intMetadata(context.metadata(), "modifiers"),
                    booleanMetadata(context.metadata(), "doubleClick"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "recipe_screen_mouse_handled" : "recipe_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("recipe_screen_scroll_input".equals(kind)) {
            IndexRecipeScreen screen = NATIVE_RECIPE_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteScroll(
                    doubleMetadata(context.metadata(), "mouseX"),
                    doubleMetadata(context.metadata(), "mouseY"),
                    doubleMetadata(context.metadata(), "scrollX"),
                    doubleMetadata(context.metadata(), "scrollY"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "recipe_screen_scroll_handled" : "recipe_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("recipe_screen_key_input".equals(kind)) {
            IndexRecipeScreen screen = NATIVE_RECIPE_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteKey(
                    intMetadata(context.metadata(), "key"),
                    booleanMetadata(context.metadata(), "recipeKey"),
                    booleanMetadata(context.metadata(), "usageKey"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "recipe_screen_key_handled" : "recipe_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("recipe_screen_char_input".equals(kind)) {
            IndexRecipeScreen screen = NATIVE_RECIPE_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteChar(
                    text(context.metadata().get("character")),
                    booleanMetadata(context.metadata(), "allowedChatCharacter"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "recipe_screen_char_handled" : "recipe_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("catalog_screen_mouse_input".equals(kind)) {
            IndexCatalogScreen screen = NATIVE_CATALOG_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteMouse(
                    text(context.metadata().get("phase")),
                    doubleMetadata(context.metadata(), "mouseX"),
                    doubleMetadata(context.metadata(), "mouseY"),
                    intMetadata(context.metadata(), "button"),
                    intMetadata(context.metadata(), "modifiers"),
                    booleanMetadata(context.metadata(), "doubleClick"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "catalog_screen_mouse_handled" : "catalog_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("catalog_screen_scroll_input".equals(kind)) {
            IndexCatalogScreen screen = NATIVE_CATALOG_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteScroll(
                    doubleMetadata(context.metadata(), "mouseX"),
                    doubleMetadata(context.metadata(), "mouseY"),
                    doubleMetadata(context.metadata(), "scrollX"),
                    doubleMetadata(context.metadata(), "scrollY"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "catalog_screen_scroll_handled" : "catalog_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("catalog_screen_key_input".equals(kind)) {
            IndexCatalogScreen screen = NATIVE_CATALOG_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteKey(
                    intMetadata(context.metadata(), "key"),
                    booleanMetadata(context.metadata(), "recipeKey"),
                    booleanMetadata(context.metadata(), "usageKey"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "catalog_screen_key_handled" : "catalog_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("catalog_screen_char_input".equals(kind)) {
            IndexCatalogScreen screen = NATIVE_CATALOG_SCREEN.get();
            boolean handled = screen != null && screen.handleNativeRouteChar(
                    text(context.metadata().get("character")),
                    booleanMetadata(context.metadata(), "allowedChatCharacter"));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "catalog_screen_char_handled" : "catalog_screen_unavailable",
                    context.metadata());
            return handled;
        }
        if ("index_screencore_action".equals(kind)) {
            EchoAction action = NATIVE_SCREEN_CORE_ACTION.get();
            EchoActionContext actionContext = NATIVE_SCREEN_CORE_ACTION_CONTEXT.get();
            boolean handled = action != null && actionContext != null && action.run(actionContext);
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "screencore_action_handled" : "screencore_action_unavailable",
                    context.metadata());
            return handled;
        }
        if (minecraft.player == null) {
            return false;
        }
        if ("overlay_render".equals(kind)) {
            Object event = NATIVE_INDEX_OVERLAY_RENDER.get();
            boolean handled = event != null;
            if (handled) {
                IndexOverlay.onRender(event);
            }
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    kind,
                    context.metadata());
            return handled;
        }
        if ("overlay_input".equals(kind)) {
            boolean handled = IndexOverlay.handleNativeRouteInput(
                    text(context.metadata().get("eventType")),
                    objectMap(context.metadata().get("eventMetadata")));
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "overlay_input_handled" : "overlay_input_ignored",
                    context.metadata());
            return handled;
        }
        if ("item_recipe".equals(kind)) {
            String recipeMode = text(context.action().get("recipeMode"));
            if ("track".equals(recipeMode)) {
                boolean tracked = trackRouteItemInIndex(context.metadata());
                IndexNativeSessionBridge.recordNativeRoute(
                        context.actionId(),
                        context.action(),
                        tracked,
                        tracked ? "item_tracked" : "unavailable",
                        context.metadata());
                return tracked;
            }
            boolean recipes = !"usages".equals(recipeMode);
            boolean opened = openRouteItemIndexRecipe(context.metadata(), recipes)
                    || openHeldItemIndexRecipe(recipes)
                    || openScreenCoreIndexMode(recipes ? "recipes" : "usages")
                    || openIndexCatalog();
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    opened,
                    opened ? (recipes ? "recipes_opened" : "usages_opened") : "unavailable",
                    context.metadata());
            return opened;
        }
        if ("screen_core_mode".equals(kind)) {
            String mode = textOrDefault(context.action().get("mode"), "favorites");
            boolean opened = openScreenCoreIndexMode(mode)
                    || openIndexCatalog();
            IndexNativeSessionBridge.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    opened,
                    opened ? "mode_opened:" + mode : "unavailable",
                    context.metadata());
            return opened;
        }
        boolean opened = switch (context.actionId()) {
            case "index.catalog" -> openIndexCatalog();
            case "index.recipe", "index.open_recipes_for_item" -> openRouteItemIndexRecipe(context.metadata(), true)
                    || openHeldItemIndexRecipe(true)
                    || openScreenCoreIndexMode("recipes")
                    || openIndexCatalog();
            case "index.usage", "index.open_usages_for_item" -> openRouteItemIndexRecipe(context.metadata(), false)
                    || openHeldItemIndexRecipe(false)
                    || openScreenCoreIndexMode("usages")
                    || openIndexCatalog();
            case "index.track_item" -> trackRouteItemInIndex(context.metadata());
            case "index.bookmark", "index.toggle_favorite" -> openScreenCoreIndexMode("favorites")
                    || openIndexCatalog();
            case "index.inventory_overlay_render", "index.inventory_overlay_input" -> false;
            case "index.recipe_screen.mouse", "index.recipe_screen.scroll",
                    "index.recipe_screen.key", "index.recipe_screen.char" -> false;
            case "index.catalog_screen.mouse", "index.catalog_screen.scroll",
                    "index.catalog_screen.key", "index.catalog_screen.char" -> false;
            default -> false;
        };
        IndexNativeSessionBridge.recordNativeRoute(
                context.actionId(),
                context.action(),
                opened,
                opened ? "opened" : "unavailable",
                context.metadata());
        return opened;
    }

    public static boolean dispatchNativeCatalogScreenMouse(
            IndexCatalogScreen screen,
            String phase,
            double mouseX,
            double mouseY,
            int button,
            int modifiers,
            boolean doubleClick
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_CATALOG_SCREEN.set(screen);
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "native_screen_lifecycle");
            metadata.put("eventType", "catalog_screen_mouse_input");
            metadata.put("screenClass", screen.getClass().getName());
            metadata.put("phase", phase);
            metadata.put("mouseX", mouseX);
            metadata.put("mouseY", mouseY);
            metadata.put("button", button);
            metadata.put("modifiers", modifiers);
            metadata.put("doubleClick", doubleClick);
            return EchoNativeClientRouteRegistries.get().mouseInput("index", "index.catalog_screen.mouse", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_CATALOG_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeCatalogScreenScroll(
            IndexCatalogScreen screen,
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_CATALOG_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().mouseInput("index", "index.catalog_screen.scroll", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "catalog_screen_scroll_input",
                    "screenClass", screen.getClass().getName(),
                    "mouseX", mouseX,
                    "mouseY", mouseY,
                    "scrollX", scrollX,
                    "scrollY", scrollY
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_CATALOG_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeCatalogScreenKey(IndexCatalogScreen screen, int key, boolean recipeKey, boolean usageKey) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_CATALOG_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().overlayInput("index", "index.catalog_screen.key", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "catalog_screen_key_input",
                    "screenClass", screen.getClass().getName(),
                    "key", key,
                    "recipeKey", recipeKey,
                    "usageKey", usageKey
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_CATALOG_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeCatalogScreenChar(
            IndexCatalogScreen screen,
            String character,
            boolean allowedChatCharacter
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_CATALOG_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().overlayInput("index", "index.catalog_screen.char", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "catalog_screen_character_typed",
                    "screenClass", screen.getClass().getName(),
                    "character", character == null ? "" : character,
                    "allowedChatCharacter", allowedChatCharacter
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_CATALOG_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeRecipeScreenMouse(
            IndexRecipeScreen screen,
            String phase,
            double mouseX,
            double mouseY,
            int button,
            int modifiers,
            boolean doubleClick
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_RECIPE_SCREEN.set(screen);
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "native_screen_lifecycle");
            metadata.put("eventType", "recipe_screen_mouse_input");
            metadata.put("screenClass", screen.getClass().getName());
            metadata.put("phase", phase);
            metadata.put("mouseX", mouseX);
            metadata.put("mouseY", mouseY);
            metadata.put("button", button);
            metadata.put("modifiers", modifiers);
            metadata.put("doubleClick", doubleClick);
            return EchoNativeClientRouteRegistries.get().mouseInput("index", "index.recipe_screen.mouse", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_RECIPE_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeRecipeScreenScroll(
            IndexRecipeScreen screen,
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_RECIPE_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().mouseInput("index", "index.recipe_screen.scroll", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "recipe_screen_scroll_input",
                    "screenClass", screen.getClass().getName(),
                    "mouseX", mouseX,
                    "mouseY", mouseY,
                    "scrollX", scrollX,
                    "scrollY", scrollY
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_RECIPE_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeRecipeScreenKey(IndexRecipeScreen screen, int key, boolean recipeKey, boolean usageKey) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_RECIPE_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().overlayInput("index", "index.recipe_screen.key", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "recipe_screen_key_input",
                    "screenClass", screen.getClass().getName(),
                    "key", key,
                    "recipeKey", recipeKey,
                    "usageKey", usageKey
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_RECIPE_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeRecipeScreenChar(
            IndexRecipeScreen screen,
            String character,
            boolean allowedChatCharacter
    ) {
        if (!nativeLoaderActive() || screen == null) {
            return false;
        }
        NATIVE_RECIPE_SCREEN.set(screen);
        try {
            return EchoNativeClientRouteRegistries.get().overlayInput("index", "index.recipe_screen.char", Map.of(
                    "source", "native_screen_lifecycle",
                    "eventType", "recipe_screen_character_typed",
                    "screenClass", screen.getClass().getName(),
                    "character", character == null ? "" : character,
                    "allowedChatCharacter", allowedChatCharacter
            )) == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_RECIPE_SCREEN.remove();
        }
    }

    public static boolean dispatchNativeScreenCoreAction(
            String screenCoreActionId,
            EchoActionContext actionContext,
            EchoAction action
    ) {
        if (!nativeLoaderActive()) {
            return false;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_screencore_action");
        metadata.put("eventType", "index_screencore_action");
        metadata.put("screenClass", "com.knoxhack.echoindex.client.IndexActions");
        metadata.put("actionCatalog", "IndexActions");
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
            return EchoNativeClientRouteRegistries.get().dispatchStatus("index", "index.screencore.action", metadata)
                    == EchoNativeLoadStatus.MUTATED;
        } finally {
            NATIVE_SCREEN_CORE_ACTION.remove();
            NATIVE_SCREEN_CORE_ACTION_CONTEXT.remove();
        }
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
        event.put("source", "native_index_screen_transition");
        event.put("eventType", "index_screen_lifecycle");
        event.put("screenClass", screenClass == null ? "" : screenClass);
        event.put("screenTransitionPhase", phase == null ? "" : phase);
        if (metadata != null) {
            event.putAll(metadata);
        }
        event.put("nativeLoaderUiHostService", "screen_lifecycle");
        event.put("nativeLoaderUiHostSurface", "index");
        event.put("nativeLoaderUiHostAction", textOrDefault(actionId, "index.screen.lifecycle"));
        event.put("nativeLoaderScreenLifecycleHandoff", true);
        String safePhase = phase == null ? "" : phase;
        String safeActionId = textOrDefault(actionId, "index.screen.lifecycle");
        return switch (safePhase) {
            case "mount" -> EchoNativeClientRouteRegistries.get().mountSurface("index", safeActionId, Map.copyOf(event));
            case "open" -> EchoNativeClientRouteRegistries.get().openSurface("index", safeActionId, Map.copyOf(event));
            case "close" -> EchoNativeClientRouteRegistries.get().closeSurface("index", safeActionId, Map.copyOf(event));
            case "unmount" -> EchoNativeClientRouteRegistries.get().unmountSurface("index", safeActionId, Map.copyOf(event));
            default -> EchoNativeClientRouteRegistries.get().screenLifecycle(
                    "index",
                    safePhase,
                    safeActionId,
                    Map.copyOf(event));
        };
    }

    private static Map<String, Object> nativeRouteEvidence() {
        return Map.of(
                "nativeClientRouteProcess", true,
                "clientRouteMutationSupported", true,
                "nativeClientRouteSdk", "echo-native-client-route-registry");
    }

    private static boolean nativeLoaderActive() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String textOrDefault(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback : text;
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

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            metadata.put(key, String.valueOf(value));
        }
    }

    private static void invalidateScreenCoreIndex() {
        if (!EchoRuntimeModules.isLoaded("echoscreencore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echoindex.client.IndexScreenCoreBridge")
                    .getMethod("invalidate")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoIndex.LOGGER.debug("ECHO: ScreenCore Index invalidation was skipped.", exception);
        }
    }

    private static KeyMapping.Category registerKeyCategory(String path) {
        try {
            return KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(EchoIndex.MODID, path));
        } catch (IllegalArgumentException duplicate) {
            String uniquePath = path + "_native_" + Long.toUnsignedString(System.nanoTime(), 36);
            EchoIndex.LOGGER.debug("ECHO: Index key category {} already exists; using {} for this client loader.",
                    path, uniquePath);
            return KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    EchoIndex.MODID,
                    uniquePath));
        }
    }

    public static final class ClientModEvents {
        private ClientModEvents() {
        }

        static void onRegisterKeyMappings(Object event) {
            EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
            EchoBackendClientBridge.registerKeyMapping(event, SHOW_RECIPE_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, SHOW_USAGE_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, BOOKMARK_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, OPEN_INDEX_KEY);
        }

        static void onRegisterClientTooltipComponents(Object event) {
            IndexTooltipComponents.register(event);
        }

        static void onAddClientReloadListeners(Object event) {
            EchoBackendClientBridge.addClientReloadListener(event, EchoIndex.id("sources"), new IndexSourceReloadListener());
        }
    }

    private record NativeIndexHotkeyScreenContext(Screen screen, int mouseX, int mouseY) {
    }

    private record NativeIndexInputBinding(String actionId, String keyMapping, int keyCode) {
    }
}
