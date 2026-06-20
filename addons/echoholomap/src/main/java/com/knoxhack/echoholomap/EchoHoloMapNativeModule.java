package com.knoxhack.echoholomap;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoHoloMapNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct_native_module_entrypoint");
        context.attribute("nativeEntrypointClass", getClass().getName());
        context.attribute("nativeModuleEntrypoint", true);
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerServices(
                context,
                this,
                activation(context),
                "native_module_entrypoint",
                "direct_native_module_entrypoint"
        );
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerContent(context, activation(context));
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        boolean commonRegistered = ensureCommonServicesRegisteredForNativeLoader(context);
        context.attribute("nativeCommonServicesRegistered", commonRegistered);
        context.attribute("nativeCommonServicesAlreadyRegistered", !commonRegistered);
        context.recordMutation(
                "platform_services",
                commonRegistered ? "register" : "already_registered",
                "echoholomap:common_services",
                commonRegistered ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.REGISTERED);
        registerNativeClientRoutesFromModule(context);
        EchoNativeActivationSurfaceRegistrar.ready(context);
    }

    private static boolean ensureCommonServicesRegisteredForNativeLoader(EchoNativeModuleLoadContext context) {
        String moduleClassName = EchoHoloMapNativeModule.class.getPackageName() + ".EchoHoloMap";
        try {
            Object result = Class.forName(moduleClassName)
                    .getMethod("ensureCommonServicesRegisteredForNativeLoader")
                    .invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeCommonServicesDeferred", true);
            context.attribute("nativeCommonServicesDeferredReason", exception.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover HoloMap terrain, waypoint, marker, and mission layer contracts.")
                .phase("register_map_layers", "Record map UI, payload, and saved-data contracts before native rendering.")
                .phase("attach_map_events", "Record network, command, tick, reload, and deathpoint hooks.")
                .phase("ready", "Expose HoloMap as the native Ashfall navigation layer.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("holomap", "echoholomap:fullscreen_map", "Fullscreen HoloMap surface contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                        "nativeScreenBridgeClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "actions", Map.ofEntries(
                                Map.entry("holomap.open", Map.of(
                                        "kind", "screen_bridge",
                                        "bridgeMethod", "openFullscreen")),
                                Map.entry("holomap.fullscreen.key", Map.of("kind", "fullscreen_key_input")),
                                Map.entry("holomap.fullscreen.mouse", Map.of("kind", "fullscreen_mouse_input")),
                                Map.entry("holomap.fullscreen.scroll", Map.of("kind", "fullscreen_scroll_input")),
                                Map.entry("holomap.sync", Map.of("kind", "fullscreen_command", "bridgeMethod", "sync")),
                                Map.entry("holomap.center", Map.of("kind", "fullscreen_command", "bridgeMethod", "center")),
                                Map.entry("holomap.toggle_markers", Map.of(
                                        "kind", "fullscreen_command",
                                        "bridgeMethod", "toggleMarkers")),
                                Map.entry("holomap.cycle_fields", Map.of(
                                        "kind", "fullscreen_command",
                                        "bridgeMethod", "cycleFields")),
                                Map.entry("holomap.toggle_waypoints", Map.of(
                                        "kind", "fullscreen_command",
                                        "bridgeMethod", "toggleWaypoints")),
                                Map.entry("holomap.select_entry", Map.of(
                                        "kind", "fullscreen_command",
                                        "bridgeMethod", "selectEntry")),
                                Map.entry("holomap.close", Map.of("kind", "fullscreen_command", "bridgeMethod", "close"))
                        )))
                .register("holomap", "echoholomap:minimap", "Minimap surface contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                        "actions", Map.of(
                                "holomap.minimap.render", Map.of("kind", "overlay_render"),
                                "holomap.toggle_minimap", Map.of(
                                        "kind", "overlay_command",
                                        "bridgeMethod", "toggle"),
                                "holomap.zoom_in", Map.of(
                                        "kind", "overlay_command",
                                        "bridgeMethod", "zoomIn"),
                                "holomap.zoom_out", Map.of(
                                        "kind", "overlay_command",
                                        "bridgeMethod", "zoomOut"),
                                "holomap.cycle_corner", Map.of(
                                        "kind", "overlay_command",
                                        "bridgeMethod", "cycleCorner")
                        )))
                .register("network_payload", "echoholomap:snapshot", "Map snapshot payload contract.")
                .register("network_payload", "echoholomap:request", "Map request payload contract.")
                .register("network_payload", "echoholomap:waypoint", "Waypoint payload contract.")
                .register("saved_data", "echoholomap:terrain_cache", "Terrain cache data contract.")
                .register("saved_data", "echoholomap:waypoints", "Waypoint data contract.")
                .register("service", "echoholomap:map_service", "Map service contract.")
                .register("integration", "echoholomap:worldcore", "WorldCore region layer integration.")
                .register("integration", "echoholomap:missioncore", "Mission marker integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoHoloMap.commonSetup", "Attach HoloMap services and integrations.")
                .hook("network.payload_register", "HoloMapPayloads.register", "Prepare map packet contracts.")
                .hook("commands.register", "HoloMapCommands.register", "Expose map commands when native command bridge exists.")
                .hook("player.tick", "HoloMapSync.tick", "Prepare map sync tick.")
                .hook("data.reload", "HoloMapLayerReloaders.register", "Attach map layer reloaders.")
                .hook("player.death", "DeathpointBridge.record", "Prepare deathpoint marker hook.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("holomap_lens_codex_wiki", "echoholomap:map_service", "navigation",
                        "Keeps terrain cache, waypoint, and mission-marker runtime state ready for native map surfaces.",
                        "holomap.layers")
                .surfaceService("holomap_lens_codex_wiki", "echoholomap:deathpoint_service", "recovery_navigation",
                        "Keeps deathpoint marker contracts ready for Recovery and player death hooks.",
                        "holomap.layers", "recovery.graves");
        Map<String, Object> routeSnapshot = EchoHoloMapRouteSnapshotContract.executeReferenceSnapshot(
                EchoHoloMapRouteSnapshotContract.REFERENCE_ROUTE_ID,
                EchoHoloMapRouteSnapshotContract.REFERENCE_REGION_ID);
        boolean routeSnapshotPassed = EchoHoloMapRouteSnapshotContract.referenceSnapshotPassed(routeSnapshot);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "holomap_native_route_snapshot_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("logicalRegistrationCount", 10);
        result.put("eventHookCount", 6);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "holomap.layers",
                EchoHoloMapRouteSnapshotContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("routeSnapshot", routeSnapshot);
        result.put("routeSnapshotExecuted", routeSnapshotPassed);
        result.put("requiresMapBridge", true);
        result.put("requiresUiBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("nativeClientRouteRegistrarClass", "com.knoxhack.echoholomap.EchoHoloMapClient");
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", routeSnapshotPassed);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "map_surface_projection");
        result.put("summary", "HoloMap native contract registered and executed the AdapterCore route-map snapshot service.");
        return result;
    }

    private static void registerNativeClientRoutesFromModule(EchoNativeModuleLoadContext context) {
        try {
            Object registered = Class.forName("com.knoxhack.echoholomap.EchoHoloMapClient")
                    .getMethod("ensureNativeClientRoutesRegisteredForNativeLoader")
                    .invoke(null);
            boolean mutated = Boolean.TRUE.equals(registered);
            context.attribute("nativeClientRouteRegistrarClass", "com.knoxhack.echoholomap.EchoHoloMapClient");
            context.attribute("nativeClientRoutesRegistered", mutated);
            if (mutated) {
                context.recordMutation("client_routes", "register", "echoholomap:native_client_routes",
                        EchoNativeLoadStatus.MUTATED);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeClientRoutesRegistered", false);
            context.attribute("nativeClientRouteRegistrarFailure", exception.getClass().getSimpleName());
        }
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echoholomap:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of(
                        "echoholomap:map_state_service",
                        "echoholomap:opening_recovery_layers"));
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }

    private static final String MODULE_ID = "echoholomap";
}
