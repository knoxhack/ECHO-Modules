package com.knoxhack.echolens;

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

public final class EchoLensNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
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
                .phase("discover", "Discover Lens scanner, inspection, overlay, and mission signal contracts.")
                .phase("register_scanner_contracts", "Record scanner providers and payload contracts before native scan execution.")
                .phase("attach_lens_events", "Record network, command, client scan, and mission hooks.")
                .phase("ready", "Expose Lens as the native Ashfall field scanner.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("lens", "echolens:field_lens", "Lens field scanner UI surface contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "actions", Map.of(
                                "lens.deep_scan", Map.of(
                                        "kind", "hud_scan",
                                        "mode", "deep"),
                                "lens.index_recipe", Map.of(
                                        "kind", "target_index",
                                        "recipeMode", "recipes"),
                                "lens.index_usage", Map.of(
                                        "kind", "target_index",
                                        "recipeMode", "usages"),
                                "lens.track_in_index", Map.of(
                                        "kind", "target_index",
                                        "recipeMode", "track")
                        )))
                .register("client_overlay", "echolens:lens_overlay", "Lens scan reticle and inspection overlay contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "nativeScreenBridgeClass", "com.knoxhack.echolens.client.LensHudOverlay",
                        "actions", Map.of(
                                "lens.overlay.render", Map.of("kind", "overlay_render"),
                                "lens.overlay.scan_target", Map.of("kind", "hud_scan", "mode", "target")
                        )))
                .register("scanner", "echolens:block", "Block scanner contract.")
                .register("scanner", "echolens:entity", "Entity scanner contract.")
                .register("scanner", "echolens:fluid", "Fluid scanner contract.")
                .register("scanner", "echolens:inventory", "Inventory scanner contract.")
                .register("scanner", "echolens:machine", "Machine scanner contract.")
                .register("network_payload", "echolens:scan_request", "Lens scan request payload contract.")
                .register("network_payload", "echolens:scan_response", "Lens scan response payload contract.")
                .register("service", "echolens:inspection_service", "Lens inspection service contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoLens.commonSetup", "Attach Lens scanner providers.")
                .hook("network.payload_register", "LensPayloads.register", "Prepare scanner packet contracts.")
                .hook("commands.register", "LensCommands.register", "Expose Lens commands when native command bridge exists.")
                .hook("client.overlay.render", "LensHudOverlay.render", "Render the Lens inspection overlay through the native client bridge.")
                .hook("client.scan", "LensScanBridge.scan", "Prepare client scan trigger without resolving runtime classes.")
                .hook("mission.signal", "LensMissionBridge.attach", "Prepare mission scan hooks.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("holomap_lens_codex_wiki", "echolens:inspection_service", "field_inspection",
                        "Keeps block, entity, fluid, inventory, and machine scanner providers ready for safe native scan requests.",
                        "lens.scanners")
                .surfaceService("holomap_lens_codex_wiki", "echolens:mission_signal_service", "mission_inspection",
                        "Keeps mission scan signal contracts ready for Ashfall objectives.",
                        "lens.scanners", "missions.objectives");
        Map<String, Object> fieldScan = EchoLensFieldScanContract.executeReferenceScan(
                EchoLensFieldScanContract.REFERENCE_TARGET_ID,
                EchoLensFieldScanContract.REFERENCE_SCAN_MODE);
        boolean fieldScanPassed = EchoLensFieldScanContract.referenceScanPassed(fieldScan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "lens_native_field_scan_active");
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
                "lens.scanners",
                "echolens:field_lens",
                "echolens:lens_overlay",
                EchoLensFieldScanContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("fieldScan", fieldScan);
        result.put("fieldScanExecuted", fieldScanPassed);
        result.put("requiresScannerBridge", true);
        result.put("requiresUiBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("nativeClientRouteRegistrarClass", "com.knoxhack.echolens.EchoLensClient");
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", fieldScanPassed);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "lens_scanner_surface_projection");
        result.put("summary", "Lens native contract registered field scanner providers and executed the AdapterCore field inspection service.");
        return result;
    }

    private static void registerNativeClientRoutesFromModule(EchoNativeModuleLoadContext context) {
        try {
            Object registered = Class.forName("com.knoxhack.echolens.EchoLensClient")
                    .getMethod("ensureNativeClientRoutesRegisteredForNativeLoader")
                    .invoke(null);
            boolean mutated = Boolean.TRUE.equals(registered);
            context.attribute("nativeClientRouteRegistrarClass", "com.knoxhack.echolens.EchoLensClient");
            context.attribute("nativeClientRoutesRegistered", mutated);
            if (mutated) {
                context.recordMutation("client_routes", "register", "echolens:native_client_routes",
                        EchoNativeLoadStatus.MUTATED);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeClientRoutesRegistered", false);
            context.attribute("nativeClientRouteRegistrarFailure", exception.getClass().getSimpleName());
        }
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echolens:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echolens:opening_route_scan"));
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }

    private static final String MODULE_ID = "echolens";
}
