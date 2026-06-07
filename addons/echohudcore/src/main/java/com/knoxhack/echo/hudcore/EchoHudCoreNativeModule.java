package com.knoxhack.echo.hudcore;

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

public final class EchoHudCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
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
        registerNativeClientRoutesFromModule(context);
        EchoNativeActivationSurfaceRegistrar.ready(context);
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover HUD widget, mission tracker, hazard meter, and screen-safe-area contracts.")
                .phase("register_hud_contracts", "Record HUD widgets and safe-area contracts before native overlay execution.")
                .phase("attach_hud_events", "Record HUD update and packet-consumer hooks.")
                .phase("ready", "Expose HUDCore as the native mission, hazard, compass, and field-state readout surface.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("hud", "echohudcore:native_hud", "Native HUD overlay surface contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                        "actions", Map.of(
                                "hud.render", Map.of("kind", "hud_render"),
                                "hud.update_snapshot", Map.of("kind", "hud_state_update"),
                                "native_loader.overlay_focus", Map.of("kind", "hud_overlay_focus")
                        )))
                .register("hud_widget", "echohudcore:mission_tracker", "Mission tracker HUD widget contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                        "actions", Map.of(
                                "hud.mission_tracker.render", Map.of("kind", "hud_widget_render", "widget", "mission_tracker")
                        )))
                .register("hud_widget", "echohudcore:hazard_readout", "Hazard/weather readout HUD widget contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                        "actions", Map.of(
                                "hud.hazard_readout.render", Map.of("kind", "hud_widget_render", "widget", "hazard_readout")
                        )))
                .register("hud_widget", "echohudcore:compass_indicator", "Compass indicator HUD widget contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                        "actions", Map.of(
                                "hud.compass_indicator.render", Map.of("kind", "hud_widget_render", "widget", "compass_indicator")
                        )))
                .register("hud_layout", "echohudcore:screen_safe_area", "Screen-safe HUD anchor contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                        "actions", Map.of(
                                "hud.screen_safe_area.resolve", Map.of("kind", "hud_layout_resolve")
                        )))
                .register("service", "echohudcore:hud_service", "HUD runtime service contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("hud.packet", "EchoHudCoreNativeModule.consumeRuntimePackets", "Consume AdapterCore HUD runtime packets.")
                .hook("hud.layout", "EchoScreenSafeArea", "Resolve screen-safe HUD anchors without client runtime access.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("ui_hud_screen_safe", "echohudcore:hud_service", "hud_runtime",
                        "Keeps mission tracker, hazard readout, compass, and safe-area contracts ready for AdapterCore packets.",
                        "hud.mission_tracker", "hud.hazard_readout", "hud.screen_safe");
        Map<String, Object> hudSnapshot = EchoHudSnapshotContract.executeReferenceSnapshot(
                EchoHudSnapshotContract.REFERENCE_MISSION_ID,
                EchoHudSnapshotContract.REFERENCE_HAZARD_ID);
        boolean hudSnapshotPassed = EchoHudSnapshotContract.referenceSnapshotPassed(hudSnapshot);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "hudcore_native_client_surface_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("logicalRegistrationCount", 6);
        result.put("eventHookCount", 2);
        result.put("approvedNativeServiceCount", 1);
        result.put("registeredFeatureContracts", List.of(
                "hud.hazard_readout",
                "hud.mission_tracker",
                "hud.screen_safe",
                EchoHudSnapshotContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("hudSnapshot", hudSnapshot);
        result.put("hudSnapshotExecuted", hudSnapshotPassed);
        result.put("requiresHudBridge", true);
        result.put("nativeHudClientSurface", "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay");
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("nativeClientRouteRegistrarClass", "com.knoxhack.echo.hudcore.EchoHudCoreClient");
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", hudSnapshotPassed);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "hud_surface_projection");
        result.put("summary", "HUDCore native contract registered the mission, hazard, compass, and screen-safe HUD client surface.");
        return result;
    }

    private static void registerNativeClientRoutesFromModule(EchoNativeModuleLoadContext context) {
        try {
            Object registered = Class.forName("com.knoxhack.echo.hudcore.EchoHudCoreClient")
                    .getMethod("ensureNativeClientRoutesRegisteredForNativeLoader")
                    .invoke(null);
            boolean mutated = Boolean.TRUE.equals(registered);
            context.attribute("nativeClientRouteRegistrarClass", "com.knoxhack.echo.hudcore.EchoHudCoreClient");
            context.attribute("nativeClientRoutesRegistered", mutated);
            if (mutated) {
                context.recordMutation("client_routes", "register", "echohudcore:native_client_routes",
                        EchoNativeLoadStatus.MUTATED);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeClientRoutesRegistered", false);
            context.attribute("nativeClientRouteRegistrarFailure", exception.getClass().getSimpleName());
        }
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return consumeRuntimePackets(runtimePacketBindings);
    }

    public Map<String, Object> consumeRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echohudcore:runtime_packet_consumers",
                runtimePacketBindings,
                List.of(
                        "echohudcore:mission_tracker",
                        "echohudcore:hazard_readout"));
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }

    private static final String MODULE_ID = "echohudcore";
}
