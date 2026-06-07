package com.knoxhack.echothemecore.content;

import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.EchoThemeBlockPalette;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.api.EchoThemeSoundProfile;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.api.EchoThemeUiAssets;
import com.knoxhack.echothemecore.api.EchoThemeVanillaUiProfile;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Shared fallback assets reused by all built-in theme presets.
 * Color palettes are what give each preset its visual identity.
 */
final class BuiltinThemeFallbackAssets {
    private static final String TX = "cyberglass";
    private static final String NS = EchoThemeCore.MODID;

    private static final Identifier BG = texture(TX, "background");
    private static final Identifier PANEL = texture(TX, "glass_panel");
    private static final Identifier PANEL_ALT = texture(TX, "glass_panel_alt");
    private static final Identifier BTN = texture(TX, "glass_button");
    private static final Identifier BTN_HOVER = texture(TX, "glass_button_hover");
    private static final Identifier TAB = texture(TX, "tab");
    private static final Identifier TAB_ACTIVE = texture(TX, "tab_active");
    private static final Identifier MISSION_CARD = texture(TX, "mission_card");
    private static final Identifier MISSION_CARD_SEL = texture(TX, "mission_card_selected");
    private static final Identifier CHIP = texture(TX, "status_chip");
    private static final Identifier PROGRESS = texture(TX, "progress_bar");
    private static final Identifier SCROLL = texture(TX, "scrollbar");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(NS,
            "textures/gui/themes/cyberglass/icons/icon_theme.png");
    private static final Identifier HOLO = texture(TX, "hologram_overlay");
    private static final Identifier ENERGY = texture(TX, "energy_overlay");
    private static final Identifier EDGE = texture(TX, "edge_glow");
    private static final Identifier PARTICLE = texture(TX, "particle_glints");
    private static final Identifier LOCKED = texture(TX, "locked_overlay");

    private static final Identifier SOUND_CLICK = soundcore("ui.terminal.select");
    private static final Identifier SOUND_ERROR = soundcore("ui.terminal.error");
    private static final Identifier SOUND_OPEN = soundcore("ui.terminal.open");
    private static final Identifier SOUND_CLOSE = soundcore("ui.terminal.close");
    private static final Identifier SOUND_MUSIC = soundcore("music.terminal.command_bed");
    private static final Identifier SOUND_STINGER = soundcore("stinger.objective.complete");
    private static final Identifier SOUND_WARN = soundcore("ui.terminal.warning");

    private BuiltinThemeFallbackAssets() {
    }

    static EchoThemeUiAssets uiAssets() {
        return new EchoThemeUiAssets(BG, PANEL, PANEL_ALT, BTN, BTN_HOVER, TAB, TAB_ACTIVE,
                MISSION_CARD, MISSION_CARD_SEL, CHIP, PROGRESS, SCROLL, ICON, HOLO, ENERGY, EDGE, PARTICLE, LOCKED);
    }

    static EchoThemeVanillaUiProfile vanillaProfile(EchoThemeColors colors) {
        return new EchoThemeVanillaUiProfile(
                BG, PANEL, BTN, CHIP, texture(TX, "vanilla_tooltip_panel"),
                texture(TX, "vanilla_toast_accent"), texture(TX, "vanilla_boss_bar_accent"),
                colors.background(), colors.panel(), colors.glass(),
                colors.border(), colors.selection(), colors.glow(),
                0.34F, 0.58F, 0.72F, true
        );
    }

    static EchoThemeSoundProfile soundProfile() {
        return new EchoThemeSoundProfile(SOUND_CLICK, SOUND_ERROR, SOUND_OPEN, SOUND_CLOSE,
                SOUND_MUSIC, SOUND_STINGER, SOUND_WARN);
    }

