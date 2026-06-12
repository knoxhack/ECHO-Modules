package com.knoxhack.echoindex;

import com.echoplatform.echocore.api.config.EchoConfigCategory;
import com.echoplatform.echocore.api.config.EchoConfigEntry;
import com.echoplatform.echocore.api.config.EchoConfigModule;
import com.echoplatform.echocore.api.config.EchoConfigProvider;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import java.util.List;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class Config {
    public static final EchoNativeConfigSpec SPEC;
    public static final EchoNativeConfigSpec.BooleanValue OVERLAY_ENABLED;
    public static final EchoNativeConfigSpec.EnumValue<OverlaySide> OVERLAY_SIDE;
    public static final EchoNativeConfigSpec.EnumValue<OverlayLayout> OVERLAY_LAYOUT;
    public static final EchoNativeConfigSpec.IntValue OVERLAY_WIDTH;
    public static final EchoNativeConfigSpec.IntValue OVERLAY_MAX_COLUMNS;
    public static final EchoNativeConfigSpec.EnumValue<GridDensity> OVERLAY_GRID_DENSITY;
    public static final EchoNativeConfigSpec.BooleanValue OVERLAY_SHOW_BOOKMARKS;
    public static final EchoNativeConfigSpec.BooleanValue SEARCH_TOOLTIP_SEARCH;
    public static final EchoNativeConfigSpec.BooleanValue SEARCH_TAG_SEARCH;
    public static final EchoNativeConfigSpec.BooleanValue SEARCH_REGISTRY_SEARCH;
    public static final EchoNativeConfigSpec.BooleanValue UI_CINEMATIC_STYLE;
    public static final EchoNativeConfigSpec.BooleanValue UI_USE_SCREENCORE;
    public static final EchoNativeConfigSpec.BooleanValue UI_GROUP_BY_MOD;
    public static final EchoNativeConfigSpec.BooleanValue UI_COMPACT_GRID;
    public static final EchoNativeConfigSpec.BooleanValue UI_REMEMBER_LAST_PAGE;
    public static final EchoNativeConfigSpec.BooleanValue UI_SHOW_LOCKED_ITEMS;
    public static final EchoNativeConfigSpec.BooleanValue DISCOVERY_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue DISCOVERY_HIDE_LOCKED;
    public static final EchoNativeConfigSpec.BooleanValue DISCOVERY_SHOW_LOCKED_HINTS;
    public static final EchoNativeConfigSpec.BooleanValue RECIPES_SHOW_ALL;
    public static final EchoNativeConfigSpec.BooleanValue RECIPES_REQUIRE_DISCOVERY;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_SHOW_RECIPE_IDS;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_SCREENCORE;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_PROVIDERS;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_ACTIONS;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_RECIPE_PARSING;
    public static final EchoNativeConfigSpec.BooleanValue SEARCH_CACHE_ENABLED;
    public static final EchoNativeConfigSpec.IntValue SEARCH_MAX_RESULTS;
    public static final EchoNativeConfigSpec.IntValue UI_MAX_RENDERED_ITEMS;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_COMMANDS;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
        builder.push("overlay");
        OVERLAY_ENABLED = builder.define("enabled", true);
        OVERLAY_SIDE = builder.defineEnum("side", OverlaySide.RIGHT);
        OVERLAY_LAYOUT = builder.defineEnum("layout", OverlayLayout.JEI);
        OVERLAY_WIDTH = builder.defineInRange("width", 300, 160, 520);
        OVERLAY_MAX_COLUMNS = builder.defineInRange("max_columns", 9, 3, 14);
        OVERLAY_GRID_DENSITY = builder.defineEnum("grid_density", GridDensity.NORMAL);
        OVERLAY_SHOW_BOOKMARKS = builder.define("show_bookmarks", true);
        builder.pop();
        builder.push("search");
        SEARCH_TOOLTIP_SEARCH = builder.define("tooltip_search", false);
        SEARCH_TAG_SEARCH = builder.define("tag_search", true);
        SEARCH_REGISTRY_SEARCH = builder.define("registry_search", true);
        SEARCH_CACHE_ENABLED = builder.define("cache_enabled", true);
        SEARCH_MAX_RESULTS = builder.defineInRange("max_results", 256, 32, 4096);
        builder.pop();
        builder.push("ui");
        UI_CINEMATIC_STYLE = builder.define("cinematic_style", true);
        UI_USE_SCREENCORE = builder.define("use_screencore", true);
        UI_GROUP_BY_MOD = builder.define("group_by_mod", true);
        UI_COMPACT_GRID = builder.define("compact_grid", true);
        UI_REMEMBER_LAST_PAGE = builder.define("remember_last_page", true);
        UI_SHOW_LOCKED_ITEMS = builder.define("show_locked_items", true);
        UI_MAX_RENDERED_ITEMS = builder.defineInRange("max_rendered_items", 160, 36, 800);
        builder.pop();
        builder.push("discovery");
        DISCOVERY_ENABLED = builder.define("enabled", false);
        DISCOVERY_HIDE_LOCKED = builder.define("hide_locked", false);
        DISCOVERY_SHOW_LOCKED_HINTS = builder.define("show_locked_hints", true);
        builder.pop();
        builder.push("recipes");
        RECIPES_SHOW_ALL = builder.define("show_all", true);
        RECIPES_REQUIRE_DISCOVERY = builder.define("require_discovery", false);
        builder.pop();
        builder.push("debug");
        DEBUG_SHOW_RECIPE_IDS = builder.define("show_recipe_ids", false);
        DEBUG_SCREENCORE = builder.define("screencore", false);
        DEBUG_PROVIDERS = builder.define("providers", false);
        DEBUG_ACTIONS = builder.define("actions", false);
        DEBUG_RECIPE_PARSING = builder.define("recipe_parsing", false);
        DEBUG_COMMANDS = builder.define("commands", true);
        builder.pop();
        SPEC = builder.build();
    }

    private Config() {
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoIndex.MODID, () -> new EchoConfigModule(
                EchoIndex.MODID,
                "Index",
                List.of(
                        new EchoConfigCategory("overlay", "Overlay", List.of(
                                EchoConfigEntry.booleanSpec("overlay_enabled", "Overlay Enabled",
                                        "Shows the in-inventory Index overlay beside container screens.",
                                        EchoConfigSide.CLIENT, OVERLAY_ENABLED, true, false, false),
                                EchoConfigEntry.enumSpec("overlay_side", "Overlay Side",
                                        "Preferred screen side for the inventory overlay.",
                                        EchoConfigSide.CLIENT, OVERLAY_SIDE, OverlaySide.class,
                                        true, false, false),
                                EchoConfigEntry.enumSpec("overlay_layout", "Overlay Layout",
                                        "Overlay layout preset used by the inventory Index.",
                                        EchoConfigSide.CLIENT, OVERLAY_LAYOUT, OverlayLayout.class,
                                        true, false, false),
                                EchoConfigEntry.intSpec("overlay_width", "Overlay Width",
                                        "Default overlay panel width in scaled screen pixels.",
                                        EchoConfigSide.CLIENT, OVERLAY_WIDTH, 160, 520,
                                        true, false, false),
                                EchoConfigEntry.intSpec("overlay_columns", "Overlay Columns",
                                        "Preferred column cap for compact/default overlay sizing; manually enlarged panels can show more columns to use available space.",
                                        EchoConfigSide.CLIENT, OVERLAY_MAX_COLUMNS, 3, 14,
                                        true, false, false),
                                EchoConfigEntry.enumSpec("overlay_grid_density", "Grid Density",
                                        "Default inventory overlay item grid density.",
                                        EchoConfigSide.CLIENT, OVERLAY_GRID_DENSITY, GridDensity.class,
                                        true, false, false),
                                EchoConfigEntry.booleanSpec("show_bookmarks", "Show Bookmarks",
                                        "Shows bookmark filters and bookmarked state in overlay surfaces.",
                                        EchoConfigSide.CLIENT, OVERLAY_SHOW_BOOKMARKS, true, false, false))),
                        new EchoConfigCategory("search", "Search", List.of(
                                EchoConfigEntry.booleanSpec("tooltip_search", "Tooltip Search",
                                        "Includes tooltip text in item search when available.",
                                        EchoConfigSide.CLIENT, SEARCH_TOOLTIP_SEARCH, true, false, false),
                                EchoConfigEntry.booleanSpec("tag_search", "Tag Search",
                                        "Includes item tags in Index item search.",
                                        EchoConfigSide.CLIENT, SEARCH_TAG_SEARCH, true, false, false),
                                EchoConfigEntry.booleanSpec("registry_search", "Registry Search",
                                        "Includes raw registry ids in Index item search.",
                                        EchoConfigSide.CLIENT, SEARCH_REGISTRY_SEARCH, true, false, false),
                                EchoConfigEntry.booleanSpec("cache_enabled", "Search Cache",
                                        "Caches repeated Index search results between UI refreshes.",
                                        EchoConfigSide.CLIENT, SEARCH_CACHE_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("max_results", "Max Results",
                                        "Maximum item search results returned to Index UI surfaces.",
                                        EchoConfigSide.CLIENT, SEARCH_MAX_RESULTS, 32, 4096,
                                        true, false, false))),
                        new EchoConfigCategory("ui", "UI", List.of(
                                EchoConfigEntry.booleanSpec("cinematic_style", "Cinematic Style",
                                        "Uses the higher-contrast ECHO visual style for Index screens.",
                                        EchoConfigSide.CLIENT, UI_CINEMATIC_STYLE, true, false, false),
                                EchoConfigEntry.booleanSpec("use_screencore", "Use ScreenCore Index",
                                        "Opens the new ScreenCore-powered Index database when ScreenCore is installed; the old Index screen remains the fallback.",
                                        EchoConfigSide.CLIENT, UI_USE_SCREENCORE, true, false, false),
                                EchoConfigEntry.booleanSpec("group_by_mod", "Group By Mod",
                                        "Groups item browsing by mod in the ScreenCore Index UI by default.",
                                        EchoConfigSide.CLIENT, UI_GROUP_BY_MOD, true, false, false),
                                EchoConfigEntry.booleanSpec("compact_grid", "Compact Grid",
                                        "Uses compact item rows and cards in ScreenCore Index browsing surfaces.",
                                        EchoConfigSide.CLIENT, UI_COMPACT_GRID, true, false, false),
                                EchoConfigEntry.booleanSpec("remember_last_page", "Remember Last Page",
                                        "Reopens ScreenCore Index to the last selected browser page.",
                                        EchoConfigSide.CLIENT, UI_REMEMBER_LAST_PAGE, true, false, false),
                                EchoConfigEntry.booleanSpec("show_locked_items", "Show Locked Items",
                                        "Shows locked or unavailable entries with disabled/locked state instead of hiding them in ScreenCore Index.",
                                        EchoConfigSide.CLIENT, UI_SHOW_LOCKED_ITEMS, true, false, false),
                                EchoConfigEntry.intSpec("max_rendered_items", "Max Rendered Items",
                                        "Maximum item cells rendered per Index grid page.",
                                        EchoConfigSide.CLIENT, UI_MAX_RENDERED_ITEMS, 36, 800,
                                        true, false, false))),
                        new EchoConfigCategory("discovery", "Discovery", List.of(
                                EchoConfigEntry.booleanSpec("discovery_enabled", "Discovery Enabled",
                                        "Enables discovery/read/bookmark progression state for Index entries.",
                                        EchoConfigSide.COMMON, DISCOVERY_ENABLED, true, false, false),
                                EchoConfigEntry.booleanSpec("hide_locked", "Hide Locked",
                                        "Hides locked entries instead of showing locked hints.",
                                        EchoConfigSide.COMMON, DISCOVERY_HIDE_LOCKED, true, false, false),
                                EchoConfigEntry.booleanSpec("show_locked_hints", "Show Locked Hints",
                                        "Shows placeholder hints for locked entries when hiding is disabled.",
                                        EchoConfigSide.COMMON, DISCOVERY_SHOW_LOCKED_HINTS, true, false, false))),
                        new EchoConfigCategory("recipes", "Recipes", List.of(
                                EchoConfigEntry.booleanSpec("show_all_recipes", "Show All Recipes",
                                        "Keeps recipes and source cards visible by default for usability.",
                                        EchoConfigSide.COMMON, RECIPES_SHOW_ALL, true, false, false),
                                EchoConfigEntry.booleanSpec("require_discovery", "Require Discovery",
                                        "Requires discovery state before recipe cards are treated as unlocked.",
                                        EchoConfigSide.COMMON, RECIPES_REQUIRE_DISCOVERY, true, false, false))),
                        new EchoConfigCategory("debug", "Debug", List.of(
                                EchoConfigEntry.booleanSpec("show_recipe_ids", "Show Recipe Ids",
                                        "Shows raw indexed recipe ids in developer-facing recipe surfaces.",
                                        EchoConfigSide.CLIENT, DEBUG_SHOW_RECIPE_IDS, true, false, false),
                                EchoConfigEntry.booleanSpec("screencore", "ScreenCore Debug",
                                        "Shows ScreenCore Index diagnostics and developer affordances.",
                                        EchoConfigSide.CLIENT, DEBUG_SCREENCORE, true, false, false),
                                EchoConfigEntry.booleanSpec("providers", "Provider Debug",
                                        "Logs ScreenCore Index provider refreshes and cache rebuilds.",
                                        EchoConfigSide.CLIENT, DEBUG_PROVIDERS, true, false, false),
                                EchoConfigEntry.booleanSpec("actions", "Action Debug",
                                        "Logs ScreenCore Index action dispatch and ignored invalid action values.",
                                        EchoConfigSide.CLIENT, DEBUG_ACTIONS, true, false, false),
                                EchoConfigEntry.booleanSpec("recipe_parsing", "Recipe Parsing Debug",
                                        "Exposes unsupported recipe layout warnings in ScreenCore Index developer surfaces.",
                                        EchoConfigSide.CLIENT, DEBUG_RECIPE_PARSING, true, false, false),
                                EchoConfigEntry.booleanSpec("commands", "Debug Commands",
                                        "Enables mutating /echoindex debug commands for pack author testing.",
                                        EchoConfigSide.COMMON, DEBUG_COMMANDS, true, false, false)))))));
    }

    public enum OverlaySide {
        LEFT,
        RIGHT
    }

    public enum OverlayLayout {
        JEI,
        COMPACT,
        TALL
    }

    public enum GridDensity {
        COMPACT,
        NORMAL,
        LARGE
    }
}
