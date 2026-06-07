package com.knoxhack.echoscreencore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoScreenCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
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
        EchoNativeActivationSurfaceRegistrar.ready(context);
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover ScreenCore action, binding, component, layout, markup, and theme bridge contracts.")
                .phase("register_screen_contracts", "Record screen contracts before native screen execution.")
                .phase("attach_screen_events", "Record markup reload, screen open, input route, and theme resolve hooks.")
                .phase("ready", "Expose ScreenCore as the native ECHO UI framework provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("screen_surface", "echoscreencore:native_screen_host", "Native ScreenCore screen host surface contract.")
                .register("screen_action", "echoscreencore:actions", "Screen action contract.")
                .register("screen_binding", "echoscreencore:bindings", "Screen binding contract.")
                .register("screen_component", "echoscreencore:components", "Screen component contract.")
                .register("screen_layout", "echoscreencore:layouts", "Screen layout contract.")
                .register("screen_markup", "echoscreencore:markup", "Screen markup contract.")
                .register("theme_bridge", "echoscreencore:theme_bridge", "Theme bridge contract.")
                .register("data_provider", "echoscreencore:data_provider", "Screen data provider contract.")
                .register("style", "echoscreencore:style", "Screen style contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("data.reload", "ScreenMarkupReloaders.register", "Attach screen markup reloaders.")
                .hook("client.screen.open", "ScreenOpenBridge.open", "Prepare screen open flow without resolving client classes.")
                .hook("input.route", "ScreenInputBridge.route", "Prepare input routing contract.")
                .hook("theme.resolve", "ScreenThemeBridge.resolve", "Prepare theme bridge resolution.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("screen_safe_ui", "echoscreencore:screen_data_service", "screen_data",
                        "Keeps screen markup, components, actions, bindings, and layouts ready for safe native UI surfaces.",
                        "screen.markup", "screen.components", "screen.actions", "screen.bindings", "screen.layouts")
                .surfaceService("screen_safe_ui", "echoscreencore:screen_theme_service", "screen_theme",
                        "Keeps theme bridge state ready for HoloMap, Wiki, Lens, HUD, and terminal surfaces.",
                        "screen.theme_bridge", "theme.ui_skins", "hud.widgets");
        Map<String, Object> screenComposition = EchoScreenCoreCompositionContract.executeReferenceComposition(
                EchoScreenCoreCompositionContract.REFERENCE_SCREEN_ID,
                EchoScreenCoreCompositionContract.REFERENCE_ACTION_ID);
        boolean screenCompositionPassed = EchoScreenCoreCompositionContract.referenceCompositionPassed(screenComposition);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "screencore_native_composition_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("logicalRegistrationCount", 9);
        result.put("eventHookCount", 4);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "screen.actions",
                "screen.native_host",
                "screen.bindings",
                "screen.components",
                "screen.contracts",
                "screen.layouts",
                "screen.markup",
                "screen.theme_bridge",
                EchoScreenCoreCompositionContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("screenComposition", screenComposition);
        result.put("screenCompositionExecuted", screenCompositionPassed);
        result.put("requiresScreenBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", screenCompositionPassed);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "screen_surface_projection");
        result.put("summary", "ScreenCore native contract registered and executed the AdapterCore field-ops screen composition service.");
        return result;
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echoscreencore:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echoscreencore:welcome_surface"));
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }

    private static final String MODULE_ID = "echoscreencore";
}