    static EchoThemeBlockPalette blockPalette() {
        return new EchoThemeBlockPalette(
                List.of(minecraft("tinted_glass"), minecraft("cyan_stained_glass"), minecraft("magenta_stained_glass")),
                List.of(minecraft("blackstone"), minecraft("deepslate_tiles")),
                List.of(minecraft("tinted_glass"), minecraft("cyan_stained_glass")),
                List.of(minecraft("sea_lantern"), minecraft("end_rod")),
                List.of(minecraft("amethyst_block"), minecraft("magenta_glazed_terracotta"))
        );
    }

    static Map<EchoThemeTextureKey, Identifier> moduleTextures() {
        Map<EchoThemeTextureKey, Identifier> map = new EnumMap<>(EchoThemeTextureKey.class);
        map.put(EchoThemeTextureKey.TERMINAL_PANEL, PANEL);
        map.put(EchoThemeTextureKey.TERMINAL_TAB, TAB);
        map.put(EchoThemeTextureKey.TERMINAL_TAB_ACTIVE, TAB_ACTIVE);
        map.put(EchoThemeTextureKey.TERMINAL_MISSION_CARD, MISSION_CARD);
        map.put(EchoThemeTextureKey.TERMINAL_STATUS_CHIP, CHIP);
        map.put(EchoThemeTextureKey.TERMINAL_BUTTON, BTN);
        map.put(EchoThemeTextureKey.TERMINAL_ICON, texture(TX, "icons/icon_terminal"));
        map.put(EchoThemeTextureKey.INDEX_PANEL, texture(TX, "index_panel"));
        map.put(EchoThemeTextureKey.INDEX_PANEL_WIDE, texture(TX, "index_panel_wide"));
        map.put(EchoThemeTextureKey.INDEX_PANEL_ACTIVE, texture(TX, "index_panel_active"));
        map.put(EchoThemeTextureKey.INDEX_BUTTON, texture(TX, "index_button"));
        map.put(EchoThemeTextureKey.INDEX_BUTTON_HOVER, texture(TX, "index_button_hover"));
        map.put(EchoThemeTextureKey.INDEX_CARD, texture(TX, "index_card"));
        map.put(EchoThemeTextureKey.INDEX_CARD_SELECTED, texture(TX, "index_card_selected"));
        map.put(EchoThemeTextureKey.INDEX_STATUS_CHIP, texture(TX, "index_status_chip"));
        map.put(EchoThemeTextureKey.INDEX_PROGRESS_BAR, texture(TX, "index_progress_bar"));
        map.put(EchoThemeTextureKey.INDEX_SCROLLBAR, texture(TX, "index_scrollbar"));
        map.put(EchoThemeTextureKey.INDEX_ICON, texture(TX, "icons/icon_index"));
        map.put(EchoThemeTextureKey.HOLOMAP_GRID, texture(TX, "holomap_grid"));
        map.put(EchoThemeTextureKey.HOLOMAP_PANEL, texture(TX, "holomap_panel"));
        map.put(EchoThemeTextureKey.HOLOMAP_ROUTE, texture(TX, "route_line"));
        map.put(EchoThemeTextureKey.HOLOMAP_MARKER_SIGNAL, texture(TX, "marker_signal"));
        map.put(EchoThemeTextureKey.HOLOMAP_MARKER_HAZARD, texture(TX, "marker_hazard"));
        map.put(EchoThemeTextureKey.HOLOMAP_MARKER_MISSION, texture(TX, "marker_mission"));
        map.put(EchoThemeTextureKey.HOLOMAP_MARKER_NEXUS, texture(TX, "marker_nexus"));
        map.put(EchoThemeTextureKey.HOLOMAP_MARKER_RECLAIMED, texture(TX, "marker_reclamation"));
        map.put(EchoThemeTextureKey.HOLOMAP_SELECTED_RING, texture(TX, "selected_marker_ring"));
        map.put(EchoThemeTextureKey.HOLOMAP_DANGER, texture(TX, "marker_hazard"));
        map.put(EchoThemeTextureKey.HOLOMAP_ANOMALY, texture(TX, "marker_nexus"));
        map.put(EchoThemeTextureKey.HOLOMAP_RECLAIMED, texture(TX, "marker_reclamation"));
        map.put(EchoThemeTextureKey.LENS_SCAN_RING, texture(TX, "lens_scan_ring"));
        map.put(EchoThemeTextureKey.LENS_TARGET_BOX, texture(TX, "lens_target_box"));
        map.put(EchoThemeTextureKey.LENS_WEAK_POINT, texture(TX, "lens_weakpoint_marker"));
        map.put(EchoThemeTextureKey.LENS_WARNING, texture(TX, "lens_warning_overlay"));
        map.put(EchoThemeTextureKey.LENS_ANOMALY_REVEAL, texture(TX, "lens_anomaly_overlay"));
        map.put(EchoThemeTextureKey.LENS_COMPLETION_PULSE, texture(TX, "lens_progress_arc"));
        map.put(EchoThemeTextureKey.LENS_PROGRESS_ARC, texture(TX, "lens_progress_arc"));
        map.put(EchoThemeTextureKey.LENS_NOISE_OVERLAY, texture(TX, "lens_noise_overlay"));
        map.put(EchoThemeTextureKey.VANILLA_CONTAINER_FRAME, texture(TX, "vanilla_container_frame"));
        map.put(EchoThemeTextureKey.VANILLA_INVENTORY_FRAME, texture(TX, "vanilla_inventory_frame"));
        map.put(EchoThemeTextureKey.VANILLA_CREATIVE_FRAME, texture(TX, "vanilla_inventory_frame"));
        map.put(EchoThemeTextureKey.VANILLA_TITLE_BACKPLATE, texture(TX, "vanilla_title_backplate"));
        map.put(EchoThemeTextureKey.VANILLA_PAUSE_PANEL, texture(TX, "vanilla_pause_panel"));
        map.put(EchoThemeTextureKey.VANILLA_SELECTED_SLOT, texture(TX, "vanilla_hotbar_accent"));
        map.put(EchoThemeTextureKey.VANILLA_TOOLTIP_PANEL, texture(TX, "vanilla_tooltip_panel"));
        map.put(EchoThemeTextureKey.VANILLA_TOAST_ACCENT, texture(TX, "vanilla_toast_accent"));
        map.put(EchoThemeTextureKey.VANILLA_BOSS_BAR_ACCENT, texture(TX, "vanilla_boss_bar_accent"));
        map.put(EchoThemeTextureKey.VANILLA_WIDGET_OUTLINE, texture(TX, "vanilla_widget_outline"));
        map.put(EchoThemeTextureKey.LOADING_BACKGROUND, texture(TX, "loading/background"));
        map.put(EchoThemeTextureKey.LOADING_PANEL, texture(TX, "loading/panel"));
        map.put(EchoThemeTextureKey.LOADING_PROGRESS_BAR, texture(TX, "loading/progress_bar"));
        map.put(EchoThemeTextureKey.LOADING_SPINNER, texture(TX, "loading/spinner"));
        map.put(EchoThemeTextureKey.LOADING_LOGO_MARK, texture(TX, "loading/logo_mark"));
        map.put(EchoThemeTextureKey.MENU_MAIN_BACKPLATE, texture(TX, "menu/main_backplate"));
        map.put(EchoThemeTextureKey.MENU_PAUSE_PANEL, texture(TX, "menu/pause_panel"));
        map.put(EchoThemeTextureKey.MENU_OPTIONS_PANEL, texture(TX, "menu/options_panel"));
        map.put(EchoThemeTextureKey.MENU_WORLD_ROW, texture(TX, "menu/world_row"));
        map.put(EchoThemeTextureKey.MENU_MODS_PANEL, texture(TX, "menu/mods_panel"));
        map.put(EchoThemeTextureKey.HUD_HOTBAR_FRAME, texture(TX, "hud/hotbar_frame"));
        map.put(EchoThemeTextureKey.HUD_SELECTED_SLOT, texture(TX, "hud/selected_slot"));
        map.put(EchoThemeTextureKey.HUD_CROSSHAIR_ACCENT, texture(TX, "hud/crosshair_accent"));
        map.put(EchoThemeTextureKey.HUD_BOSS_BAR, texture(TX, "hud/boss_bar"));
        map.put(EchoThemeTextureKey.HUD_CHAT_PANEL, texture(TX, "hud/chat_panel"));
        map.put(EchoThemeTextureKey.HUD_NOTIFICATION_CHIP, texture(TX, "hud/notification_chip"));
        map.put(EchoThemeTextureKey.ITEM_ICON_FRAME, texture(TX, "item_icon/frame"));
        map.put(EchoThemeTextureKey.ITEM_ICON_RARITY_RING, texture(TX, "item_icon/rarity_ring"));
        map.put(EchoThemeTextureKey.ITEM_ICON_BADGE, texture(TX, "item_icon/badge"));
        map.put(EchoThemeTextureKey.ITEM_ICON_LOCK_OVERLAY, texture(TX, "item_icon/lock_overlay"));
        map.put(EchoThemeTextureKey.ITEM_ICON_MISSION_MARKER, texture(TX, "item_icon/mission_marker"));
        map.put(EchoThemeTextureKey.RENDERCORE_GLOW_OVERLAY, texture(TX, "rendercore/glow_overlay_reference"));
        map.put(EchoThemeTextureKey.RENDERCORE_DISTORTION_OVERLAY, texture(TX, "rendercore/distortion_overlay"));
        map.put(EchoThemeTextureKey.RENDERCORE_ENTITY_HIGHLIGHT, texture(TX, "rendercore/entity_highlight_reference"));
        map.put(EchoThemeTextureKey.RENDERCORE_MULTIBLOCK_ENERGY, texture(TX, "rendercore/multiblock_energy_lines"));
        map.put(EchoThemeTextureKey.SCREENCORE_SURFACE_BASE, texture(TX, "screencore/surface_base"));
        map.put(EchoThemeTextureKey.SCREENCORE_SURFACE_RAISED, texture(TX, "screencore/surface_raised"));
        map.put(EchoThemeTextureKey.SCREENCORE_SURFACE_FLOATING, texture(TX, "screencore/surface_floating"));
        map.put(EchoThemeTextureKey.SCREENCORE_BUTTON, texture(TX, "screencore/button"));
        map.put(EchoThemeTextureKey.SCREENCORE_BUTTON_HOVER, texture(TX, "screencore/button_hover"));
        map.put(EchoThemeTextureKey.SCREENCORE_STATUS_CHIP, texture(TX, "screencore/status_chip"));
        map.put(EchoThemeTextureKey.SCREENCORE_PROGRESS_BAR, texture(TX, "screencore/progress_bar"));
        map.put(EchoThemeTextureKey.SCREENCORE_FOCUS_RING, texture(TX, "screencore/focus_ring"));
        map.put(EchoThemeTextureKey.SCREENCORE_CORNER_CUTS, texture(TX, "screencore/corner_cuts"));
        map.put(EchoThemeTextureKey.SCREENCORE_EDGE_RAILS, texture(TX, "screencore/edge_rails"));
        map.put(EchoThemeTextureKey.SCREENCORE_PANEL_SHEEN, texture(TX, "screencore/panel_sheen"));
        map.put(EchoThemeTextureKey.SCREENCORE_MICRO_TICKS, texture(TX, "screencore/micro_ticks"));
        return map;
    }

    private static Identifier texture(String theme, String path) {
        return Identifier.fromNamespaceAndPath(NS, "textures/gui/themes/" + theme + "/" + path + ".png");
    }

    private static Identifier soundcore(String path) {
        return Identifier.fromNamespaceAndPath("echosoundcore", path);
    }

    private static Identifier minecraft(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}
