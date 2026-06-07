package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.theme.BuiltinTerminalThemes;
import com.knoxhack.echoterminal.api.theme.TerminalTheme;
import com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Client-only terminal presentation defaults. These values are intentionally
 * additive and do not affect server-side mission or action authority.
 */
public final class TerminalClientOptions {
    private static final InterfaceDensity DEFAULT_INTERFACE_DENSITY = InterfaceDensity.COMFORTABLE;
    private static final CyberglassDensity DEFAULT_CYBERGLASS_DENSITY = CyberglassDensity.COMFORTABLE;
    private static final TerminalZoom DEFAULT_TERMINAL_ZOOM = TerminalZoom.ZOOM_90;
    private static final boolean DEFAULT_SCREEN_CORE_EXPERIMENTAL_TABS = true;

    public static NavigationStyle navigationStyle = NavigationStyle.APP_HUB;
    public static MissionView missionView = MissionView.GUIDED;
    public static InterfaceDensity interfaceDensity = DEFAULT_INTERFACE_DENSITY;
    public static TerminalZoom terminalZoom = DEFAULT_TERMINAL_ZOOM;
    public static VisualLevel visualLevel = VisualLevel.BALANCED;
    public static boolean reducedMotion = false;
    public static boolean highContrastMode = false;
    public static boolean reducedClutterMode = false;
    public static boolean largeTextMode = false;
    public static boolean simplifiedTerminalMode = false;
    public static boolean hideDebugInfo = true;
    public static boolean reduceGlow = true;
    public static boolean reduceGridNoise = true;
    public static boolean missionHudNotifications = true;
    public static boolean useScreenCore = true;
    public static boolean screenCoreMatchExistingLayout = true;
    public static boolean screenCoreDebug = false;
    public static boolean screenCoreExperimentalTabs = DEFAULT_SCREEN_CORE_EXPERIMENTAL_TABS;
    public static boolean useCyberglassScreenCoreTheme = true;
    public static CyberglassDensity cyberglassDensity = DEFAULT_CYBERGLASS_DENSITY;
    public static boolean cyberglassMotion = true;
    public static boolean cyberglassBackgroundEffects = true;
    public static float cyberglassGlowStrength = 0.75F;
    public static boolean cyberglassReduceVisualNoise = false;
    public static boolean cyberglassUseClassicLayout = false;
    private static final String CONFIG_FILE = "echoterminal-client.properties";
    private static final String THEME_KEY = "theme";
    private static final String NAVIGATION_STYLE_KEY = "navigationStyle";
    private static final String MISSION_VIEW_KEY = "missionView";
    private static final String INTERFACE_DENSITY_KEY = "interfaceDensity";
    private static final String TERMINAL_ZOOM_KEY = "terminalZoom";
    private static final String VISUAL_LEVEL_KEY = "visualLevel";
    private static final String REDUCED_MOTION_KEY = "reducedMotion";
    private static final String HIGH_CONTRAST_MODE_KEY = "highContrastMode";
    private static final String REDUCED_CLUTTER_MODE_KEY = "reducedClutterMode";
    private static final String LARGE_TEXT_MODE_KEY = "largeTextMode";
    private static final String SIMPLIFIED_TERMINAL_MODE_KEY = "simplifiedTerminalMode";
    private static final String HIDE_DEBUG_INFO_KEY = "hideDebugInfo";
    private static final String REDUCE_GLOW_KEY = "reduceGlow";
    private static final String REDUCE_GRID_NOISE_KEY = "reduceGridNoise";
    private static final String MISSION_HUD_NOTIFICATIONS_KEY = "missionHudNotifications";
    private static final String USE_SCREEN_CORE_KEY = "useScreenCore";
    private static final String SCREEN_CORE_MATCH_EXISTING_LAYOUT_KEY = "screenCoreMatchExistingLayout";
    private static final String SCREEN_CORE_DEBUG_KEY = "screenCoreDebug";
    private static final String SCREEN_CORE_EXPERIMENTAL_TABS_KEY = "screenCoreExperimentalTabs";
    private static final String USE_CYBERGLASS_SCREEN_CORE_THEME_KEY = "useCyberglassScreenCoreTheme";
    private static final String CYBERGLASS_DENSITY_KEY = "cyberglassDensity";
    private static final String CYBERGLASS_MOTION_KEY = "cyberglassMotion";
    private static final String CYBERGLASS_BACKGROUND_EFFECTS_KEY = "cyberglassBackgroundEffects";
    private static final String CYBERGLASS_GLOW_STRENGTH_KEY = "cyberglassGlowStrength";
    private static final String CYBERGLASS_REDUCE_VISUAL_NOISE_KEY = "cyberglassReduceVisualNoise";
    private static final String CYBERGLASS_USE_CLASSIC_LAYOUT_KEY = "cyberglassUseClassicLayout";
    private static final String[] CONFIG_KEYS = {
            THEME_KEY,
            NAVIGATION_STYLE_KEY,
            MISSION_VIEW_KEY,
            INTERFACE_DENSITY_KEY,
            TERMINAL_ZOOM_KEY,
            VISUAL_LEVEL_KEY,
            REDUCED_MOTION_KEY,
            HIGH_CONTRAST_MODE_KEY,
            REDUCED_CLUTTER_MODE_KEY,
            LARGE_TEXT_MODE_KEY,
            SIMPLIFIED_TERMINAL_MODE_KEY,
            HIDE_DEBUG_INFO_KEY,
            REDUCE_GLOW_KEY,
            REDUCE_GRID_NOISE_KEY,
            MISSION_HUD_NOTIFICATIONS_KEY,
            USE_SCREEN_CORE_KEY,
            SCREEN_CORE_MATCH_EXISTING_LAYOUT_KEY,
            SCREEN_CORE_DEBUG_KEY,
            SCREEN_CORE_EXPERIMENTAL_TABS_KEY,
            USE_CYBERGLASS_SCREEN_CORE_THEME_KEY,
            CYBERGLASS_DENSITY_KEY,
            CYBERGLASS_MOTION_KEY,
            CYBERGLASS_BACKGROUND_EFFECTS_KEY,
            CYBERGLASS_GLOW_STRENGTH_KEY,
            CYBERGLASS_REDUCE_VISUAL_NOISE_KEY,
            CYBERGLASS_USE_CLASSIC_LAYOUT_KEY
    };
    private static Identifier selectedTheme;
    private static boolean loaded;

