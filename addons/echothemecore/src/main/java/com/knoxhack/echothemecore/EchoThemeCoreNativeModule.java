package com.knoxhack.echothemecore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoThemeCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover ThemeCore token, skin, palette, render profile, and asset-kit contracts.")
                .phase("register_theme_contracts", "Record theme contracts before UI skin application.")
                .phase("attach_theme_events", "Record theme reload and UI apply hooks.")
                .phase("ready", "Expose ThemeCore as the native ECHO visual theme provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("theme", "echothemecore:echo_platform", "Unified ECHO Platform theme contract.")
                .register("ui_surface", "echothemecore:echo_platform_theme_surface", "Native Loader ECHO Platform theme surface.")
                .register("ui_overlay", "echothemecore:echo_platform_blue_console_overlay", "Native Loader blue console theme overlay.")
                .register("main_menu", "echothemecore:echo_platform_main_menu", "Native Loader custom main-menu theme contract.")
                .register("loading_screen", "echothemecore:echo_platform_loading", "Native Loader custom loading-screen theme contract.")
                .register("theme", "echothemecore:cyberglass", "Cyberglass theme contract.")
                .register("theme", "echothemecore:nexus", "Nexus theme contract.")
                .register("theme", "echothemecore:ashfall", "Ashfall theme contract.")
                .register("theme_tokens", "echothemecore:tokens", "Theme token contract.")
                .register("ui_skin", "echothemecore:ui_skins", "UI skin contract.")
                .register("render_profile", "echothemecore:render_profiles", "Theme render profile contract.")
                .register("asset_kit", "echothemecore:asset_kits", "Theme asset kit contract.")
                .register("block_palette", "echothemecore:block_palettes", "Theme block palette contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("data.reload", "ThemeReloaders.register", "Attach theme token and palette reloaders.")
                .hook("client.theme.apply", "ThemeApplyBridge.apply", "Prepare theme application without resolving client classes.")
                .hook("integration.optional", "ThemeIntegrationBridge.attach", "Prepare optional UI and render integrations.");
        EchoThemeCoreThemeApplicationContract themeApplicationContract = new EchoThemeCoreThemeApplicationContract();
        Map<String, Object> themeApplication = themeApplicationContract.execute(
                "echothemecore:echo_platform",
                EchoThemeCoreThemeApplicationContract.REFERENCE_SURFACE_ID,
                "echo_native");
        boolean themeApplicationPassed = themeApplicationContract.referenceApplicationPassed(themeApplication);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "themecore_native_theme_application_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", 13);
        result.put("eventHookCount", 3);
        result.put("adapterDomains", List.of("assets", "blocks", "rendering", "themes", "ui_screens", "native_loader_client"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("registeredFeatureContracts", List.of(
                EchoThemeCoreThemeApplicationContract.ADAPTERCORE_CONTRACT_ID,
                "theme.asset_kits",
                "theme.block_palettes",
                "theme.echo_platform",
                "theme.native_loader.echo_platform",
                "theme.native_loader.main_menu",
                "theme.native_loader.loading_screen",
                "theme.cyberglass",
                "theme.render_profiles",
                "theme.tokens",
                "theme.ui_skins",
                "ui.native_loader.theme_surface"
        ));
        result.put("themeApplication", themeApplication);
        result.put("themeApplicationExecuted", themeApplicationPassed);
        result.put("themeApplicationContract", EchoThemeCoreThemeApplicationContract.ADAPTERCORE_CONTRACT_ID);
        result.put("selectedThemeId", themeApplication.get("selectedThemeId"));
        result.put("nativeLoaderThemeSurfaceId", "echothemecore:echo_platform_theme_surface");
        result.put("nativeLoaderOverlayId", "echothemecore:echo_platform_blue_console_overlay");
        result.put("themeSurfaceAssetCount", ((List<?>) themeApplication.get("surfaceAssets")).size());
        result.put("themeTokenGroupsResolved", 4);
        result.put("requiresThemeBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", themeApplicationPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "ThemeCore native contract resolved and applied the ECHO Platform theme token, texture, render, and surface asset bundle through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echothemecore";
}
