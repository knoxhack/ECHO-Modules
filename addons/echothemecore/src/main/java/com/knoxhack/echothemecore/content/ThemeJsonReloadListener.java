package com.knoxhack.echothemecore.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.DistortionStyle;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeBlockPalette;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.api.EchoThemeRenderProfile;
import com.knoxhack.echothemecore.api.EchoThemeSoundProfile;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.api.EchoThemeUiAssets;
import com.knoxhack.echothemecore.api.EchoThemeVanillaUiProfile;
import com.knoxhack.echothemecore.api.HologramStyle;
import com.knoxhack.echothemecore.api.ParticleStyle;
import com.knoxhack.echothemecore.api.TransitionStyle;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ThemeJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, EchoTheme>> {
    private static final List<String> THEME_DIRS = List.of("themes", EchoThemeCore.MODID + "/themes");

    @Override
    protected Map<Identifier, EchoTheme> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, EchoTheme> themes = new LinkedHashMap<>();
        for (String themeDir : THEME_DIRS) {
            for (Map.Entry<Identifier, Resource> entry : manager.listResources(themeDir, id -> id.getPath().endsWith(".json")).entrySet()) {
                Identifier resourceId = entry.getKey();
                Identifier fallbackId = contentId(resourceId, themeDir);
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (!root.isJsonObject()) {
                        throw new JsonParseException("Root must be a JSON object.");
                    }
                    EchoTheme theme = parseTheme(fallbackId, root.getAsJsonObject());
                    if (themes.putIfAbsent(theme.id(), theme) != null) {
                        EchoThemeCore.LOGGER.warn("Duplicate ThemeCore theme id {} from {} ignored.", theme.id(), resourceId);
                    } else if (ThemeCoreConfig.debugThemeLogging()) {
                        EchoThemeCore.LOGGER.info("Loaded ThemeCore theme {} from {}.", theme.id(), resourceId);
                    }
                } catch (IOException | RuntimeException exception) {
                    EchoThemeCore.LOGGER.warn("Could not parse ThemeCore theme file {}.", resourceId, exception);
                }
            }
        }
        return themes;
    }

    @Override
    protected void apply(Map<Identifier, EchoTheme> themes, ResourceManager manager, ProfilerFiller profiler) {
        ThemeRegistry.replaceLoaded(themes);
    }

    public static EchoTheme parseThemeForTests(Identifier fallbackId, JsonObject json) {
        return parseTheme(fallbackId, json);
    }

    private static EchoTheme parseTheme(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        JsonObject colorsJson = requiredObject(json, "colors", id);
        EchoThemeColors colors = parseColors(colorsJson);
        EchoThemeUiAssets ui = parseUi(object(json, "ui"), id);
        EchoThemeRenderProfile render = parseRender(object(json, "render"), colors);
        EchoThemeSoundProfile sound = parseSound(object(json, "sound"));
        EchoThemeBlockPalette blocks = parseBlocks(object(json, "block_palette"));
        EchoThemeVanillaUiProfile vanilla = parseVanilla(object(json, "vanilla_ui"), colors, ui, render);
        JsonObject moduleAssets = object(json, "module_assets");
        Map<EchoThemeTextureKey, Identifier> moduleTextures = parseModuleTextures(moduleAssets, id);
        Map<String, String> metadata = new java.util.LinkedHashMap<>(stringMap(object(json, "metadata")));
        appendItemIconReplacements(metadata, object(moduleAssets, "item_icon"));
        return new EchoTheme(
            id,
            string(json, "display_name", id.getPath()),
            string(json, "description", ""),
            colors,
            ui,
            render,
            sound,
            blocks,
            vanilla,
            moduleTextures,
            metadata
        );
    }

    private static EchoThemeColors parseColors(JsonObject json) {
        return new EchoThemeColors(
            color(json, "primary", 0xFF00E5FF),
            color(json, "secondary", 0xFFB44CFF),
            color(json, "accent", 0xFFFF2BD6),
            color(json, "background", 0xFF030711),
            color(json, "panel", 0xCC08111F),
            color(json, "panel_alt", 0xCC0D1A2E),
            color(json, "glass", 0x8810243A),
            color(json, "border", 0xFF2BEAFF),
            color(json, "border_soft", 0xFF1A6F8A),
            color(json, "text", 0xFFEAFBFF),
            color(json, "muted_text", 0xFF8AAFC2),
            color(json, "success", 0xFF45FFB0),
            color(json, "warning", 0xFFFFD166),
            color(json, "error", 0xFFFF4D6D),
            color(json, "locked", 0xFF3B4652),
            color(json, "glow", 0xFF00E5FF),
            color(json, "selection", 0xFFB44CFF)
        );
    }

    private static EchoThemeUiAssets parseUi(JsonObject json, Identifier id) {
        String themeFolder = id.getPath();
        return new EchoThemeUiAssets(
            texture(json, "background_texture", themeFolder, "background"),
            texture(json, "panel_texture", themeFolder, "glass_panel"),
            texture(json, "panel_alt_texture", themeFolder, "glass_panel_alt"),
            texture(json, "button_texture", themeFolder, "glass_button"),
            texture(json, "button_hover_texture", themeFolder, "glass_button_hover"),
            texture(json, "tab_texture", themeFolder, "tab"),
            texture(json, "tab_active_texture", themeFolder, "tab_active"),
            texture(json, "mission_card_texture", themeFolder, "mission_card"),
            texture(json, "mission_card_selected_texture", themeFolder, "mission_card_selected"),
            texture(json, "status_chip_texture", themeFolder, "status_chip"),
            texture(json, "progress_bar_texture", themeFolder, "progress_bar"),
            texture(json, "scrollbar_texture", themeFolder, "scrollbar"),
            texture(json, "icon_pack", themeFolder, "icons/icon_theme"),
            texture(json, "hologram_overlay", themeFolder, "hologram_overlay"),
            texture(json, "energy_overlay", themeFolder, "energy_overlay"),
            texture(json, "edge_glow", themeFolder, "edge_glow"),
            texture(json, "particle_glints", themeFolder, "particle_glints"),
            texture(json, "locked_overlay", themeFolder, "locked_overlay")
        );
    }

    private static EchoThemeRenderProfile parseRender(JsonObject json, EchoThemeColors colors) {
        return new EchoThemeRenderProfile(
            color(json, "hologram_color", colors.primary()),
            color(json, "hologram_secondary", colors.secondary()),
            color(json, "particle_primary", colors.primary()),
            color(json, "particle_secondary", colors.accent()),
            color(json, "emissive_primary", colors.primary()),
            color(json, "emissive_secondary", colors.secondary()),
            color(json, "edge_glow_color", colors.glow()),
            color(json, "warning_glow_color", colors.warning()),
            color(json, "success_glow_color", colors.success()),
            color(json, "error_glow_color", colors.error()),
            color(json, "distortion_color", colors.secondary()),
            decimal(json, "glow_intensity", 0.85F),
            decimal(json, "emissive_intensity", 0.9F),
            decimal(json, "hologram_opacity", 0.68F),
            decimal(json, "glass_opacity", 0.62F),
            decimal(json, "noise_strength", 0.0F),
            decimal(json, "distortion_strength", 0.0F),
            decimal(json, "edge_glow_strength", 0.75F),
            decimal(json, "hologram_pulse_strength", 0.45F),
            decimal(json, "energy_pattern_strength", 0.35F),
            decimal(json, "particle_intensity", 0.65F),
            decimal(json, "animation_intensity", 0.75F),
            decimal(json, "pulse_speed", 0.85F),
            HologramStyle.byName(string(json, "hologram_style", "CYBER_GLASS"), HologramStyle.CYBER_GLASS),
            ParticleStyle.byName(string(json, "particle_style", "SOFT_GLINTS"), ParticleStyle.SOFT_GLINTS),
            DistortionStyle.byName(string(json, "distortion_style", "NONE"), DistortionStyle.NONE),
            string(json, "overlay_style", "GLASS_GEOMETRIC"),
            TransitionStyle.byName(string(json, "transition_style", "GLASS_FADE"), TransitionStyle.GLASS_FADE)
        );
    }

    private static EchoThemeSoundProfile parseSound(JsonObject json) {
        return new EchoThemeSoundProfile(
            optionalId(json, "ui_click"),
            optionalId(json, "ui_error"),
            optionalId(json, "ui_open"),
            optionalId(json, "ui_close"),
            optionalId(json, "theme_music"),
            optionalId(json, "stinger_confirm"),
            optionalId(json, "stinger_warning")
        );
    }

    private static EchoThemeBlockPalette parseBlocks(JsonObject json) {
        return new EchoThemeBlockPalette(
            idArray(json, "recommended_blocks"),
            idArray(json, "panel_blocks"),
            idArray(json, "glass_blocks"),
            idArray(json, "light_blocks"),
            idArray(json, "accent_blocks")
        );
    }

    private static EchoThemeVanillaUiProfile parseVanilla(JsonObject json, EchoThemeColors colors, EchoThemeUiAssets ui, EchoThemeRenderProfile render) {
        if (json == null) {
            return EchoThemeVanillaUiProfile.fromParts(colors, ui, render);
        }
        EchoThemeVanillaUiProfile fallback = EchoThemeVanillaUiProfile.fromParts(colors, ui, render);
        return new EchoThemeVanillaUiProfile(
            identifier(json, "background_texture", fallback.backgroundTexture()),
            identifier(json, "panel_texture", fallback.panelTexture()),
            identifier(json, "button_texture", fallback.buttonTexture()),
            identifier(json, "hotbar_texture", fallback.hotbarTexture()),
            identifier(json, "tooltip_texture", fallback.tooltipTexture()),
            identifier(json, "toast_texture", fallback.toastTexture()),
            identifier(json, "boss_bar_texture", fallback.bossBarTexture()),
            color(json, "background_tint", fallback.backgroundTint()),
            color(json, "panel_tint", fallback.panelTint()),
            color(json, "tooltip_tint", fallback.tooltipTint()),
            color(json, "widget_accent", fallback.widgetAccent()),
            color(json, "hotbar_accent", fallback.hotbarAccent()),
            color(json, "chat_accent", fallback.chatAccent()),
            decimal(json, "overlay_opacity", fallback.overlayOpacity()),
            decimal(json, "panel_opacity", fallback.panelOpacity()),
            decimal(json, "edge_glow_strength", fallback.edgeGlowStrength()),
            bool(json, "reduce_vanilla_brown", fallback.reduceVanillaBrown())
        );
    }

    private static Map<EchoThemeTextureKey, Identifier> parseModuleTextures(JsonObject json, Identifier themeId) {
        if (json == null) {
            return Map.of();
        }
        String themeFolder = themeId.getPath();
        Map<EchoThemeTextureKey, Identifier> result = new EnumMap<>(EchoThemeTextureKey.class);
        JsonObject terminal = object(json, "terminal");
        if (terminal != null) {
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_PANEL, "panel_texture", themeFolder, "glass_panel");
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_TAB, "tab_texture", themeFolder, "tab");
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_TAB_ACTIVE, "tab_active_texture", themeFolder, "tab_active");
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_MISSION_CARD, "mission_card_texture", themeFolder, "mission_card");
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_STATUS_CHIP, "status_chip_texture", themeFolder, "status_chip");
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_BUTTON, "button_texture", themeFolder, "glass_button");
            putIfPresent(result, terminal, EchoThemeTextureKey.TERMINAL_ICON, "icon_texture", themeFolder, "icons/icon_terminal");
        }
        JsonObject index = object(json, "index");
        if (index != null) {
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_PANEL, "panel_texture", themeFolder, "index_panel");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_PANEL_WIDE, "panel_wide_texture", themeFolder, "index_panel_wide");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_PANEL_ACTIVE, "panel_active_texture", themeFolder, "index_panel_active");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_BUTTON, "button_texture", themeFolder, "index_button");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_BUTTON_HOVER, "button_hover_texture", themeFolder, "index_button_hover");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_CARD, "card_texture", themeFolder, "index_card");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_CARD_SELECTED, "card_selected_texture", themeFolder, "index_card_selected");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_STATUS_CHIP, "status_chip_texture", themeFolder, "index_status_chip");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_PROGRESS_BAR, "progress_bar_texture", themeFolder, "index_progress_bar");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_SCROLLBAR, "scrollbar_texture", themeFolder, "index_scrollbar");
            putIfPresent(result, index, EchoThemeTextureKey.INDEX_ICON, "icon_texture", themeFolder, "icons/icon_index");
        }
        JsonObject holomap = object(json, "holomap");
        if (holomap != null) {
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_GRID, "grid_texture", themeFolder, "holomap_grid");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_PANEL, "panel_texture", themeFolder, "holomap_panel");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_ROUTE, "route_texture", themeFolder, "route_line");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_MARKER_SIGNAL, "marker_signal_texture", themeFolder, "marker_signal");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_MARKER_HAZARD, "marker_hazard_texture", themeFolder, "marker_hazard");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_MARKER_MISSION, "marker_mission_texture", themeFolder, "marker_mission");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_MARKER_NEXUS, "marker_nexus_texture", themeFolder, "marker_nexus");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_MARKER_RECLAIMED, "marker_reclaimed_texture", themeFolder, "marker_reclamation");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_SELECTED_RING, "selected_ring_texture", themeFolder, "selected_marker_ring");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_DANGER, "danger_overlay_texture", themeFolder, "marker_hazard");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_ANOMALY, "anomaly_overlay_texture", themeFolder, "marker_nexus");
            putIfPresent(result, holomap, EchoThemeTextureKey.HOLOMAP_RECLAIMED, "reclaimed_overlay_texture", themeFolder, "marker_reclamation");
        }
        JsonObject lens = object(json, "lens");
        if (lens != null) {
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_SCAN_RING, "scan_ring_texture", themeFolder, "lens_scan_ring");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_TARGET_BOX, "target_box_texture", themeFolder, "lens_target_box");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_WEAK_POINT, "weak_point_texture", themeFolder, "lens_weakpoint_marker");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_WARNING, "warning_texture", themeFolder, "lens_warning_overlay");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_ANOMALY_REVEAL, "anomaly_reveal_texture", themeFolder, "lens_anomaly_overlay");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_COMPLETION_PULSE, "completion_pulse_texture", themeFolder, "lens_progress_arc");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_PROGRESS_ARC, "progress_arc_texture", themeFolder, "lens_progress_arc");
            putIfPresent(result, lens, EchoThemeTextureKey.LENS_NOISE_OVERLAY, "noise_overlay_texture", themeFolder, "lens_noise_overlay");
        }
        JsonObject vanilla = object(json, "vanilla_ui");
        if (vanilla != null) {
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_CONTAINER_FRAME, "container_frame_texture", themeFolder, "vanilla_container_frame");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_INVENTORY_FRAME, "inventory_frame_texture", themeFolder, "vanilla_inventory_frame");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_CREATIVE_FRAME, "creative_frame_texture", themeFolder, "vanilla_inventory_frame");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_TITLE_BACKPLATE, "title_backplate_texture", themeFolder, "vanilla_title_backplate");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_PAUSE_PANEL, "pause_panel_texture", themeFolder, "vanilla_pause_panel");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_SELECTED_SLOT, "selected_slot_texture", themeFolder, "vanilla_hotbar_accent");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_TOOLTIP_PANEL, "tooltip_panel_texture", themeFolder, "vanilla_tooltip_panel");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_TOAST_ACCENT, "toast_accent_texture", themeFolder, "vanilla_toast_accent");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_BOSS_BAR_ACCENT, "boss_bar_accent_texture", themeFolder, "vanilla_boss_bar_accent");
            putIfPresent(result, vanilla, EchoThemeTextureKey.VANILLA_WIDGET_OUTLINE, "widget_outline_texture", themeFolder, "vanilla_widget_outline");
        }
        JsonObject loading = object(json, "loading");
        if (loading != null) {
            putIfPresent(result, loading, EchoThemeTextureKey.LOADING_BACKGROUND, "background_texture", themeFolder, "loading/background");
            putIfPresent(result, loading, EchoThemeTextureKey.LOADING_PANEL, "panel_texture", themeFolder, "loading/panel");
            putIfPresent(result, loading, EchoThemeTextureKey.LOADING_PROGRESS_BAR, "progress_bar_texture", themeFolder, "loading/progress_bar");
            putIfPresent(result, loading, EchoThemeTextureKey.LOADING_SPINNER, "spinner_texture", themeFolder, "loading/spinner");
            putIfPresent(result, loading, EchoThemeTextureKey.LOADING_LOGO_MARK, "logo_mark_texture", themeFolder, "loading/logo_mark");
        }
        JsonObject menu = object(json, "menu");
        if (menu != null) {
            putIfPresent(result, menu, EchoThemeTextureKey.MENU_MAIN_BACKPLATE, "main_backplate_texture", themeFolder, "menu/main_backplate");
            putIfPresent(result, menu, EchoThemeTextureKey.MENU_PAUSE_PANEL, "pause_panel_texture", themeFolder, "menu/pause_panel");
            putIfPresent(result, menu, EchoThemeTextureKey.MENU_OPTIONS_PANEL, "options_panel_texture", themeFolder, "menu/options_panel");
            putIfPresent(result, menu, EchoThemeTextureKey.MENU_WORLD_ROW, "world_row_texture", themeFolder, "menu/world_row");
            putIfPresent(result, menu, EchoThemeTextureKey.MENU_MODS_PANEL, "mods_panel_texture", themeFolder, "menu/mods_panel");
        }
        JsonObject hud = object(json, "hud");
        if (hud != null) {
            putIfPresent(result, hud, EchoThemeTextureKey.HUD_HOTBAR_FRAME, "hotbar_frame_texture", themeFolder, "hud/hotbar_frame");
            putIfPresent(result, hud, EchoThemeTextureKey.HUD_SELECTED_SLOT, "selected_slot_texture", themeFolder, "hud/selected_slot");
            putIfPresent(result, hud, EchoThemeTextureKey.HUD_CROSSHAIR_ACCENT, "crosshair_accent_texture", themeFolder, "hud/crosshair_accent");
            putIfPresent(result, hud, EchoThemeTextureKey.HUD_BOSS_BAR, "boss_bar_texture", themeFolder, "hud/boss_bar");
            putIfPresent(result, hud, EchoThemeTextureKey.HUD_CHAT_PANEL, "chat_panel_texture", themeFolder, "hud/chat_panel");
            putIfPresent(result, hud, EchoThemeTextureKey.HUD_NOTIFICATION_CHIP, "notification_chip_texture", themeFolder, "hud/notification_chip");
        }
        JsonObject itemIcon = object(json, "item_icon");
        if (itemIcon != null) {
            putIfPresent(result, itemIcon, EchoThemeTextureKey.ITEM_ICON_FRAME, "frame_texture", themeFolder, "item_icon/frame");
            putIfPresent(result, itemIcon, EchoThemeTextureKey.ITEM_ICON_RARITY_RING, "rarity_ring_texture", themeFolder, "item_icon/rarity_ring");
            putIfPresent(result, itemIcon, EchoThemeTextureKey.ITEM_ICON_BADGE, "badge_texture", themeFolder, "item_icon/badge");
            putIfPresent(result, itemIcon, EchoThemeTextureKey.ITEM_ICON_LOCK_OVERLAY, "lock_overlay_texture", themeFolder, "item_icon/lock_overlay");
            putIfPresent(result, itemIcon, EchoThemeTextureKey.ITEM_ICON_MISSION_MARKER, "mission_marker_texture", themeFolder, "item_icon/mission_marker");
        }
        JsonObject rendercore = object(json, "rendercore");
        if (rendercore != null) {
            putIfPresent(result, rendercore, EchoThemeTextureKey.RENDERCORE_GLOW_OVERLAY, "glow_overlay_texture", themeFolder, "rendercore/glow_overlay_reference");
            putIfPresent(result, rendercore, EchoThemeTextureKey.RENDERCORE_DISTORTION_OVERLAY, "distortion_overlay_texture", themeFolder, "rendercore/distortion_overlay");
            putIfPresent(result, rendercore, EchoThemeTextureKey.RENDERCORE_ENTITY_HIGHLIGHT, "entity_highlight_texture", themeFolder, "rendercore/entity_highlight_reference");
            putIfPresent(result, rendercore, EchoThemeTextureKey.RENDERCORE_MULTIBLOCK_ENERGY, "multiblock_energy_texture", themeFolder, "rendercore/multiblock_energy_lines");
        }
        JsonObject screenCore = object(json, "screen_core");
        if (screenCore != null) {
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_SURFACE_BASE, "surface_base_texture", themeFolder, "screencore/surface_base");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_SURFACE_RAISED, "surface_raised_texture", themeFolder, "screencore/surface_raised");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_SURFACE_FLOATING, "surface_floating_texture", themeFolder, "screencore/surface_floating");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_BUTTON, "button_texture", themeFolder, "screencore/button");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_BUTTON_HOVER, "button_hover_texture", themeFolder, "screencore/button_hover");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_STATUS_CHIP, "status_chip_texture", themeFolder, "screencore/status_chip");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_PROGRESS_BAR, "progress_bar_texture", themeFolder, "screencore/progress_bar");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_FOCUS_RING, "focus_ring_texture", themeFolder, "screencore/focus_ring");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_CORNER_CUTS, "corner_cuts_texture", themeFolder, "screencore/corner_cuts");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_EDGE_RAILS, "edge_rails_texture", themeFolder, "screencore/edge_rails");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_PANEL_SHEEN, "panel_sheen_texture", themeFolder, "screencore/panel_sheen");
            putIfPresent(result, screenCore, EchoThemeTextureKey.SCREENCORE_MICRO_TICKS, "micro_ticks_texture", themeFolder, "screencore/micro_ticks");
        }
        return result;
    }

    private static void putIfPresent(Map<EchoThemeTextureKey, Identifier> map, JsonObject json, EchoThemeTextureKey key, String jsonKey, String themeFolder, String fallbackName) {
        Identifier parsed = Identifier.tryParse(string(json, jsonKey, ""));
        if (parsed == null) {
            parsed = Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, "textures/gui/themes/" + themeFolder + "/" + fallbackName + ".png");
        }
        map.put(key, parsed);
    }

    private static Identifier contentId(Identifier resourceId, String folder) {
        String path = resourceId.getPath();
        String prefix = folder + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonObject requiredObject(JsonObject json, String key, Identifier id) {
        JsonObject object = object(json, key);
        if (object == null) {
            throw new JsonParseException("Theme " + id + " is missing required object '" + key + "'.");
        }
        return object;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    private static float decimal(JsonObject json, String key, float fallback) {
        JsonElement element = json == null ? null : json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return Math.max(0.0F, Math.min(2.0F, element.getAsFloat()));
    }

    private static int color(JsonObject json, String key, int fallback) {
        return EchoThemeColors.parseHex(string(json, key, null), fallback);
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        Identifier parsed = Identifier.tryParse(string(json, key, ""));
        return parsed == null ? fallback : parsed;
    }

    private static Identifier optionalId(JsonObject json, String key) {
        return Identifier.tryParse(string(json, key, ""));
    }

    private static Identifier texture(JsonObject json, String key, String themeFolder, String fallbackName) {
        Identifier parsed = Identifier.tryParse(string(json, key, ""));
        return parsed == null
            ? Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, "textures/gui/themes/" + themeFolder + "/" + fallbackName + ".png")
            : parsed;
    }

    private static List<Identifier> idArray(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<Identifier> ids = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            Identifier id = Identifier.tryParse(entry.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private static Map<String, String> stringMap(JsonObject json) {
        if (json == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonNull()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return result;
    }

    private static void appendItemIconReplacements(Map<String, String> metadata, JsonObject itemIcon) {
        JsonObject replacements = object(itemIcon, "replacements");
        if (metadata == null || replacements == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : replacements.entrySet()) {
            if (entry.getValue().isJsonNull() || !entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString()) {
                continue;
            }
            Identifier itemId = Identifier.tryParse(entry.getKey());
            Identifier texture = Identifier.tryParse(entry.getValue().getAsString());
            if (itemId != null && texture != null) {
                metadata.put("item_icon.replacement." + itemId, texture.toString());
            }
        }
    }
}