    private TerminalClientOptions() {
    }

    public static boolean useSidebarHub() {
        NavigationStyle style = navigationStyle();
        return style == NavigationStyle.APP_HUB || style == NavigationStyle.SIDEBAR_HUB;
    }

    public static boolean useAppHub() {
        return navigationStyle() == NavigationStyle.APP_HUB;
    }

    public static NavigationStyle navigationStyle() {
        ensureLoaded();
        return navigationStyle;
    }

    public static boolean useVisualAssets() {
        ensureLoaded();
        return visualLevel != VisualLevel.MINIMAL && !reducedClutterMode && !simplifiedTerminalMode;
    }

    public static boolean reduceMotion() {
        ensureLoaded();
        return reducedMotion || visualLevel == VisualLevel.REDUCED_MOTION || reducedClutterMode || simplifiedTerminalMode;
    }

    public static boolean highContrastMode() {
        ensureLoaded();
        return highContrastMode;
    }

    public static boolean reducedClutterMode() {
        ensureLoaded();
        return reducedClutterMode || visualLevel == VisualLevel.MINIMAL || simplifiedTerminalMode;
    }

    public static boolean largeTextMode() {
        ensureLoaded();
        return largeTextMode;
    }

    public static boolean simplifiedTerminalMode() {
        ensureLoaded();
        return simplifiedTerminalMode;
    }

    public static boolean hideDebugInfo() {
        ensureLoaded();
        return hideDebugInfo;
    }

    public static boolean reduceGlow() {
        ensureLoaded();
        return reduceGlow || reducedClutterMode() || highContrastMode();
    }

