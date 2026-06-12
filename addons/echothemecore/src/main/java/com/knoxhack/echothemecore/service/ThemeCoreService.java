package com.knoxhack.echothemecore.service;

import com.echoplatform.echocore.api.EchoThemeToken;
import com.echoplatform.echocore.api.IThemeService;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeColorKey;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * ThemeCore implementation of the Core {@link IThemeService}.
 * Bridges dotted token resolution to the client-local theme when present.
 */
public final class ThemeCoreService implements IThemeService {
    public static final ThemeCoreService INSTANCE = new ThemeCoreService();

    private ThemeCoreService() {
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public int resolveColor(String token, int fallback) {
        if (token == null || token.isBlank()) {
            return fallback;
        }
        EchoThemeColorKey key = tokenToColorKey(token);
        if (key == null) {
            return EchoThemeToken.resolveDefault(token, fallback);
        }
        return activeTheme().colors().color(key);
    }

    @Override
    public Optional<Identifier> resolveColorId(String token) {
        EchoThemeColorKey key = tokenToColorKey(token);
        if (key == null) {
            return Optional.empty();
        }
        return Optional.of(Identifier.fromNamespaceAndPath("echothemecore", "color/" + key.name().toLowerCase()));
    }

    @Override
    public Optional<Identifier> resolveTexture(String token) {
        EchoThemeTextureKey key = tokenToTextureKey(token);
        if (key == null) {
            return Optional.empty();
        }
        EchoTheme theme = activeTheme();
        Optional<Identifier> module = theme.moduleTexture(key);
        if (module.isPresent()) {
            return module;
        }
        Optional<Identifier> vanilla = theme.vanillaUiProfile().texture(key);
        if (vanilla.isPresent()) {
            return vanilla;
        }
        return theme.uiAssets().texture(key);
    }

    @Override
    public float resolveFloat(String token, float fallback) {
        if (token == null || token.isBlank()) {
            return fallback;
        }
        EchoTheme theme = activeTheme();
        return switch (token) {
            case "ui.overlay_opacity" -> theme.vanillaUiProfile().overlayOpacity();
            case "ui.panel_opacity" -> theme.vanillaUiProfile().panelOpacity();
            case "ui.edge_glow_strength" -> theme.vanillaUiProfile().edgeGlowStrength();
            case "render.glow_intensity" -> theme.renderProfile().glowIntensity();
            case "render.hologram_opacity" -> theme.renderProfile().hologramOpacity();
            case "render.particle_intensity" -> theme.renderProfile().particleIntensity();
            case "render.animation_intensity" -> theme.renderProfile().animationIntensity();
            default -> fallback;
        };
    }

    @Override
    public List<String> knownTokens() {
        List<String> tokens = new ArrayList<>(EchoThemeToken.defaultDarkColorTokens());
        tokens.addAll(List.of(
            "border.soft",
            "state.success",
            "state.warning",
            "state.error",
            "ui.background",
            "ui.panel",
            "ui.panel_alt",
            "ui.button",
            "ui.button_hover",
            "ui.tooltip",
            "ui.toast",
            "ui.hotbar",
            "ui.container_frame",
            "ui.inventory_frame",
            "ui.creative_frame",
            "ui.widget_outline",
            "ui.selected_slot",
            "ui.boss_bar",
            "ui.pause_panel",
            "loading.background",
            "loading.panel",
            "loading.progress_bar",
            "loading.spinner",
            "loading.logo_mark",
            "menu.main_backplate",
            "menu.pause_panel",
            "menu.options_panel",
            "menu.world_row",
            "menu.mods_panel",
            "hud.hotbar_frame",
            "hud.selected_slot",
            "hud.crosshair_accent",
            "hud.boss_bar",
            "hud.chat_panel",
            "hud.notification_chip",
            "item_icon.frame",
            "item_icon.rarity_ring",
            "item_icon.badge",
            "item_icon.lock_overlay",
            "item_icon.mission_marker",
            "terminal.panel",
            "terminal.tab",
            "terminal.tab.active",
            "terminal.button",
            "terminal.icon",
            "index.background",
            "index.panel",
            "index.panel_alt",
            "index.glass",
            "index.row",
            "index.border",
            "index.border_soft",
            "index.text",
            "index.muted",
            "index.accent",
            "index.accent_secondary",
            "index.success",
            "index.warning",
            "index.error",
            "index.panel_wide",
            "index.panel_active",
            "index.button",
            "index.button_hover",
            "index.card",
            "index.card_selected",
            "index.status_chip",
            "index.progress_bar",
            "index.scrollbar",
            "index.icon",
            "screencore.surface.base",
            "screencore.surface.raised",
            "screencore.surface.floating",
            "screencore.button",
            "screencore.button.hover",
            "screencore.button_hover",
            "screencore.status_chip",
            "screencore.progress_bar",
            "screencore.focus_ring",
            "screencore.corner_cuts",
            "screencore.edge_rails",
            "screencore.panel_sheen",
            "screencore.micro_ticks",
            "holomap.panel",
            "holomap.grid",
            "holomap.route",
            "holomap.marker.signal",
            "holomap.marker.hazard",
            "holomap.marker.mission",
            "holomap.selected_ring",
            "lens.scan_ring",
            "lens.target_box",
            "lens.warning",
            "lens.progress_arc",
            "rendercore.glow_overlay",
            "rendercore.distortion_overlay",
            "rendercore.entity_highlight",
            "rendercore.multiblock_energy",
            "ui.overlay_opacity",
            "ui.panel_opacity",
            "ui.edge_glow_strength",
            "render.glow_intensity",
            "render.hologram_opacity",
            "render.particle_intensity",
            "render.animation_intensity"
        ));
        Collections.sort(tokens);
        return List.copyOf(tokens);
    }

    @Override
    public String currentThemeName() {
        return activeTheme().displayName();
    }

    private static EchoTheme activeTheme() {
        try {
            Class<?> state = Class.forName("com.knoxhack.echothemecore.client.ClientThemeState");
            Object theme = state.getMethod("currentTheme").invoke(null);
            if (theme instanceof EchoTheme echoTheme) {
                return echoTheme;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Dedicated servers and early common setup fall back to the registry catalog state.
        }
        return ThemeRegistry.getCurrentTheme();
    }

    private static EchoThemeColorKey tokenToColorKey(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("index.") && !ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_INDEX)) {
            return null;
        }
        return switch (token) {
            case "index.background" -> EchoThemeColorKey.BACKGROUND;
            case "index.panel" -> EchoThemeColorKey.PANEL;
            case "index.panel_alt" -> EchoThemeColorKey.PANEL_ALT;
            case "index.glass", "index.row" -> EchoThemeColorKey.GLASS;
            case "index.border" -> EchoThemeColorKey.BORDER;
            case "index.border_soft" -> EchoThemeColorKey.BORDER_SOFT;
            case "index.text" -> EchoThemeColorKey.TEXT;
            case "index.muted" -> EchoThemeColorKey.MUTED_TEXT;
            case "index.accent" -> EchoThemeColorKey.PRIMARY;
            case "index.accent_secondary" -> EchoThemeColorKey.ACCENT;
            case "index.success" -> EchoThemeColorKey.SUCCESS;
            case "index.warning" -> EchoThemeColorKey.WARNING;
            case "index.error" -> EchoThemeColorKey.ERROR;
            case "background.primary" -> EchoThemeColorKey.BACKGROUND;
            case "background.secondary" -> EchoThemeColorKey.GLASS;
            case "panel.primary" -> EchoThemeColorKey.PANEL;
            case "panel.secondary" -> EchoThemeColorKey.PANEL_ALT;
            case "panel.raised" -> EchoThemeColorKey.GLASS;
            case "panel.warning" -> EchoThemeColorKey.WARNING;
            case "panel.danger" -> EchoThemeColorKey.ERROR;
            case "ui.panel" -> EchoThemeColorKey.PANEL;
            case "ui.panel_alt" -> EchoThemeColorKey.PANEL_ALT;
            case "ui.glass" -> EchoThemeColorKey.GLASS;
            case "text.primary" -> EchoThemeColorKey.TEXT;
            case "text.muted" -> EchoThemeColorKey.MUTED_TEXT;
            case "text.warning" -> EchoThemeColorKey.WARNING;
            case "text.success" -> EchoThemeColorKey.SUCCESS;
            case "accent.primary" -> EchoThemeColorKey.PRIMARY;
            case "accent.secondary" -> EchoThemeColorKey.ACCENT;
            case "state.locked" -> EchoThemeColorKey.LOCKED;
            case "state.ready" -> EchoThemeColorKey.SUCCESS;
            case "state.active" -> EchoThemeColorKey.PRIMARY;
            case "state.completed" -> EchoThemeColorKey.SUCCESS;
            case "state.failed" -> EchoThemeColorKey.ERROR;
            case "state.success" -> EchoThemeColorKey.SUCCESS;
            case "state.warning" -> EchoThemeColorKey.WARNING;
            case "state.error" -> EchoThemeColorKey.ERROR;
            case "border.primary" -> EchoThemeColorKey.BORDER;
            case "border.soft" -> EchoThemeColorKey.BORDER_SOFT;
            case "border.selected" -> EchoThemeColorKey.SELECTION;
            default -> null;
        };
    }

