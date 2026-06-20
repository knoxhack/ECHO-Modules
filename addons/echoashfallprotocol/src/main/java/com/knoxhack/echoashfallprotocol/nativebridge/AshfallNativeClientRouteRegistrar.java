package com.knoxhack.echoashfallprotocol.nativebridge;

import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycle;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycleEvent;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AshfallNativeClientRouteRegistrar {
    private static final String MODID = "echoashfallprotocol";
    private static final String HANDLER_ID = "echoashfallprotocol:native_client_route_dispatcher";
    private static final System.Logger LOGGER =
            System.getLogger(AshfallNativeClientRouteRegistrar.class.getName());
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final List<RouteSurface> SURFACES = List.of(
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:echo_native_main_menu",
                    "main_menu",
                    "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen",
                    "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen",
                    Map.of(),
                    false),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:echo_native_loading",
                    "loading_screen",
                    "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallLoadingOverlay",
                    "dev.echo.nativeplatform.bootstrap.EchoNativeLiveLoadingRenderBridge",
                    Map.of(),
                    false),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:ashfall_survival_hud",
                    "hud",
                    "com.knoxhack.echoashfallprotocol.client.hud.EchoNativeAshfallHudOverlay",
                    "dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge",
                    Map.of(),
                    false),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:ashfall_status_overlay",
                    "client_overlay",
                    "com.knoxhack.echoashfallprotocol.client.hud.MutationOverlayEffect",
                    "dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge",
                    Map.of(),
                    false),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:terminal_eui_handoff",
                    "terminal",
                    "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen",
                    "com.knoxhack.echoterminal.client.screen.EchoTerminalScreens",
                    actions(
                            "terminal.open", action("kind", "terminal_screen"),
                    "signalos.terminal", action("kind", "terminal_screen", "aliasSurface", "signalos")),
                    true),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:index_handoff",
                    "index",
                    "com.knoxhack.echoindex.client.IndexCatalogScreen",
                    "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                    actions(
                            "index.catalog", action("kind", "screen_bridge", "bridgeMethod", "open"),
                            "index.recipe", action("kind", "item_recipe", "recipeMode", "recipes"),
                            "index.usage", action("kind", "item_recipe", "recipeMode", "usages"),
                            "index.bookmark", action("kind", "screen_core_mode", "mode", "favorites")),
                    true),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:lens_handoff",
                    "lens",
                    "com.knoxhack.echolens.client.LensHudOverlay",
                    "com.knoxhack.echolens.client.LensHudOverlay",
                    actions("lens.deep_scan", action("kind", "hud_scan", "mode", "deep")),
                    true),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:holomap_minimap_handoff",
                    "holomap",
                    "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                    "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                    actions(
                            "holomap.toggle_minimap", action("kind", "overlay_command", "bridgeMethod", "toggle"),
                            "holomap.zoom_in", action("kind", "overlay_command", "bridgeMethod", "zoomIn"),
                            "holomap.zoom_out", action("kind", "overlay_command", "bridgeMethod", "zoomOut"),
                            "holomap.cycle_corner", action("kind", "overlay_command", "bridgeMethod", "cycleCorner")),
                    true),
            new RouteSurface(
                    MODID,
                    "echoashfallprotocol:holomap_fullscreen_handoff",
                    "holomap",
                    "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                    "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                    actions(
                            "holomap.open", action("kind", "screen_bridge", "bridgeMethod", "openFullscreen")),
                    true)
    );

    private AshfallNativeClientRouteRegistrar() {
    }

    public static boolean ensureNativeClientRoutesRegisteredForNativeLoader() {
        register(true, (surfaceType, action, metadata) -> true);
        return EchoNativeClientRouteRegistries.get() != EchoNativeClientRouteRegistry.NOOP;
    }

    public static void register(boolean nativeLoaderActive, SurfaceDispatcher dispatcher) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        if (!nativeLoaderActive
                || dispatcher == null
                || registry == EchoNativeClientRouteRegistry.NOOP) {
            return;
        }
        boolean firstRegistration = REGISTERED.compareAndSet(false, true);
        if (firstRegistration) {
            for (RouteSurface surface : SURFACES) {
                registerRoute(registry, surface);
                registerLifecycle(registry, surface);
                registerActions(registry, surface);
            }
        }
        for (RouteSurface surface : SURFACES) {
            if (surface.dispatchable()) {
                registerHandler(registry, surface.surfaceType(), dispatcher);
            }
        }
    }

    public static boolean dispatch(String surfaceType, String action) {
        return EchoNativeClientRouteRegistries.get().dispatch(surfaceType, action);
    }

    public static List<Map<String, Object>> surfaceCatalogForTests() {
        List<Map<String, Object>> catalog = new java.util.ArrayList<>();
        for (RouteSurface surface : SURFACES) {
            Map<String, Object> metadata = productSurfaceMetadata(surface);
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("moduleId", surface.moduleId());
            snapshot.put("surfaceId", surface.surfaceId());
            snapshot.put("surfaceType", surface.surfaceType());
            snapshot.put("implementationClass", surface.implementationClass());
            snapshot.put("bridgeClass", surface.bridgeClass());
            snapshot.put("actions", List.copyOf(surface.actions().keySet()));
            snapshot.put("actionMetadata", surface.actions());
            snapshot.put("dispatchable", surface.dispatchable());
            snapshot.put("role", metadata.get("role"));
            snapshot.put("visibleRuntimePath", metadata.get("visibleRuntimePath"));
            snapshot.put("defaultMountedProductSurface", metadata.get("mountedByDefault"));
            snapshot.put("metadata", metadata);
            catalog.add(Map.copyOf(snapshot));
        }
        return List.copyOf(catalog);
    }

    public static void registerInputBinding(
            String surfaceType,
            String action,
            Map<String, Object> binding
    ) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        if (registry == EchoNativeClientRouteRegistry.NOOP) {
            return;
        }
        EchoNativeLoadStatus status = registry.registerInputBinding(surfaceType, action, binding);
        if (status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            debug("Native Loader client input binding " + surfaceType + ":" + action + " rejected with " + status + ".");
        }
    }

    public static void publishLifecycleEvent(
            String surfaceType,
            String phase,
            String action,
            Map<String, Object> metadata
    ) {
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        if (registry == EchoNativeClientRouteRegistry.NOOP) {
            return;
        }
        EchoNativeLoadStatus status = registry.publishLifecycleEvent(
                new NativeClientSurfaceLifecycleEvent(surfaceType, phase, action, metadata));
        if (status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            debug("Native Loader client lifecycle event " + surfaceType + ":" + phase + " rejected with " + status + ".");
        }
    }

    private static void registerRoute(EchoNativeClientRouteRegistry registry, RouteSurface surface) {
        EchoNativeLoadStatus status = registry.registerRoute(
                surface.moduleId(),
                surface.surfaceId(),
                surface.surfaceType(),
                Map.of(
                        "nativeSurfaceImplementationClass", surface.implementationClass(),
                        "nativeScreenBridgeClass", surface.bridgeClass(),
                        "source", "ashfall_windowed_native_client",
                        "productSurface", productSurfaceMetadata(surface)),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "releaseClientRouteTrusted", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry",
                        "visibleRuntimePath", true,
                        "defaultMountedProductSurface", true,
                        "productSurface", productSurfaceMetadata(surface)),
                true);
        if (status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            debug("Native Loader client route registry rejected " + surface.surfaceId() + " with " + status + ".");
        }
    }

    private static void registerLifecycle(EchoNativeClientRouteRegistry registry, RouteSurface surface) {
        EchoNativeLoadStatus status = registry.registerLifecycle(
                surface.surfaceType(),
                lifecycle(surface));
        if (status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            debug("Native Loader client route lifecycle rejected " + surface.surfaceType() + " with " + status + ".");
        }
    }

    private static void registerActions(EchoNativeClientRouteRegistry registry, RouteSurface surface) {
        if (surface.actions().isEmpty()) {
            return;
        }
        EchoNativeLoadStatus status = registry.registerActions(surface.surfaceType(), surface.actions());
        if (status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            debug("Native Loader client route actions rejected " + surface.surfaceType() + " with " + status + ".");
        }
    }

    private static void registerHandler(
            EchoNativeClientRouteRegistry registry,
            String surfaceType,
            SurfaceDispatcher dispatcher
    ) {
        EchoNativeLoadStatus status = registry.registerActionHandler(
                surfaceType,
                HANDLER_ID,
                context -> dispatcher.dispatch(context.surfaceType(), context.actionId(), context.action()));
        if (status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            debug("Native Loader client route handler rejected " + surfaceType + " with " + status + ".");
        }
    }

    private static void debug(String message) {
        LOGGER.log(System.Logger.Level.DEBUG, message);
    }

    private static NativeClientSurfaceLifecycle lifecycle(RouteSurface surface) {
        String surfaceType = surface.surfaceType();
        boolean render = switch (surfaceType) {
            case "main_menu", "loading_screen", "hud", "client_overlay", "lens", "holomap" -> true;
            default -> false;
        };
        boolean screen = switch (surfaceType) {
            case "main_menu", "loading_screen", "terminal", "index", "lens", "holomap" -> true;
            default -> false;
        };
        boolean input = !surface.actions().isEmpty() || switch (surfaceType) {
            case "main_menu", "terminal", "index", "lens", "holomap", "client_overlay" -> true;
            default -> false;
        };
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "ashfall_product_default_surface_catalog");
        metadata.put("productProfile", "echoashfallprotocol:ashfall_native_product");
        metadata.put("visibleRuntimePath", true);
        metadata.put("defaultMountedProductSurface", true);
        metadata.put("moduleId", surface.moduleId());
        metadata.put("surfaceId", surface.surfaceId());
        metadata.put("productSurface", productSurfaceMetadata(surface));
        metadata.put("implementationClass", surface.implementationClass());
        metadata.put("bridgeClass", surface.bridgeClass());
        metadata.put("actionIds", List.copyOf(surface.actions().keySet()));
        metadata.put("dispatchable", surface.dispatchable());
        metadata.put("nativeClientLifecycleSdk", "echo-native-client-route-registry");
        return new NativeClientSurfaceLifecycle(
                surfaceType,
                render,
                screen,
                input,
                true,
                true,
                render ? List.of("mount", "frame_begin", "render", "frame_end", "unmount") : List.of(),
                screen ? List.of("mount", "open", "focus", "fallback", "unavailable", "close", "unmount") : List.of(),
                input ? List.of("bind", "focus", "key", "mouse", "action", "fallback", "unavailable", "conflict_report") : List.of(),
                Map.copyOf(metadata));
    }

    private static Map<String, Map<String, Object>> actions(Object... entries) {
        if (entries == null || entries.length == 0) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            Object key = entries[index];
            Object value = entries[index + 1];
            if (key != null && value instanceof Map<?, ?> action) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedAction = (Map<String, Object>) action;
                result.put(String.valueOf(key), Map.copyOf(typedAction));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> action(Object... entries) {
        if (entries == null || entries.length == 0) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            Object key = entries[index];
            if (key != null) {
                result.put(String.valueOf(key), entries[index + 1]);
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Object> productSurfaceMetadata(RouteSurface surface) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productProfile", "echoashfallprotocol:ashfall_native_product");
        metadata.put("surfaceId", surface.surfaceId());
        metadata.put("surfaceType", surface.surfaceType());
        metadata.put("visibleByDefault", true);
        metadata.put("mountedByDefault", true);
        metadata.put("visibleRuntimePath", true);
        metadata.put("releaseSurface", true);
        metadata.put("ownerClass", surface.implementationClass());
        metadata.put("bridgeClass", surface.bridgeClass());
        metadata.put("role", productSurfaceRole(surface.surfaceType(), surface.surfaceId()));
        if ("main_menu".equals(surface.surfaceType()) || "loading_screen".equals(surface.surfaceType())) {
            metadata.put("productWorldStartupSurface", true);
            metadata.put("productWorldPreset", "echoashfallprotocol:ashfall_wasteland");
            metadata.put("productWorldFolder", "echo_native_ashfall_wasteland");
            metadata.put("productDatapack", "echo-native-ashfall-datapack.zip");
            metadata.put("productResourcePack", "echoashfallprotocol:ashfall_client_resources");
            metadata.put("oldVanillaSaveGuard", true);
            metadata.put("requiredWorldResources", List.of(
                    "echoashfallprotocol:ashfall_worldgen_datapack",
                    "echoashfallprotocol:ashfall_client_resources",
                    "echoashfallprotocol:ashfall_wasteland",
                    "minecraft:normal",
                    "echoashfallprotocol:wasteland_overworld_noise_settings",
                    "echoashfallprotocol:wasteland_biomes",
                    "echoashfallprotocol:wasteland_structures",
                    "echoashfallprotocol:wasteland_worldgen_tags"));
        }
        if ("hud".equals(surface.surfaceType()) || "client_overlay".equals(surface.surfaceType())) {
            metadata.put("hudDataSurface", true);
            metadata.put("runtimeStateRequired", List.of(
                    "ashfall_survival_state",
                    "hazard_meters",
                    "mission_tracker",
                    "weather_radiation_cold_readouts"));
        }
        if ("terminal".equals(surface.surfaceType())
                || "index".equals(surface.surfaceType())
                || "lens".equals(surface.surfaceType())
                || "holomap".equals(surface.surfaceType())) {
            metadata.put("moduleUxSurface", true);
            metadata.put("nativeActionDispatchRequired", true);
        }
        return Map.copyOf(metadata);
    }

    private static String productSurfaceRole(String surfaceType, String surfaceId) {
        return switch (surfaceType) {
            case "main_menu" -> "product_world_create_open_and_profile_status";
            case "loading_screen" -> "native_product_loading_progress";
            case "hud" -> "ashfall_survival_hud";
            case "client_overlay" -> "ashfall_status_overlay";
            case "terminal" -> "terminal_command_pages_and_actions";
            case "index" -> "index_inventory_recipe_source_overlay";
            case "lens" -> "lens_scanner_target_overlay";
            case "holomap" -> surfaceId.endsWith("fullscreen_map") || surfaceId.endsWith("fullscreen_handoff")
                    ? "holomap_fullscreen_navigation"
                    : "holomap_minimap_layers";
            default -> "native_client_surface";
        };
    }

    @FunctionalInterface
    public interface SurfaceDispatcher {
        boolean dispatch(String surfaceType, String action, Map<String, Object> actionMetadata);
    }

    private record RouteSurface(
            String moduleId,
            String surfaceId,
            String surfaceType,
            String implementationClass,
            String bridgeClass,
            Map<String, Map<String, Object>> actions,
            boolean dispatchable
    ) {
    }
}
