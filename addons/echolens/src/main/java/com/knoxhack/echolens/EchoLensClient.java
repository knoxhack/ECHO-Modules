package com.knoxhack.echolens;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echolens.client.LensClientActions;
import com.knoxhack.echolens.client.LensHudOverlay;
import com.knoxhack.echolens.platform.LensModuleAccess;
import com.mojang.blaze3d.platform.InputConstants;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientRouteActionContext;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class EchoLensClient {
    private static final AtomicBoolean NATIVE_ROUTE_REGISTERED = new AtomicBoolean(false);
    private static final ThreadLocal<NativeLensOverlayRender> NATIVE_LENS_OVERLAY_RENDER = new ThreadLocal<>();
    private static final Map<Integer, NativeLensInputBinding> NATIVE_INPUT_BINDINGS = Map.of(
            GLFW.GLFW_KEY_LEFT_ALT, new NativeLensInputBinding(
                    "lens.deep_scan", "echolens.key.deep_scan", GLFW.GLFW_KEY_LEFT_ALT),
            GLFW.GLFW_KEY_R, new NativeLensInputBinding(
                    "lens.index_recipe", "key.echolens.index_recipe", GLFW.GLFW_KEY_R),
            GLFW.GLFW_KEY_U, new NativeLensInputBinding(
                    "lens.index_usage", "key.echolens.index_usage", GLFW.GLFW_KEY_U),
            GLFW.GLFW_KEY_T, new NativeLensInputBinding(
                    "lens.track_in_index", "key.echolens.track_in_index", GLFW.GLFW_KEY_T));
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(EchoLens.MODID, "lens"));
    public static final KeyMapping DEEP_SCAN_KEY = new KeyMapping(
            "echolens.key.deep_scan",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            KEY_CATEGORY);

    public EchoLensClient() {
        this(null);
    }

    public EchoLensClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoLensClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoLensClient::onRenderGui);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterKeyMappings);
        if (LensModuleAccess.isLoaded("echoindex")) {
            registerIndexClientIntegration();
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
        if (EchoBackendClientBridge.keyMappingMatches(DEEP_SCAN_KEY, event)) {
            LensHudOverlay.requestDeepScan();
            return;
        }
        if (LensHudOverlay.currentTargetStack().isEmpty()) {
            return;
        }
        int key = key(event);
        if (key == GLFW.GLFW_KEY_R) {
            LensClientActions.openIndexRecipes(LensHudOverlay.currentTargetStack());
        } else if (key == GLFW.GLFW_KEY_U) {
            LensClientActions.openIndexUses(LensHudOverlay.currentTargetStack());
        } else if (key == GLFW.GLFW_KEY_T) {
            LensClientActions.trackInIndex(LensHudOverlay.currentTargetStack());
        }
    }

    private static void onRenderGui(Object event) {
        var graphics = EchoBackendClientBridge.guiGraphics(event);
        float partialTick = EchoBackendClientBridge.guiPartialTick(event);
        if (graphics == null) {
            return;
        }
        if (nativeLoaderActive()) {
            NATIVE_LENS_OVERLAY_RENDER.set(new NativeLensOverlayRender(graphics, partialTick));
            try {
                EchoNativeClientRouteRegistries.get().renderGuiLayer("client_overlay", "lens.overlay.render", Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "adaptercore_compatibility_adapter",
                        "eventType", "render_gui_post",
                        "partialTick", partialTick
                ));
            } finally {
                NATIVE_LENS_OVERLAY_RENDER.remove();
            }
            return;
        }
        LensHudOverlay.render(graphics, partialTick);
    }

    private static void registerIndexClientIntegration() {
        try {
            Class.forName("com.knoxhack.echolens.client.integration.LensIndexClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            EchoLens.LOGGER.warn("ECHO: Lens Index client integration could not be registered.", exception);
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
                EchoLens.MODID,
                "echolens:field_lens",
                "lens",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "source", "echolens_native_module_route_registrar"),
                nativeRouteEvidence(),
                true);
        registry.registerActions(EchoLens.MODID, "echolens:field_lens", "lens", Map.of(
                "lens.deep_scan", Map.of("kind", "hud_scan", "mode", "deep"),
                "lens.index_recipe", Map.of("kind", "target_index", "recipeMode", "recipes"),
                "lens.index_usage", Map.of("kind", "target_index", "recipeMode", "usages"),
                "lens.track_in_index", Map.of("kind", "target_index", "recipeMode", "track")));
        NATIVE_INPUT_BINDINGS.values().forEach(binding -> registerInputBinding(
                registry, binding.actionId(), binding.keyMapping(), binding.keyCode()));
        registry.registerActionHandler("lens", "echolens:field_lens", EchoLensClient::dispatchNativeClientRoute);
        registry.registerRoute(
                EchoLens.MODID,
                "echolens:lens_overlay",
                "client_overlay",
                Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "source", "echolens_native_module_route_registrar"),
                nativeRouteEvidence(),
                true);
        registry.registerActions(EchoLens.MODID, "echolens:lens_overlay", "client_overlay", Map.of(
                "lens.overlay.render", Map.of("kind", "overlay_render"),
                "lens.overlay.scan_target", Map.of("kind", "hud_scan", "mode", "target")));
        registry.registerActionHandler("client_overlay", "echolens:lens_overlay",
                EchoLensClient::dispatchNativeClientRoute);
    }

    private static void registerInputBinding(
            EchoNativeClientRouteRegistry registry,
            String actionId,
            String keyMapping,
            int keyCode
    ) {
        registry.registerInputBinding("lens", actionId, Map.of(
                "keyMapping", keyMapping,
                "keyCode", keyCode,
                "inputType", "press",
                "action", actionId,
                "source", "echolens_native_input_route_registry",
                "nativeLoaderHostService", "key_input",
                "nativeInputOwner", "EchoNativeClientRouteRegistries"));
    }

    private static EchoNativeLoadStatus dispatchNativeInput(Object event) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        int keyCode = key(event);
        NativeLensInputBinding binding = NATIVE_INPUT_BINDINGS.get(keyCode);
        if (binding == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return registry.keyInput(
                binding.keyMapping(),
                binding.keyCode(),
                "press",
                nativeKeyMetadata(event, binding.actionId(), binding.keyMapping()));
    }

    private static Map<String, Object> nativeKeyMetadata(Object event, String actionId, String keyMapping) {
        return Map.of(
                "source", "native_loader_input_binding",
                "forwardedFrom", "adaptercore_compatibility_adapter",
                "eventType", "key_input",
                "actionId", actionId == null ? "" : actionId,
                "keyMapping", keyMapping == null ? "" : keyMapping,
                "inputType", "press",
                "key", key(event),
                "glfwAction", action(event),
                "keyEvent", String.valueOf(keyEvent(event))
        );
    }

    private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context) {
        String kind = text(context.action().get("kind"));
        if ("overlay_render".equals(kind) || "lens.overlay.render".equals(context.actionId())) {
            NativeLensOverlayRender event = NATIVE_LENS_OVERLAY_RENDER.get();
            if (event != null) {
                LensHudOverlay.render(
                        event.graphics(),
                        event.partialTick());
                LensHudOverlay.recordNativeRoute(
                        context.actionId(),
                        context.action(),
                        true,
                        "overlay_render",
                        context.metadata());
                return true;
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            LensHudOverlay.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    false,
                    "missing_player_or_screen_open",
                    context.metadata());
            return false;
        }
        if ("hud_scan".equals(kind)) {
            boolean requested = LensHudOverlay.requestDeepScan();
            LensHudOverlay.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    requested,
                    requested ? "scan_requested" : "unavailable",
                    context.metadata());
            return requested;
        }
        if ("target_index".equals(kind)) {
            boolean handled = dispatchTargetIndexAction(text(context.action().get("recipeMode")));
            LensHudOverlay.recordNativeRoute(
                    context.actionId(),
                    context.action(),
                    handled,
                    handled ? "target_index:" + text(context.action().get("recipeMode")) : "unavailable",
                    context.metadata());
            return handled;
        }
        boolean handled = switch (context.actionId()) {
            case "lens.deep_scan", "lens.overlay.scan_target" -> LensHudOverlay.requestDeepScan();
            case "lens.overlay.render" -> false;
            case "lens.index_recipe" -> dispatchTargetIndexAction("recipes");
            case "lens.index_usage" -> dispatchTargetIndexAction("usages");
            case "lens.track_in_index" -> dispatchTargetIndexAction("track");
            default -> false;
        };
        LensHudOverlay.recordNativeRoute(
                context.actionId(),
                context.action(),
                handled,
                handled ? "handled" : "unavailable",
                context.metadata());
        return handled;
    }

    private static boolean dispatchTargetIndexAction(String mode) {
        if (LensHudOverlay.currentTargetStack().isEmpty()) {
            return false;
        }
        if (!LensModuleAccess.isLoaded("echoindex")) {
            return false;
        }
        switch (mode) {
            case "recipes" -> {
                return dispatchNativeIndexTargetRoute("index.open_recipes_for_item", "recipes");
            }
            case "usages" -> {
                return dispatchNativeIndexTargetRoute("index.open_usages_for_item", "usages");
            }
            case "track" -> {
                return dispatchNativeIndexTargetRoute("index.track_item", "track");
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean dispatchNativeIndexTargetRoute(String actionId, String mode) {
        ItemStack stack = LensHudOverlay.currentTargetStack();
        if (stack.isEmpty()) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "echolens_native_route");
        metadata.put("eventType", "lens_target_index_route");
        metadata.put("upstreamSurfaceType", "lens");
        metadata.put("upstreamSurfaceId", "echolens:field_lens");
        metadata.put("upstreamRecipeMode", mode);
        metadata.put("itemId", itemId.toString());
        metadata.put("itemCount", stack.getCount());
        return EchoNativeClientRouteRegistries.get().dispatchStatus(
                "client_overlay",
                actionId,
                Map.copyOf(metadata)) == EchoNativeLoadStatus.MUTATED;
    }

    private static Map<String, Object> nativeRouteEvidence() {
        return Map.of(
                "nativeClientRouteProcess", true,
                "clientRouteMutationSupported", true,
                "nativeClientRouteSdk", "echo-native-client-route-registry");
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private record NativeLensInputBinding(String actionId, String keyMapping, int keyCode) {
    }

    public static final class ClientModEvents {
        private ClientModEvents() {
        }

        static void onRegisterKeyMappings(Object event) {
            EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
            EchoBackendClientBridge.registerKeyMapping(event, DEEP_SCAN_KEY);
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

    private record NativeLensOverlayRender(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            float partialTick
    ) {
    }
}