    public static boolean reduceGridNoise() {
        ensureLoaded();
        return reduceGridNoise || reducedClutterMode();
    }

    public static boolean useScreenCore() {
        ensureLoaded();
        return useScreenCore;
    }

    public static boolean screenCoreMatchExistingLayout() {
        ensureLoaded();
        return screenCoreMatchExistingLayout;
    }

    public static boolean screenCoreDebug() {
        ensureLoaded();
        return screenCoreDebug;
    }

    public static boolean screenCoreExperimentalTabs() {
        ensureLoaded();
        return screenCoreExperimentalTabs;
    }

    public static boolean useCyberglassScreenCoreTheme() {
        ensureLoaded();
        return useCyberglassScreenCoreTheme;
    }

    public static CyberglassDensity cyberglassDensity() {
        ensureLoaded();
        return cyberglassDensity;
    }

    public static boolean cyberglassMotion() {
        ensureLoaded();
        return cyberglassMotion && !reduceMotion();
    }

    public static boolean cyberglassBackgroundEffects() {
        ensureLoaded();
        return cyberglassBackgroundEffects && !reduceGridNoise();
    }

    public static float cyberglassGlowStrength() {
        ensureLoaded();
        return Math.max(0.0F, Math.min(1.0F, cyberglassGlowStrength));
    }

    public static boolean cyberglassReduceVisualNoise() {
        ensureLoaded();
        return cyberglassReduceVisualNoise || reducedClutterMode();
    }

    public static boolean cyberglassUseClassicLayout() {
        ensureLoaded();
        return cyberglassUseClassicLayout;
    }

    public static boolean cyberglassActive() {
        ensureLoaded();
        return BuiltinTerminalThemes.CYBERGLASS.equals(selectedThemeId()) && !cyberglassUseClassicLayout;
    }

    public static boolean cyberglassCompact() {
        return cyberglassActive() && cyberglassDensity() == CyberglassDensity.COMPACT;
    }

    public static boolean cyberglassComfortable() {
        return cyberglassActive() && cyberglassDensity() == CyberglassDensity.COMFORTABLE;
    }

    public static boolean cyberglassCinematic() {
        return cyberglassActive() && cyberglassDensity() == CyberglassDensity.CINEMATIC;
    }

    public static TerminalTheme currentTheme() {
        ensureLoaded();
        return TerminalThemeRegistry.byId(selectedTheme);
    }

    public static InterfaceDensity interfaceDensity() {
        ensureLoaded();
        return interfaceDensity;
    }

    public static TerminalZoom terminalZoom() {
        ensureLoaded();
        return terminalZoom;
    }

    public static Identifier selectedThemeId() {
        ensureLoaded();
        return TerminalThemeRegistry.contains(selectedTheme)
                ? selectedTheme
                : TerminalThemeRegistry.defaultThemeId();
    }

    public static void selectTheme(Identifier themeId) {
        ensureLoaded();
        selectedTheme = TerminalThemeRegistry.contains(themeId)
                ? themeId
                : TerminalThemeRegistry.defaultThemeId();
        save();
    }

    public static void selectNavigationStyle(NavigationStyle style) {
        ensureLoaded();
        navigationStyle = style == null ? NavigationStyle.APP_HUB : style;
        save();
    }

    public static void selectMissionView(MissionView view) {
        ensureLoaded();
        missionView = normalizeMissionView(view);
        save();
    }

    public static MissionView normalizeMissionView(MissionView view) {
        return MissionView.GUIDED;
    }

    public static void selectInterfaceDensity(InterfaceDensity density) {
        ensureLoaded();
        interfaceDensity = density == null ? DEFAULT_INTERFACE_DENSITY : density;
        save();
    }

    public static void selectTerminalZoom(TerminalZoom zoom) {
        ensureLoaded();
        terminalZoom = zoom == null ? DEFAULT_TERMINAL_ZOOM : zoom;
        save();
    }

