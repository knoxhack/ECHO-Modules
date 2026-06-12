package com.knoxhack.echolens.config;

import com.echoplatform.echocore.api.config.EchoConfigCategory;
import com.echoplatform.echocore.api.config.EchoConfigEntry;
import com.echoplatform.echocore.api.config.EchoConfigModule;
import com.echoplatform.echocore.api.config.EchoConfigProvider;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensAccessPolicy;
import java.util.List;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class LensConfig {
    public static final EchoNativeConfigSpec COMMON_SPEC;
    public static final EchoNativeConfigSpec CLIENT_SPEC;

    public static final EchoNativeConfigSpec.EnumValue<LensAccessPolicy> INVENTORY_ACCESS_POLICY;
    public static final EchoNativeConfigSpec.BooleanValue MACHINE_STATUS_VISIBILITY;
    public static final EchoNativeConfigSpec.BooleanValue BEGINNER_HINTS;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_COMMANDS;
    public static final EchoNativeConfigSpec.BooleanValue SERVER_DEEP_SCAN_ENABLED;
    public static final EchoNativeConfigSpec.DoubleValue SERVER_SCAN_DISTANCE;
    public static final EchoNativeConfigSpec.IntValue SERVER_SCAN_RATE_LIMIT;
    public static final EchoNativeConfigSpec.BooleanValue SERVER_REDACT_PROTECTED_TARGETS;

    public static final EchoNativeConfigSpec.BooleanValue HUD_ENABLED;
    public static final EchoNativeConfigSpec.EnumValue<OverlayPosition> OVERLAY_POSITION;
    public static final EchoNativeConfigSpec.IntValue OFFSET_X;
    public static final EchoNativeConfigSpec.IntValue OFFSET_Y;
    public static final EchoNativeConfigSpec.DoubleValue SCALE;
    public static final EchoNativeConfigSpec.DoubleValue OPACITY;
    public static final EchoNativeConfigSpec.BooleanValue ANIMATION;
    public static final EchoNativeConfigSpec.BooleanValue REDUCED_MOTION;
    public static final EchoNativeConfigSpec.EnumValue<LensThemeId> THEME;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_IDENTITY;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_BLOCK;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_ENTITY;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_FLUID;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_MACHINE;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_INVENTORY;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_INTEGRATION;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_ACTIONS;
    public static final EchoNativeConfigSpec.IntValue COMPACT_ROW_LIMIT;
    public static final EchoNativeConfigSpec.IntValue EXPANDED_ROW_LIMIT;
    public static final EchoNativeConfigSpec.IntValue DEEP_ROW_LIMIT;
    public static final EchoNativeConfigSpec.DoubleValue MAX_SCAN_DISTANCE;
    public static final EchoNativeConfigSpec.IntValue SERVER_DEEP_SCAN_TIMEOUT_TICKS;
    public static final EchoNativeConfigSpec.IntValue SERVER_DEEP_SCAN_CACHE_TICKS;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_SERVER_SCAN_STATUS;

    static {
        EchoNativeConfigSpec.Builder common = new EchoNativeConfigSpec.Builder();
        common.push("privacy");
        INVENTORY_ACCESS_POLICY = common.comment("Controls how much inventory state Lens may reveal. Public-only never exposes contents.")
                .defineEnum("inventory_access_policy", LensAccessPolicy.PUBLIC_ONLY);
        MACHINE_STATUS_VISIBILITY = common.define("machine_status_visibility", true);
        common.pop();
        common.push("guidance");
        BEGINNER_HINTS = common.define("beginner_hints", true);
        common.pop();
        common.push("debug");
        DEBUG_COMMANDS = common.define("commands", true);
        common.pop();
        common.push("server_scan");
        SERVER_DEEP_SCAN_ENABLED = common.comment("Allow Deep Scan to request public server-verified data.")
                .define("enabled", true);
        SERVER_SCAN_DISTANCE = common.defineInRange("distance", 24.0D, 4.0D, 64.0D);
        SERVER_SCAN_RATE_LIMIT = common.defineInRange("rate_limit", 4, 1, 20);
        SERVER_REDACT_PROTECTED_TARGETS = common.comment("Redact protected/private targets instead of exposing details.")
                .define("redact_protected_targets", true);
        common.pop();
        COMMON_SPEC = common.build();

        EchoNativeConfigSpec.Builder client = new EchoNativeConfigSpec.Builder();
        client.push("hud");
        HUD_ENABLED = client.define("enabled", true);
        OVERLAY_POSITION = client.defineEnum("position", OverlayPosition.TOP_CENTER);
        OFFSET_X = client.defineInRange("offset_x", 0, -1000, 1000);
        OFFSET_Y = client.defineInRange("offset_y", 12, -1000, 1000);
        SCALE = client.defineInRange("scale", 1.0D, 0.65D, 1.8D);
        OPACITY = client.defineInRange("opacity", 0.86D, 0.25D, 1.0D);
        ANIMATION = client.define("animation", true);
        REDUCED_MOTION = client.define("reduced_motion", false);
        THEME = client.defineEnum("theme", LensThemeId.ECHO_DARK);
        MAX_SCAN_DISTANCE = client.defineInRange("max_scan_distance", 18.0D, 4.0D, 64.0D);
        client.pop();
        client.push("categories");
        SHOW_IDENTITY = client.define("identity", true);
        SHOW_BLOCK = client.define("block", true);
        SHOW_ENTITY = client.define("entity", true);
        SHOW_FLUID = client.define("fluid", true);
        SHOW_MACHINE = client.define("machine", true);
        SHOW_INVENTORY = client.define("inventory", true);
        SHOW_INTEGRATION = client.define("integration", true);
        SHOW_ACTIONS = client.define("actions", true);
        client.pop();
        client.push("limits");
        COMPACT_ROW_LIMIT = client.defineInRange("compact_rows", 4, 1, 12);
        EXPANDED_ROW_LIMIT = client.defineInRange("expanded_rows", 12, 4, 32);
        DEEP_ROW_LIMIT = client.defineInRange("deep_rows", 40, 8, 96);
        client.pop();
        client.push("server_scan");
        SERVER_DEEP_SCAN_TIMEOUT_TICKS = client.defineInRange("timeout_ticks", 40, 10, 200);
        SERVER_DEEP_SCAN_CACHE_TICKS = client.defineInRange("cache_ticks", 20, 0, 200);
        SHOW_SERVER_SCAN_STATUS = client.define("show_status", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private LensConfig() {
    }

    private static final EchoConfigProvider ECHO_CONFIG_PROVIDER =
            EchoConfigProvider.of(EchoLens.MODID, LensConfig::echoConfigModule);

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(ECHO_CONFIG_PROVIDER);
    }

    private static EchoConfigModule echoConfigModule() {
        List<EchoConfigCategory> categories = List.of(
                new EchoConfigCategory("privacy", "Privacy", List.of(
                        EchoConfigEntry.enumSpec("inventory_access_policy", "Inventory Access Policy",
                                "Controls how much inventory state Lens may reveal.",
                                EchoConfigSide.COMMON, INVENTORY_ACCESS_POLICY, LensAccessPolicy.class,
                                true, false, false),
                        EchoConfigEntry.booleanSpec("machine_status_visibility", "Machine Status Visibility",
                                "Allow Lens reports to include machine status details.",
                                EchoConfigSide.COMMON, MACHINE_STATUS_VISIBILITY, true, false, false))),
                new EchoConfigCategory("guidance", "Guidance", List.of(
                        EchoConfigEntry.booleanSpec("beginner_hints", "Beginner Hints",
                                "Show beginner-friendly Lens hints.",
                                EchoConfigSide.COMMON, BEGINNER_HINTS, true, false, false))),
                new EchoConfigCategory("server_scan", "Server Deep Scan", List.of(
                        EchoConfigEntry.booleanSpec("server_deep_scan_enabled", "Server Deep Scan Enabled",
                                "Allow Deep Scan to request public server-verified facts.",
                                EchoConfigSide.COMMON, SERVER_DEEP_SCAN_ENABLED, true, false, false),
                        EchoConfigEntry.doubleSpec("server_scan_distance", "Server Scan Distance",
                                "Maximum distance for server-assisted Deep Scan requests.",
                                EchoConfigSide.COMMON, SERVER_SCAN_DISTANCE, 4.0D, 64.0D,
                                true, false, false),
                        EchoConfigEntry.intSpec("server_scan_rate_limit", "Server Scan Rate Limit",
                                "Per-player Deep Scan request rate limit.",
                                EchoConfigSide.COMMON, SERVER_SCAN_RATE_LIMIT, 1, 20,
                                true, false, false),
                        EchoConfigEntry.booleanSpec("server_redact_protected_targets", "Redact Protected Targets",
                                "Hide private/protected server details from Lens responses.",
                                EchoConfigSide.COMMON, SERVER_REDACT_PROTECTED_TARGETS, true, false, false))),
                new EchoConfigCategory("hud", "HUD", List.of(
                        EchoConfigEntry.booleanSpec("hud_enabled", "HUD Enabled", "",
                                EchoConfigSide.CLIENT, HUD_ENABLED, true, false, false),
                        EchoConfigEntry.enumSpec("overlay_position", "Overlay Position", "",
                                EchoConfigSide.CLIENT, OVERLAY_POSITION, OverlayPosition.class,
                                true, false, false),
                        EchoConfigEntry.intSpec("offset_x", "Offset X", "",
                                EchoConfigSide.CLIENT, OFFSET_X, -1000, 1000,
                                true, false, false),
                        EchoConfigEntry.intSpec("offset_y", "Offset Y", "",
                                EchoConfigSide.CLIENT, OFFSET_Y, -1000, 1000,
                                true, false, false),
                        EchoConfigEntry.doubleSpec("scale", "Scale", "",
                                EchoConfigSide.CLIENT, SCALE, 0.65D, 1.8D,
                                true, false, false),
                        EchoConfigEntry.doubleSpec("opacity", "Opacity", "",
                                EchoConfigSide.CLIENT, OPACITY, 0.25D, 1.0D,
                                true, false, false),
                        EchoConfigEntry.booleanSpec("animation", "Animation", "",
                                EchoConfigSide.CLIENT, ANIMATION, true, false, false),
                        EchoConfigEntry.booleanSpec("reduced_motion", "Reduced Motion", "",
                                EchoConfigSide.CLIENT, REDUCED_MOTION, true, false, false),
                        EchoConfigEntry.enumSpec("theme", "Theme", "",
                                EchoConfigSide.CLIENT, THEME, LensThemeId.class,
                                true, false, false),
                        EchoConfigEntry.doubleSpec("max_scan_distance", "Max Scan Distance", "",
                                EchoConfigSide.CLIENT, MAX_SCAN_DISTANCE, 4.0D, 64.0D,
                                true, false, false))),
                new EchoConfigCategory("client_server_scan", "Client Server Scan", List.of(
                        EchoConfigEntry.intSpec("server_deep_scan_timeout_ticks", "Server Scan Timeout",
                                "Ticks before pending Deep Scan data is marked unavailable.",
                                EchoConfigSide.CLIENT, SERVER_DEEP_SCAN_TIMEOUT_TICKS, 10, 200,
                                true, false, false),
                        EchoConfigEntry.intSpec("server_deep_scan_cache_ticks", "Server Scan Cache",
                                "Ticks to reuse verified Deep Scan data for the same target.",
                                EchoConfigSide.CLIENT, SERVER_DEEP_SCAN_CACHE_TICKS, 0, 200,
                                true, false, false),
                        EchoConfigEntry.booleanSpec("show_server_scan_status", "Show Server Scan Status",
                                "Show Deep Scan status badges such as Querying, Verified, and Redacted.",
                                EchoConfigSide.CLIENT, SHOW_SERVER_SCAN_STATUS, true, false, false))),
                new EchoConfigCategory("categories", "Categories", List.of(
                        EchoConfigEntry.booleanSpec("show_identity", "Identity", "",
                                EchoConfigSide.CLIENT, SHOW_IDENTITY, true, false, false),
                        EchoConfigEntry.booleanSpec("show_block", "Block", "",
                                EchoConfigSide.CLIENT, SHOW_BLOCK, true, false, false),
                        EchoConfigEntry.booleanSpec("show_entity", "Entity", "",
                                EchoConfigSide.CLIENT, SHOW_ENTITY, true, false, false),
                        EchoConfigEntry.booleanSpec("show_fluid", "Fluid", "",
                                EchoConfigSide.CLIENT, SHOW_FLUID, true, false, false),
                        EchoConfigEntry.booleanSpec("show_machine", "Machine", "",
                                EchoConfigSide.CLIENT, SHOW_MACHINE, true, false, false),
                        EchoConfigEntry.booleanSpec("show_inventory", "Inventory", "",
                                EchoConfigSide.CLIENT, SHOW_INVENTORY, true, false, false),
                        EchoConfigEntry.booleanSpec("show_integration", "Integration", "",
                                EchoConfigSide.CLIENT, SHOW_INTEGRATION, true, false, false),
                        EchoConfigEntry.booleanSpec("show_actions", "Actions", "",
                                EchoConfigSide.CLIENT, SHOW_ACTIONS, true, false, false))),
                new EchoConfigCategory("limits", "Limits", List.of(
                        EchoConfigEntry.intSpec("compact_rows", "Compact Rows", "",
                                EchoConfigSide.CLIENT, COMPACT_ROW_LIMIT, 1, 12,
                                true, false, false),
                        EchoConfigEntry.intSpec("expanded_rows", "Expanded Rows", "",
                                EchoConfigSide.CLIENT, EXPANDED_ROW_LIMIT, 4, 32,
                                true, false, false),
                        EchoConfigEntry.intSpec("deep_rows", "Deep Rows", "",
                                EchoConfigSide.CLIENT, DEEP_ROW_LIMIT, 8, 96,
                                true, false, false))));
        return new EchoConfigModule(EchoLens.MODID, "Lens", categories);
    }

    public static <T> T value(EchoNativeConfigSpec.ConfigValue<T> configValue, T fallback) {
        try {
            T value = configValue.get();
            return value == null ? fallback : value;
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }

    public static boolean bool(EchoNativeConfigSpec.ConfigValue<Boolean> configValue, boolean fallback) {
        return value(configValue, fallback);
    }

    public static int integer(EchoNativeConfigSpec.ConfigValue<Integer> configValue, int fallback) {
        return value(configValue, fallback);
    }

    public static double decimal(EchoNativeConfigSpec.ConfigValue<Double> configValue, double fallback) {
        return value(configValue, fallback);
    }

    public enum OverlayPosition {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }

    public enum LensThemeId {
        ECHO_DARK,
        CLEAN_MINIMAL,
        VANILLA_COMPACT,
        ASHFALL_HAZARD
    }
}
