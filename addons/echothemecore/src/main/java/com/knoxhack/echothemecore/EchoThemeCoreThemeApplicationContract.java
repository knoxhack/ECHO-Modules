package com.knoxhack.echothemecore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoThemeCoreThemeApplicationContract {
    public static final String MODULE_ID = "echothemecore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echothemecore:themes/echo_platform_theme_application";
    public static final String REFERENCE_THEME_ID = "echothemecore:echo_platform";
    public static final String REFERENCE_SURFACE_ID = "echocore:native_hub";

    public Map<String, Object> execute(String requestedThemeId, String surfaceId, String runtime) {
        String selectedThemeId = normalizeThemeId(requestedThemeId);
        String selectedSurfaceId = normalizeText(surfaceId, REFERENCE_SURFACE_ID);
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        application.put("service", "echothemecore:theme_application_service");
        application.put("themeApplicationExecuted", true);
        application.put("requestedThemeId", normalizeText(requestedThemeId, REFERENCE_THEME_ID));
        application.put("selectedThemeId", selectedThemeId);
        application.put("surfaceId", selectedSurfaceId);
        application.put("runtime", normalizeText(runtime, "echo_native"));
        application.put("selectedTheme", selectedTheme());
        application.put("colorTokens", colorTokens());
        application.put("textureTokens", textureTokens());
        application.put("renderTokens", renderTokens());
        application.put("layoutTokens", layoutTokens());
        application.put("surfaceAssets", surfaceAssets());
        application.put("soundBindings", soundBindings());
        application.put("diagnostics", List.of(
                "theme.catalog.public_theme_selected",
                "theme.tokens.resolved",
                "theme.surface_assets.bound",
                "theme.native_loader_surface.bound",
                "theme.standalone_fallback.enabled"
        ));
        application.put("referenceBehavior", "themecore_resolves_echo_platform_theme_application");
        return Map.copyOf(application);
    }

    public boolean referenceApplicationPassed(Map<String, Object> application) {
        return Boolean.TRUE.equals(application.get("themeApplicationExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(application.get("adapterCoreContract"))
                && REFERENCE_THEME_ID.equals(application.get("selectedThemeId"))
                && String.valueOf(application.get("selectedTheme")).contains("replacementLevel=full")
                && String.valueOf(application.get("selectedTheme")).contains("standaloneFallback=true")
                && String.valueOf(application.get("colorTokens")).contains("accent.primary=#69D7FF")
                && String.valueOf(application.get("colorTokens")).contains("state.warning=#FFD166")
                && String.valueOf(application.get("textureTokens")).contains("terminal.panel=echothemecore:textures/gui/themes/cyberglass/glass_panel.png")
                && String.valueOf(application.get("textureTokens")).contains("holomap.marker.hazard=echothemecore:textures/gui/themes/cyberglass/marker_hazard.png")
                && String.valueOf(application.get("renderTokens")).contains("render.glow_intensity=0.88")
                && String.valueOf(application.get("layoutTokens")).contains("tokens.min_button_height=28")
                && String.valueOf(application.get("surfaceAssets")).contains("surface=terminal")
                && String.valueOf(application.get("surfaceAssets")).contains("surface=native_loader_hub")
                && String.valueOf(application.get("diagnostics")).contains("theme.surface_assets.bound");
    }

    private static Map<String, Object> selectedTheme() {
        Map<String, Object> theme = new LinkedHashMap<>();
        theme.put("id", REFERENCE_THEME_ID);
        theme.put("displayName", "ECHO Platform");
        theme.put("description", "Blue console platform interface shared by Native Loader and Standalone runtime surfaces.");
        theme.put("family", "echo_platform");
        theme.put("publicTheme", true);
        theme.put("cycleOrder", 0);
        theme.put("replacementLevel", "full");
        theme.put("packTheme", "echo_platform_blue_console");
        theme.put("density", "compact");
        theme.put("standaloneFallback", true);
        theme.put("moduleTags", List.of(
                "terminal",
                "signalos",
                "index",
                "holomap",
                "lens",
                "screencore",
                "rendercore",
                "soundcore",
                "vanilla_ui",
                "hud",
                "loading",
                "menu",
                "item_icon",
                "native_loader",
                "standalone",
                "platform"
        ));
        return Map.copyOf(theme);
    }

    private static Map<String, Object> colorTokens() {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("accent.primary", "#69D7FF");
        tokens.put("accent.secondary", "#2B8CFF");
        tokens.put("background.primary", "#030815");
        tokens.put("panel.primary", "#071426E6");
        tokens.put("panel.secondary", "#0B1E38CC");
        tokens.put("text.primary", "#EAF4FF");
        tokens.put("text.muted", "#8AA9C4");
        tokens.put("state.ready", "#52F2B8");
        tokens.put("state.warning", "#FFD166");
        tokens.put("state.danger", "#FF5E7A");
        tokens.put("border.selected", "#9BE7FF");
        return Map.copyOf(tokens);
    }

    private static Map<String, Object> textureTokens() {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("terminal.panel", texture("glass_panel"));
        tokens.put("terminal.tab", texture("tab"));
        tokens.put("terminal.tab.active", texture("tab_active"));
        tokens.put("terminal.button", texture("glass_button"));
        tokens.put("terminal.icon", texture("icons/icon_terminal"));
        tokens.put("index.panel", texture("index_panel"));
        tokens.put("index.card.selected", texture("index_card_selected"));
        tokens.put("holomap.grid", texture("holomap_grid"));
        tokens.put("holomap.marker.hazard", texture("marker_hazard"));
        tokens.put("lens.scan_ring", texture("lens_scan_ring"));
        tokens.put("screencore.surface.base", texture("screencore/surface_base"));
        tokens.put("hud.hotbar_frame", texture("hud/hotbar_frame"));
        tokens.put("loading.progress_bar", texture("loading/progress_bar"));
        tokens.put("menu.pause_panel", texture("menu/pause_panel"));
        tokens.put("item_icon.mission_marker", texture("item_icon/mission_marker"));
        return Map.copyOf(tokens);
    }

    private static Map<String, Object> renderTokens() {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("render.hologram_color", "#69D7FF");
        tokens.put("render.warning_glow_color", "#FFD166");
        tokens.put("render.success_glow_color", "#52F2B8");
        tokens.put("render.glow_intensity", 0.88D);
        tokens.put("render.hologram_opacity", 0.70D);
        tokens.put("render.particle_intensity", 0.58D);
        tokens.put("render.animation_intensity", 0.70D);
        tokens.put("render.overlay_style", "ECHO_PLATFORM_BLUE_CONSOLE");
        tokens.put("render.transition_style", "GLASS_FADE");
        return Map.copyOf(tokens);
    }

    private static Map<String, Object> layoutTokens() {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("tokens.safe_area_margin", 14);
        tokens.put("tokens.panel_radius", 4);
        tokens.put("tokens.card_radius", 4);
        tokens.put("tokens.button_radius", 4);
        tokens.put("tokens.min_button_height", 28);
        tokens.put("tokens.min_list_row_height", 44);
        tokens.put("tokens.min_text_contrast", 4.5D);
        tokens.put("tokens.animation.enter_ms", 110);
        tokens.put("tokens.animation.exit_ms", 90);
        tokens.put("tokens.animation.transition_ms", 170);
        return Map.copyOf(tokens);
    }

    private static List<Map<String, Object>> surfaceAssets() {
        return List.of(
                surface("terminal", texture("glass_panel"), texture("status_chip"), "accent.primary"),
                surface("index", texture("index_panel"), texture("index_status_chip"), "state.ready"),
                surface("holomap", texture("holomap_panel"), texture("marker_hazard"), "state.warning"),
                surface("lens", texture("lens_scan_ring"), texture("lens_warning_overlay"), "state.danger"),
                surface("screencore", texture("screencore/surface_base"), texture("screencore/focus_ring"), "border.selected"),
                surface("native_loader_hub", texture("glass_panel"), texture("hologram_overlay"), "accent.primary")
        );
    }

    private static Map<String, Object> soundBindings() {
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("ui.click", "echosoundcore:ui.terminal.select");
        bindings.put("ui.error", "echosoundcore:ui.terminal.error");
        bindings.put("ui.open", "echosoundcore:ui.terminal.open");
        bindings.put("stinger.warning", "echosoundcore:ui.terminal.warning");
        return Map.copyOf(bindings);
    }

    private static Map<String, Object> surface(String surface, String panelTexture, String accentTexture, String accentToken) {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("surface", surface);
        binding.put("panelTexture", panelTexture);
        binding.put("accentTexture", accentTexture);
        binding.put("accentToken", accentToken);
        binding.put("themeId", REFERENCE_THEME_ID);
        return Map.copyOf(binding);
    }

    private static String texture(String path) {
        return "echothemecore:textures/gui/themes/cyberglass/" + path + ".png";
    }

    private static String normalizeThemeId(String themeId) {
        String normalized = normalizeText(themeId, REFERENCE_THEME_ID);
        return REFERENCE_THEME_ID.equals(normalized)
                || "echo_platform".equals(normalized)
                || "legacy_loader_platform".equals(normalized)
                || "native_loader_platform".equals(normalized)
                || "standalone_echo_platform".equals(normalized)
                || "echo_platform_blue_console".equals(normalized)
                ? REFERENCE_THEME_ID
                : normalized;
    }

    private static String normalizeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }
}