    public static void selectVisualLevel(VisualLevel level) {
        ensureLoaded();
        visualLevel = level == null ? VisualLevel.BALANCED : level;
        reducedMotion = visualLevel == VisualLevel.REDUCED_MOTION;
        save();
    }

    public static void setReducedMotion(boolean value) {
        ensureLoaded();
        reducedMotion = value;
        if (value && visualLevel != VisualLevel.REDUCED_MOTION) {
            visualLevel = VisualLevel.REDUCED_MOTION;
        } else if (!value && visualLevel == VisualLevel.REDUCED_MOTION) {
            visualLevel = VisualLevel.BALANCED;
        }
        save();
    }

    public static void setHighContrastMode(boolean value) {
        ensureLoaded();
        highContrastMode = value;
        save();
    }

    public static void setReducedClutterMode(boolean value) {
        ensureLoaded();
        reducedClutterMode = value;
        if (value) {
            reduceGlow = true;
            reduceGridNoise = true;
        }
        save();
    }

    public static void setLargeTextMode(boolean value) {
        ensureLoaded();
        largeTextMode = value;
        if (value && terminalZoom.percent() < 100) {
            terminalZoom = TerminalZoom.ZOOM_100;
        }
        save();
    }

    public static void setSimplifiedTerminalMode(boolean value) {
        ensureLoaded();
        simplifiedTerminalMode = value;
        if (value) {
            reducedClutterMode = true;
            reduceGlow = true;
            reduceGridNoise = true;
            hideDebugInfo = true;
        }
        save();
    }

    public static void setHideDebugInfo(boolean value) {
        ensureLoaded();
        hideDebugInfo = value;
        save();
    }

    public static void setReduceGlow(boolean value) {
        ensureLoaded();
        reduceGlow = value;
        save();
    }

    public static void setReduceGridNoise(boolean value) {
        ensureLoaded();
        reduceGridNoise = value;
        save();
    }

    public static void setMissionHudNotifications(boolean value) {
        ensureLoaded();
        missionHudNotifications = value;
        save();
    }

    public static void setUseScreenCore(boolean value) {
        ensureLoaded();
        useScreenCore = value;
        save();
    }

    public static void setScreenCoreMatchExistingLayout(boolean value) {
        ensureLoaded();
        screenCoreMatchExistingLayout = value;
        save();
    }

    public static void setScreenCoreDebug(boolean value) {
        ensureLoaded();
        screenCoreDebug = value;
        save();
    }

    public static void setScreenCoreExperimentalTabs(boolean value) {
        ensureLoaded();
        screenCoreExperimentalTabs = value;
        save();
    }

    public static void setUseCyberglassScreenCoreTheme(boolean value) {
        ensureLoaded();
        useCyberglassScreenCoreTheme = value;
        save();
    }

    public static void selectCyberglassDensity(CyberglassDensity density) {
        ensureLoaded();
        cyberglassDensity = density == null ? DEFAULT_CYBERGLASS_DENSITY : density;
        save();
    }

    public static void setCyberglassMotion(boolean value) {
        ensureLoaded();
        cyberglassMotion = value;
        save();
    }

    public static void setCyberglassBackgroundEffects(boolean value) {
        ensureLoaded();
        cyberglassBackgroundEffects = value;
        save();
    }

    public static void setCyberglassGlowStrength(float value) {
        ensureLoaded();
        cyberglassGlowStrength = Math.max(0.0F, Math.min(1.0F, value));
        save();
    }

    public static void setCyberglassReduceVisualNoise(boolean value) {
        ensureLoaded();
        cyberglassReduceVisualNoise = value;
        save();
    }

    public static void setCyberglassUseClassicLayout(boolean value) {
        ensureLoaded();
        cyberglassUseClassicLayout = value;
        save();
    }

    public static void cycleTheme(int offset) {
        var ids = TerminalThemeRegistry.ids();
        if (ids.isEmpty()) {
            selectedTheme = TerminalThemeRegistry.defaultThemeId();
            return;
        }
        int index = ids.indexOf(selectedThemeId());
        selectedTheme = ids.get(Math.floorMod((index < 0 ? 0 : index) + offset, ids.size()));
        save();
    }

