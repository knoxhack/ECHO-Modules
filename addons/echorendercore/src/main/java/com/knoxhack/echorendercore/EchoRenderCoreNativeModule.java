package com.knoxhack.echorendercore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRenderCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> previewFrame = EchoRenderCorePreviewFrameContract.executeReferencePreview(
                context.getOrDefault("packId", "unknown")
        );
        boolean previewFramePassed = EchoRenderCorePreviewFrameContract.referencePreviewPassed(previewFrame);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover RenderCore profile, particle, animation, preview, and diagnostics contracts.")
                .phase("register_render_contracts", "Record visual profile contracts before client renderer execution.")
                .phase("attach_render_events", "Record reload, render-planning, and diagnostics hooks.")
                .phase("execute_preview_frame", "Execute deterministic preview frame planning through AdapterCore.")
                .phase("ready", "Expose RenderCore as the native visual contract provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("render_profile", "echorendercore:profiles", "Render profile contract.")
                .register("particle_profile", "echorendercore:particles", "Particle profile contract.")
                .register("animation_profile", "echorendercore:animations", "Animation profile contract.")
                .register("screen_chrome", "echorendercore:screen_chrome", "Screen chrome contract.")
                .register("preview", "echorendercore:preview", "Preview rendering contract.")
                .register("creator_export", "echorendercore:creator_exports", "Creator export visual contract.")
                .register("diagnostics", "echorendercore:diagnostics", "Render diagnostics contract.")
                .register("validation", "echorendercore:validation", "Render validation contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("data.reload", "RenderProfileReloaders.register", "Attach render profile reloaders.")
                .hook("client.render.plan", "RenderPlanBridge.plan", "Prepare render plan without resolving client classes.")
                .hook("diagnostics.snapshot", "RenderDiagnostics.snapshot", "Prepare render diagnostics snapshot.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "rendercore_native_preview_frame_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("previewFrame", previewFrame);
        result.put("previewFrameExecuted", previewFramePassed);
        result.put("logicalRegistrationCount", 8);
        result.put("eventHookCount", 3);
        result.put("registeredFeatureContracts", List.of(
                "render.animation_profiles",
                "render.creator_exports",
                "render.diagnostics",
                "render.particle_profiles",
                "render.preview",
                "render.profiles",
                "render.screen_chrome",
                "render.validation",
                EchoRenderCorePreviewFrameContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresRenderBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", previewFramePassed);
        result.put("transformsPerformed", false);
        result.put("summary", "RenderCore native contract registered visual surfaces and executed the AdapterCore preview frame service.");
        return result;
    }

    private static final String MODULE_ID = "echorendercore";
}
