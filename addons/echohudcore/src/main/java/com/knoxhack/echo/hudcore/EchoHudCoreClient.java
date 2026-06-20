package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay;
import com.mojang.logging.LogUtils;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientRouteActionContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class EchoHudCoreClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String REGISTER_GUI_LAYERS_EVENT =
            "net.neoforged.neoforge.client.event.RegisterGuiLayersEvent";
    private static final Identifier HUD_LAYER =
            Identifier.fromNamespaceAndPath(EchoHudCore.MODID, "hud");
    private static final AtomicBoolean NATIVE_ROUTE_REGISTERED = new AtomicBoolean(false);
    private static final ThreadLocal<NativeHudRenderContext> NATIVE_HUD_RENDER = new ThreadLocal<>();

    public EchoHudCoreClient() {
        this(null);
    }

    public EchoHudCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_GUI_LAYERS_EVENT,
                EchoHudCoreClient::registerGuiLayers);
        registerNativeClientRoutes();
    }

    private static void registerGuiLayers(Object event) {
        if (EchoBackendClientBridge.registerGuiLayerAboveAir(event, HUD_LAYER, EchoHudCoreClient::renderHudLayer)) {
            LOGGER.info("ECHO: HUDCore client overlay layer registered.");
        }
    }

    private static void renderHudLayer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        renderHud(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
    }

    public static void renderHud(GuiGraphicsExtractor graphics, float partialTick) {
        if (nativeLoaderActive()) {
            NATIVE_HUD_RENDER.set(new NativeHudRenderContext(graphics, partialTick));
            try {
                EchoNativeClientRouteRegistries.get().renderHudLayer("hud", "hud.render", Map.of(
                        "source", "native_loader_hud_layer",
                        "forwardedFrom", "echo_native_client_bridge",
                        "eventType", "hud_render",
                        "partialTick", partialTick
                ));
            } finally {
                NATIVE_HUD_RENDER.remove();
            }
            return;
        }
        EchoHudCoreOverlay.render(graphics, partialTick);
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
        registerRoute(registry, "echohudcore:native_hud", "hud",
                "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                Map.of(
                        "hud.render", Map.of("kind", "hud_render"),
                        "hud.update_snapshot", Map.of("kind", "hud_state_update"),
                        "native_loader.overlay_focus", Map.of("kind", "hud_overlay_focus")));
        registerRoute(registry, "echohudcore:mission_tracker", "hud_widget",
                "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                Map.of("hud.mission_tracker.render",
                        Map.of("kind", "hud_widget_render", "widget", "mission_tracker")));
        registerRoute(registry, "echohudcore:hazard_readout", "hud_widget",
                "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                Map.of("hud.hazard_readout.render",
                        Map.of("kind", "hud_widget_render", "widget", "hazard_readout")));
        registerRoute(registry, "echohudcore:compass_indicator", "hud_widget",
                "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                Map.of("hud.compass_indicator.render",
                        Map.of("kind", "hud_widget_render", "widget", "compass_indicator")));
        registerRoute(registry, "echohudcore:screen_safe_area", "hud_layout",
                "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                Map.of("hud.screen_safe_area.resolve", Map.of("kind", "hud_layout_resolve")));
        registry.registerActionHandler("hud", "echohudcore:native_hud", EchoHudCoreClient::dispatchNativeClientRoute);
        registry.registerActionHandler("hud_widget", "echohudcore:mission_tracker",
                context -> dispatchNativeRouteSurface("echohudcore:mission_tracker", context));
        registry.registerActionHandler("hud_widget", "echohudcore:hazard_readout",
                context -> dispatchNativeRouteSurface("echohudcore:hazard_readout", context));
        registry.registerActionHandler("hud_widget", "echohudcore:compass_indicator",
                context -> dispatchNativeRouteSurface("echohudcore:compass_indicator", context));
        registry.registerActionHandler("hud_layout", "echohudcore:screen_safe_area",
                context -> dispatchNativeRouteSurface("echohudcore:screen_safe_area", context));
    }

    private static void registerRoute(
            EchoNativeClientRouteRegistry registry,
            String surfaceId,
            String surfaceType,
            String implementationClass,
            Map<String, Map<String, Object>> actions
    ) {
        registry.registerRoute(
                EchoHudCore.MODID,
                surfaceId,
                surfaceType,
                Map.of(
                        "nativeSurfaceImplementationClass", implementationClass,
                        "nativeScreenBridgeClass", implementationClass,
                        "source", "echohudcore_native_module_route_registrar"),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry"),
                true);
        registry.registerActions(EchoHudCore.MODID, surfaceId, surfaceType, actions);
    }

    private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context) {
        EchoHudCoreOverlay.enableNativeRoute();
        String kind = String.valueOf(context.action().getOrDefault("kind", "")).trim();
        if ("hud.render".equals(context.actionId()) || "hud_render".equals(kind)) {
            NativeHudRenderContext render = NATIVE_HUD_RENDER.get();
            if (render != null) {
                EchoHudCoreOverlay.handleNativeHudAction(context.actionId(), context.action(), context.metadata());
                EchoHudCoreOverlay.render(
                        render.graphics(),
                        render.partialTick());
                return true;
            }
        }
        return EchoHudCoreOverlay.handleNativeHudAction(context.actionId(), context.action(), context.metadata());
    }

    private static boolean dispatchNativeRouteSurface(
            String expectedSurfaceId,
            NativeClientRouteActionContext context
    ) {
        if (!String.valueOf(expectedSurfaceId).equals(String.valueOf(context.route().get("surfaceId")))) {
            return false;
        }
        return dispatchNativeClientRoute(context);
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private record NativeHudRenderContext(GuiGraphicsExtractor graphics, float partialTick) {
    }
}