    public static void load() {
        if (loaded) {
            return;
        }
        load(configPath());
    }

    private static void load(Path path) {
        loaded = true;
        resetDefaults();
        if (path == null) {
            return;
        }
        if (!Files.isRegularFile(path)) {
            save(path);
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            String themeValue = properties.getProperty(THEME_KEY);
            selectedTheme = themeValue == null || themeValue.isBlank() ? null : Identifier.tryParse(themeValue);
            navigationStyle = enumValue(
                    NavigationStyle.class,
                    properties.getProperty(NAVIGATION_STYLE_KEY),
                    NavigationStyle.APP_HUB);
            missionView = normalizeMissionView(enumValue(
                    MissionView.class,
                    properties.getProperty(MISSION_VIEW_KEY),
                    MissionView.GUIDED));
            interfaceDensity = enumValue(
                    InterfaceDensity.class,
                    properties.getProperty(INTERFACE_DENSITY_KEY),
                    DEFAULT_INTERFACE_DENSITY);
            terminalZoom = enumValue(
                    TerminalZoom.class,
                    properties.getProperty(TERMINAL_ZOOM_KEY),
                    DEFAULT_TERMINAL_ZOOM);
            visualLevel = enumValue(
                    VisualLevel.class,
                    properties.getProperty(VISUAL_LEVEL_KEY),
                    VisualLevel.BALANCED);
            reducedMotion = Boolean.parseBoolean(properties.getProperty(REDUCED_MOTION_KEY, "false"))
                    || visualLevel == VisualLevel.REDUCED_MOTION;
            highContrastMode = Boolean.parseBoolean(properties.getProperty(HIGH_CONTRAST_MODE_KEY, "false"));
            reducedClutterMode = Boolean.parseBoolean(properties.getProperty(REDUCED_CLUTTER_MODE_KEY,
                    Boolean.toString(visualLevel == VisualLevel.MINIMAL)));
            largeTextMode = Boolean.parseBoolean(properties.getProperty(LARGE_TEXT_MODE_KEY, "false"));
            simplifiedTerminalMode = Boolean.parseBoolean(properties.getProperty(SIMPLIFIED_TERMINAL_MODE_KEY, "false"));
            hideDebugInfo = Boolean.parseBoolean(properties.getProperty(HIDE_DEBUG_INFO_KEY, "true"));
            reduceGlow = Boolean.parseBoolean(properties.getProperty(REDUCE_GLOW_KEY, "true"));
            reduceGridNoise = Boolean.parseBoolean(properties.getProperty(REDUCE_GRID_NOISE_KEY, "true"));
            missionHudNotifications = Boolean.parseBoolean(
                    properties.getProperty(MISSION_HUD_NOTIFICATIONS_KEY, "true"));
            useScreenCore = Boolean.parseBoolean(properties.getProperty(USE_SCREEN_CORE_KEY, "true"));
            screenCoreMatchExistingLayout = Boolean.parseBoolean(
                    properties.getProperty(SCREEN_CORE_MATCH_EXISTING_LAYOUT_KEY, "true"));
            screenCoreDebug = Boolean.parseBoolean(properties.getProperty(SCREEN_CORE_DEBUG_KEY, "false"));
            screenCoreExperimentalTabs = Boolean.parseBoolean(
                    properties.getProperty(SCREEN_CORE_EXPERIMENTAL_TABS_KEY,
                            Boolean.toString(DEFAULT_SCREEN_CORE_EXPERIMENTAL_TABS)));
            useCyberglassScreenCoreTheme = Boolean.parseBoolean(
                    properties.getProperty(USE_CYBERGLASS_SCREEN_CORE_THEME_KEY, "true"));
            cyberglassDensity = enumValue(
                    CyberglassDensity.class,
                    properties.getProperty(CYBERGLASS_DENSITY_KEY),
                    DEFAULT_CYBERGLASS_DENSITY);
            cyberglassMotion = Boolean.parseBoolean(properties.getProperty(CYBERGLASS_MOTION_KEY, "true"));
            cyberglassBackgroundEffects = Boolean.parseBoolean(
                    properties.getProperty(CYBERGLASS_BACKGROUND_EFFECTS_KEY, "true"));
            cyberglassGlowStrength = floatValue(
                    properties.getProperty(CYBERGLASS_GLOW_STRENGTH_KEY), 0.75F, 0.0F, 1.0F);
            cyberglassReduceVisualNoise = Boolean.parseBoolean(
                    properties.getProperty(CYBERGLASS_REDUCE_VISUAL_NOISE_KEY, "false"));
            cyberglassUseClassicLayout = Boolean.parseBoolean(
                    properties.getProperty(CYBERGLASS_USE_CLASSIC_LAYOUT_KEY, "false"));
            if (needsBackfill(properties)) {
                saveBackfilled(path, properties);
            }
        } catch (IOException | RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Failed to load ECHO terminal client options; using defaults.", exception);
        }
    }