    private static EchoThemeTextureKey tokenToTextureKey(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("index.") && !ThemeCoreConfig.bool(ThemeCoreConfig.THEME_AFFECTS_INDEX)) {
            return null;
        }
        return switch (token) {
            case "index.panel" -> EchoThemeTextureKey.INDEX_PANEL;
            case "index.panel_wide" -> EchoThemeTextureKey.INDEX_PANEL_WIDE;
            case "index.panel_active" -> EchoThemeTextureKey.INDEX_PANEL_ACTIVE;
            case "index.button" -> EchoThemeTextureKey.INDEX_BUTTON;
            case "index.button_hover" -> EchoThemeTextureKey.INDEX_BUTTON_HOVER;
            case "index.card" -> EchoThemeTextureKey.INDEX_CARD;
            case "index.card_selected" -> EchoThemeTextureKey.INDEX_CARD_SELECTED;
            case "index.status_chip" -> EchoThemeTextureKey.INDEX_STATUS_CHIP;
            case "index.progress_bar" -> EchoThemeTextureKey.INDEX_PROGRESS_BAR;
            case "index.scrollbar" -> EchoThemeTextureKey.INDEX_SCROLLBAR;
            case "index.icon" -> EchoThemeTextureKey.INDEX_ICON;
            case "ui.background" -> EchoThemeTextureKey.VANILLA_BACKGROUND;
            case "ui.panel" -> EchoThemeTextureKey.PANEL;
            case "ui.panel_alt" -> EchoThemeTextureKey.PANEL_ALT;
            case "ui.button" -> EchoThemeTextureKey.BUTTON;
            case "ui.button_hover" -> EchoThemeTextureKey.BUTTON_HOVER;
            case "ui.tooltip" -> EchoThemeTextureKey.VANILLA_TOOLTIP_PANEL;
            case "ui.toast" -> EchoThemeTextureKey.VANILLA_TOAST_ACCENT;
            case "ui.hotbar" -> EchoThemeTextureKey.VANILLA_HOTBAR;
            case "ui.container_frame" -> EchoThemeTextureKey.VANILLA_CONTAINER_FRAME;
            case "ui.inventory_frame" -> EchoThemeTextureKey.VANILLA_INVENTORY_FRAME;
            case "ui.creative_frame" -> EchoThemeTextureKey.VANILLA_CREATIVE_FRAME;
            case "ui.widget_outline" -> EchoThemeTextureKey.VANILLA_WIDGET_OUTLINE;
            case "ui.selected_slot" -> EchoThemeTextureKey.VANILLA_SELECTED_SLOT;
            case "ui.boss_bar" -> EchoThemeTextureKey.VANILLA_BOSS_BAR_ACCENT;
            case "ui.title_backplate" -> EchoThemeTextureKey.VANILLA_TITLE_BACKPLATE;
            case "ui.pause_panel" -> EchoThemeTextureKey.VANILLA_PAUSE_PANEL;
            case "loading.background" -> EchoThemeTextureKey.LOADING_BACKGROUND;
            case "loading.panel" -> EchoThemeTextureKey.LOADING_PANEL;
            case "loading.progress_bar" -> EchoThemeTextureKey.LOADING_PROGRESS_BAR;
            case "loading.spinner" -> EchoThemeTextureKey.LOADING_SPINNER;
            case "loading.logo_mark" -> EchoThemeTextureKey.LOADING_LOGO_MARK;
            case "menu.main_backplate" -> EchoThemeTextureKey.MENU_MAIN_BACKPLATE;
            case "menu.pause_panel" -> EchoThemeTextureKey.MENU_PAUSE_PANEL;
            case "menu.options_panel" -> EchoThemeTextureKey.MENU_OPTIONS_PANEL;
            case "menu.world_row" -> EchoThemeTextureKey.MENU_WORLD_ROW;
            case "menu.mods_panel" -> EchoThemeTextureKey.MENU_MODS_PANEL;
            case "hud.hotbar_frame" -> EchoThemeTextureKey.HUD_HOTBAR_FRAME;
            case "hud.selected_slot" -> EchoThemeTextureKey.HUD_SELECTED_SLOT;
            case "hud.crosshair_accent" -> EchoThemeTextureKey.HUD_CROSSHAIR_ACCENT;
            case "hud.boss_bar" -> EchoThemeTextureKey.HUD_BOSS_BAR;
            case "hud.chat_panel" -> EchoThemeTextureKey.HUD_CHAT_PANEL;
            case "hud.notification_chip" -> EchoThemeTextureKey.HUD_NOTIFICATION_CHIP;
            case "item_icon.frame" -> EchoThemeTextureKey.ITEM_ICON_FRAME;
            case "item_icon.rarity_ring" -> EchoThemeTextureKey.ITEM_ICON_RARITY_RING;
            case "item_icon.badge" -> EchoThemeTextureKey.ITEM_ICON_BADGE;
            case "item_icon.lock_overlay" -> EchoThemeTextureKey.ITEM_ICON_LOCK_OVERLAY;
            case "item_icon.mission_marker" -> EchoThemeTextureKey.ITEM_ICON_MISSION_MARKER;
            case "terminal.panel" -> EchoThemeTextureKey.TERMINAL_PANEL;
            case "terminal.tab" -> EchoThemeTextureKey.TERMINAL_TAB;
            case "terminal.tab.active", "terminal.tab_active" -> EchoThemeTextureKey.TERMINAL_TAB_ACTIVE;
            case "terminal.button" -> EchoThemeTextureKey.TERMINAL_BUTTON;
            case "terminal.icon" -> EchoThemeTextureKey.TERMINAL_ICON;
            case "holomap.panel" -> EchoThemeTextureKey.HOLOMAP_PANEL;
            case "holomap.grid" -> EchoThemeTextureKey.HOLOMAP_GRID;
            case "holomap.route" -> EchoThemeTextureKey.HOLOMAP_ROUTE;
            case "holomap.marker.signal" -> EchoThemeTextureKey.HOLOMAP_MARKER_SIGNAL;
            case "holomap.marker.hazard" -> EchoThemeTextureKey.HOLOMAP_MARKER_HAZARD;
            case "holomap.marker.mission" -> EchoThemeTextureKey.HOLOMAP_MARKER_MISSION;
            case "holomap.selected_ring" -> EchoThemeTextureKey.HOLOMAP_SELECTED_RING;
            case "lens.scan_ring" -> EchoThemeTextureKey.LENS_SCAN_RING;
            case "lens.target_box" -> EchoThemeTextureKey.LENS_TARGET_BOX;
            case "lens.warning" -> EchoThemeTextureKey.LENS_WARNING;
            case "lens.progress_arc" -> EchoThemeTextureKey.LENS_PROGRESS_ARC;
            case "rendercore.glow_overlay" -> EchoThemeTextureKey.RENDERCORE_GLOW_OVERLAY;
            case "rendercore.distortion_overlay" -> EchoThemeTextureKey.RENDERCORE_DISTORTION_OVERLAY;
            case "rendercore.entity_highlight" -> EchoThemeTextureKey.RENDERCORE_ENTITY_HIGHLIGHT;
            case "rendercore.multiblock_energy" -> EchoThemeTextureKey.RENDERCORE_MULTIBLOCK_ENERGY;
            case "screencore.surface.base" -> EchoThemeTextureKey.SCREENCORE_SURFACE_BASE;
            case "screencore.surface.raised" -> EchoThemeTextureKey.SCREENCORE_SURFACE_RAISED;
            case "screencore.surface.floating" -> EchoThemeTextureKey.SCREENCORE_SURFACE_FLOATING;
            case "screencore.button" -> EchoThemeTextureKey.SCREENCORE_BUTTON;
            case "screencore.button.hover", "screencore.button_hover" -> EchoThemeTextureKey.SCREENCORE_BUTTON_HOVER;
            case "screencore.status_chip" -> EchoThemeTextureKey.SCREENCORE_STATUS_CHIP;
            case "screencore.progress_bar" -> EchoThemeTextureKey.SCREENCORE_PROGRESS_BAR;
            case "screencore.focus_ring" -> EchoThemeTextureKey.SCREENCORE_FOCUS_RING;
            case "screencore.corner_cuts" -> EchoThemeTextureKey.SCREENCORE_CORNER_CUTS;
            case "screencore.edge_rails" -> EchoThemeTextureKey.SCREENCORE_EDGE_RAILS;
            case "screencore.panel_sheen" -> EchoThemeTextureKey.SCREENCORE_PANEL_SHEEN;
            case "screencore.micro_ticks" -> EchoThemeTextureKey.SCREENCORE_MICRO_TICKS;
            default -> null;
        };
    }
}