    public static void resetThemeForTests(Identifier themeId) {
        loaded = true;
        selectedTheme = themeId;
    }

    public static void reloadFromPathForTests(Path path) {
        loaded = false;
        load(path);
    }

    public static void resetMissionViewForTests(MissionView view) {
        loaded = true;
        missionView = normalizeMissionView(view);
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static void save() {
        save(configPath());
    }

    private static void save(Path path) {
        if (path == null) {
            return;
        }
        writeProperties(path, currentProperties());
    }

    private static void saveBackfilled(Path path, Properties loadedProperties) {
        Properties properties = currentProperties();
        for (String key : CONFIG_KEYS) {
            if (loadedProperties.containsKey(key)) {
                properties.setProperty(key, loadedProperties.getProperty(key));
            }
        }
        writeProperties(path, properties);
    }

    private static void writeProperties(Path path, Properties properties) {
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "ECHO Terminal client options");
            }
        } catch (IOException | RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Failed to save ECHO terminal client options.", exception);
        }
    }

    private static Properties currentProperties() {
        Properties properties = new Properties();
        properties.setProperty(THEME_KEY, selectedThemeId().toString());
        properties.setProperty(NAVIGATION_STYLE_KEY, navigationStyle.name());
        properties.setProperty(MISSION_VIEW_KEY, missionView.name());
        properties.setProperty(INTERFACE_DENSITY_KEY, interfaceDensity.name());
        properties.setProperty(TERMINAL_ZOOM_KEY, terminalZoom.name());
        properties.setProperty(VISUAL_LEVEL_KEY, visualLevel.name());
        properties.setProperty(REDUCED_MOTION_KEY, Boolean.toString(reducedMotion));
        properties.setProperty(HIGH_CONTRAST_MODE_KEY, Boolean.toString(highContrastMode));
        properties.setProperty(REDUCED_CLUTTER_MODE_KEY, Boolean.toString(reducedClutterMode));
        properties.setProperty(LARGE_TEXT_MODE_KEY, Boolean.toString(largeTextMode));
        properties.setProperty(SIMPLIFIED_TERMINAL_MODE_KEY, Boolean.toString(simplifiedTerminalMode));
        properties.setProperty(HIDE_DEBUG_INFO_KEY, Boolean.toString(hideDebugInfo));
        properties.setProperty(REDUCE_GLOW_KEY, Boolean.toString(reduceGlow));
        properties.setProperty(REDUCE_GRID_NOISE_KEY, Boolean.toString(reduceGridNoise));
        properties.setProperty(MISSION_HUD_NOTIFICATIONS_KEY, Boolean.toString(missionHudNotifications));
        properties.setProperty(USE_SCREEN_CORE_KEY, Boolean.toString(useScreenCore));
        properties.setProperty(SCREEN_CORE_MATCH_EXISTING_LAYOUT_KEY,
                Boolean.toString(screenCoreMatchExistingLayout));
        properties.setProperty(SCREEN_CORE_DEBUG_KEY, Boolean.toString(screenCoreDebug));
        properties.setProperty(SCREEN_CORE_EXPERIMENTAL_TABS_KEY,
                Boolean.toString(screenCoreExperimentalTabs));
        properties.setProperty(USE_CYBERGLASS_SCREEN_CORE_THEME_KEY,
                Boolean.toString(useCyberglassScreenCoreTheme));
        properties.setProperty(CYBERGLASS_DENSITY_KEY, cyberglassDensity.name());
        properties.setProperty(CYBERGLASS_MOTION_KEY, Boolean.toString(cyberglassMotion));
        properties.setProperty(CYBERGLASS_BACKGROUND_EFFECTS_KEY,
                Boolean.toString(cyberglassBackgroundEffects));
        properties.setProperty(CYBERGLASS_GLOW_STRENGTH_KEY,
                Float.toString(cyberglassGlowStrength()));
        properties.setProperty(CYBERGLASS_REDUCE_VISUAL_NOISE_KEY,
                Boolean.toString(cyberglassReduceVisualNoise));
        properties.setProperty(CYBERGLASS_USE_CLASSIC_LAYOUT_KEY,
                Boolean.toString(cyberglassUseClassicLayout));
        return properties;
    }

    private static boolean needsBackfill(Properties properties) {
        for (String key : CONFIG_KEYS) {
            if (!properties.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private static void resetDefaults() {
        selectedTheme = null;
        navigationStyle = NavigationStyle.APP_HUB;
        missionView = MissionView.GUIDED;
        interfaceDensity = DEFAULT_INTERFACE_DENSITY;
        terminalZoom = DEFAULT_TERMINAL_ZOOM;
        visualLevel = VisualLevel.BALANCED;
        reducedMotion = false;
        highContrastMode = false;
        reducedClutterMode = false;
        largeTextMode = false;
        simplifiedTerminalMode = false;
        hideDebugInfo = true;
        reduceGlow = true;
        reduceGridNoise = true;
        missionHudNotifications = true;
        useScreenCore = true;
        screenCoreMatchExistingLayout = true;
        screenCoreDebug = false;
        screenCoreExperimentalTabs = DEFAULT_SCREEN_CORE_EXPERIMENTAL_TABS;
        useCyberglassScreenCoreTheme = true;
        cyberglassDensity = DEFAULT_CYBERGLASS_DENSITY;
        cyberglassMotion = true;
        cyberglassBackgroundEffects = true;
        cyberglassGlowStrength = 0.75F;
        cyberglassReduceVisualNoise = false;
        cyberglassUseClassicLayout = false;
    }

    private static Path configPath() {
        try {
            return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_FILE);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static float floatValue(String value, float fallback, float min, float max) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(min, Math.min(max, Float.parseFloat(value)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public enum NavigationStyle {
        APP_HUB,
        SIDEBAR_HUB,
        COMPACT_TOP
    }

    public enum MissionView {
        VISUAL_QUEST_HUB,
        GUIDED,
        VISUAL_RPG,
        MINIMAL
    }

    public enum InterfaceDensity {
        COMFORTABLE,
        BALANCED,
        COMPACT;

        public int compactness() {
            return switch (this) {
                case COMFORTABLE -> 0;
                case BALANCED -> 1;
                case COMPACT -> 2;
            };
        }
    }

    public enum CyberglassDensity {
        COMPACT,
        COMFORTABLE,
        CINEMATIC
    }

    public enum TerminalZoom {
        ZOOM_50(50),
        ZOOM_75(75),
        ZOOM_85(85),
        ZOOM_90(90),
        ZOOM_100(100),
        ZOOM_110(110),
        ZOOM_125(125),
        ZOOM_150(150);

        private final int percent;

        TerminalZoom(int percent) {
            this.percent = percent;
        }

        public int percent() {
            return percent;
        }

        public double scale() {
            return percent / 100.0D;
        }

        public String label() {
            return percent + "%";
        }
    }

    public enum VisualLevel {
        BALANCED,
        MINIMAL,
        REDUCED_MOTION
    }
}
