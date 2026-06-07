package com.knoxhack.echothemecore.api;

import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.knoxhack.echothemecore.content.RenderPresetRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class EchoThemeApi {
    private EchoThemeApi() {
    }

    public static EchoTheme getTheme() {
        return ThemeRegistry.getCurrentTheme();
    }

    public static EchoTheme getTheme(Player player) {
        return ThemeRegistry.getThemeFor(player);
    }

    public static EchoTheme getTheme(Identifier id) {
        return ThemeRegistry.get(id);
    }

    public static EchoTheme getClientTheme() {
        try {
            Class<?> state = Class.forName("com.knoxhack.echothemecore.client.ClientThemeState");
            Object theme = state.getMethod("currentTheme").invoke(null);
            if (theme instanceof EchoTheme echoTheme) {
                return echoTheme;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Dedicated servers and early common setup use the registry fallback.
        }
        return ThemeRegistry.getCurrentTheme();
    }

    public static Identifier getClientThemeId() {
        return getClientTheme().id();
    }

    public static EchoTheme setClientTheme(Identifier id) {
        try {
            Class<?> state = Class.forName("com.knoxhack.echothemecore.client.ClientThemeState");
            Object theme = state.getMethod("setTheme", Identifier.class).invoke(null, id);
            if (theme instanceof EchoTheme echoTheme) {
                return echoTheme;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // No local client state is available on dedicated servers.
        }
        return ThemeRegistry.get(id);
    }

    public static EchoTheme cycleClientTheme(int direction) {
        try {
            Class<?> state = Class.forName("com.knoxhack.echothemecore.client.ClientThemeState");
            Object theme = state.getMethod("cycleTheme", int.class).invoke(null, direction);
            if (theme instanceof EchoTheme echoTheme) {
                return echoTheme;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // No local client state is available on dedicated servers.
        }
        return ThemeRegistry.nextPublicTheme(ThemeRegistry.globalThemeId(), direction);
    }

    public static List<EchoTheme> listPublicClientThemes() {
        return ThemeRegistry.listPublicThemes();
    }

    public static Identifier getThemeId(Player player) {
        return getTheme(player).id();
    }

    public static void setGlobalTheme(Identifier id) {
        ThemeRegistry.setGlobalTheme(id);
    }

    public static void setPlayerTheme(UUID playerId, Identifier id) {
        ThemeRegistry.setPlayerTheme(playerId, id);
    }

    public static List<EchoTheme> getThemes() {
        return ThemeRegistry.listThemes();
    }

    public static List<EchoThemeRenderPreset> getRenderPresets() {
        return RenderPresetRegistry.listPresets();
    }

    public static List<EchoThemeRenderPreset> getRenderPresets(Identifier themeId) {
        return RenderPresetRegistry.forTheme(themeId);
    }

    public static Optional<EchoThemeRenderPreset> getRenderPreset(Identifier id) {
        return RenderPresetRegistry.find(id);
    }

    public static EchoThemeColors getColors(Player player) {
        return getTheme(player).colors();
    }

    public static EchoThemeRenderProfile getRenderProfile(Player player) {
        return getTheme(player).renderProfile();
    }

    public static EchoThemeVanillaUiProfile getVanillaUiProfile(Player player) {
        return getTheme(player).vanillaUiProfile();
    }

    public static EchoThemeTokenProfile getTokenProfile(EchoTheme theme) {
        return EchoThemeTokenProfile.fromTheme(theme);
    }

    public static EchoThemeTokenProfile getTokenProfile(Player player) {
        return EchoThemeTokenProfile.fromTheme(getTheme(player));
    }

    public static EchoThemeTokenProfile getClientTokenProfile() {
        return EchoThemeTokenProfile.fromTheme(getClientTheme());
    }

    public static EchoThemeContrastReport getContrastReport(EchoTheme theme) {
        return EchoThemeAccessibility.report(theme);
    }

    public static EchoThemeContrastReport getContrastReport(Player player) {
        return EchoThemeAccessibility.report(getTheme(player));
    }

    public static ThemeVisualSettings getEffectiveVisualSettings(Player player) {
        return ThemeVisualSettings.resolve(getTheme(player));
    }

    public static int color(Player player, EchoThemeColorKey key) {
        return getTheme(player).colors().color(key);
    }

    public static Optional<Identifier> getTexture(Player player, EchoThemeTextureKey key) {
        EchoTheme theme = getTheme(player);
        Optional<Identifier> vanilla = theme.vanillaUiProfile().texture(key);
        if (vanilla.isPresent()) {
            return vanilla;
        }
        Optional<Identifier> ui = theme.uiAssets().texture(key);
        if (ui.isPresent()) {
            return ui;
        }
        return theme.moduleTexture(key);
    }

    public static Optional<Identifier> getModuleTexture(Player player, EchoThemeTextureKey key) {
        EchoTheme theme = getTheme(player);
        Optional<Identifier> direct = theme.moduleTexture(key);
        if (direct.isPresent()) {
            return direct;
        }
        return switch (key) {
            case TERMINAL_PANEL, TERMINAL_TAB, TERMINAL_TAB_ACTIVE, TERMINAL_MISSION_CARD,
                 TERMINAL_STATUS_CHIP, TERMINAL_BUTTON -> theme.uiAssets().texture(EchoThemeTextureKey.PANEL);
            case TERMINAL_ICON -> theme.uiAssets().texture(EchoThemeTextureKey.ICON_PACK);
            case INDEX_PANEL, INDEX_PANEL_WIDE, INDEX_CARD -> theme.uiAssets().texture(EchoThemeTextureKey.PANEL);
            case INDEX_PANEL_ACTIVE, INDEX_CARD_SELECTED -> theme.uiAssets().texture(EchoThemeTextureKey.PANEL_ALT);
            case INDEX_BUTTON -> theme.uiAssets().texture(EchoThemeTextureKey.BUTTON);
            case INDEX_BUTTON_HOVER -> theme.uiAssets().texture(EchoThemeTextureKey.BUTTON_HOVER);
            case INDEX_STATUS_CHIP -> theme.uiAssets().texture(EchoThemeTextureKey.STATUS_CHIP);
            case INDEX_PROGRESS_BAR -> theme.uiAssets().texture(EchoThemeTextureKey.PROGRESS_BAR);
            case INDEX_SCROLLBAR -> theme.uiAssets().texture(EchoThemeTextureKey.SCROLLBAR);
            case INDEX_ICON -> theme.uiAssets().texture(EchoThemeTextureKey.ICON_PACK);
            case HOLOMAP_GRID, HOLOMAP_PANEL, HOLOMAP_ROUTE, HOLOMAP_MARKER_SIGNAL,
                 HOLOMAP_MARKER_HAZARD, HOLOMAP_MARKER_MISSION, HOLOMAP_MARKER_NEXUS,
                 HOLOMAP_MARKER_RECLAIMED, HOLOMAP_SELECTED_RING, HOLOMAP_DANGER,
                 HOLOMAP_ANOMALY, HOLOMAP_RECLAIMED ->
                theme.uiAssets().texture(EchoThemeTextureKey.PANEL);
            case LENS_SCAN_RING, LENS_TARGET_BOX, LENS_WEAK_POINT, LENS_WARNING, LENS_ANOMALY_REVEAL,
                 LENS_COMPLETION_PULSE, LENS_PROGRESS_ARC, LENS_NOISE_OVERLAY ->
                theme.uiAssets().texture(EchoThemeTextureKey.EDGE_GLOW);
            case VANILLA_CONTAINER_FRAME, VANILLA_INVENTORY_FRAME, VANILLA_TITLE_BACKPLATE,
                 VANILLA_PAUSE_PANEL, VANILLA_TOOLTIP_PANEL, VANILLA_WIDGET_OUTLINE ->
                theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_PANEL);
            case VANILLA_SELECTED_SLOT -> theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_HOTBAR);
            case VANILLA_TOAST_ACCENT -> theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_TOAST);
            case VANILLA_BOSS_BAR_ACCENT -> theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_BOSS_BAR);
            case LOADING_BACKGROUND -> theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_BACKGROUND);
            case LOADING_PANEL, MENU_MAIN_BACKPLATE, MENU_PAUSE_PANEL, MENU_OPTIONS_PANEL, MENU_MODS_PANEL,
                 HUD_CHAT_PANEL, HUD_NOTIFICATION_CHIP ->
                theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_PANEL);
            case LOADING_PROGRESS_BAR, HUD_BOSS_BAR -> theme.uiAssets().texture(EchoThemeTextureKey.PROGRESS_BAR);
            case LOADING_SPINNER, LOADING_LOGO_MARK, ITEM_ICON_BADGE ->
                theme.uiAssets().texture(EchoThemeTextureKey.ICON_PACK);
            case MENU_WORLD_ROW -> theme.uiAssets().texture(EchoThemeTextureKey.PANEL_ALT);
            case HUD_HOTBAR_FRAME, HUD_SELECTED_SLOT ->
                theme.vanillaUiProfile().texture(EchoThemeTextureKey.VANILLA_HOTBAR);
            case HUD_CROSSHAIR_ACCENT, ITEM_ICON_RARITY_RING ->
                theme.uiAssets().texture(EchoThemeTextureKey.EDGE_GLOW);
            case ITEM_ICON_FRAME -> theme.uiAssets().texture(EchoThemeTextureKey.STATUS_CHIP);
            case ITEM_ICON_LOCK_OVERLAY -> theme.uiAssets().texture(EchoThemeTextureKey.LOCKED_OVERLAY);
            case ITEM_ICON_MISSION_MARKER -> theme.uiAssets().texture(EchoThemeTextureKey.MISSION_CARD_SELECTED);
            case RENDERCORE_GLOW_OVERLAY -> theme.uiAssets().texture(EchoThemeTextureKey.ENERGY_OVERLAY);
            case RENDERCORE_DISTORTION_OVERLAY -> theme.uiAssets().texture(EchoThemeTextureKey.HOLOGRAM_OVERLAY);
            case RENDERCORE_ENTITY_HIGHLIGHT -> theme.uiAssets().texture(EchoThemeTextureKey.HOLOGRAM_OVERLAY);
            case RENDERCORE_MULTIBLOCK_ENERGY -> theme.uiAssets().texture(EchoThemeTextureKey.ENERGY_OVERLAY);
            case SCREENCORE_SURFACE_BASE -> theme.uiAssets().texture(EchoThemeTextureKey.PANEL);
            case SCREENCORE_SURFACE_RAISED, SCREENCORE_SURFACE_FLOATING ->
                theme.uiAssets().texture(EchoThemeTextureKey.PANEL_ALT);
            case SCREENCORE_BUTTON -> theme.uiAssets().texture(EchoThemeTextureKey.BUTTON);
            case SCREENCORE_BUTTON_HOVER -> theme.uiAssets().texture(EchoThemeTextureKey.BUTTON_HOVER);
            case SCREENCORE_STATUS_CHIP -> theme.uiAssets().texture(EchoThemeTextureKey.STATUS_CHIP);
            case SCREENCORE_PROGRESS_BAR -> theme.uiAssets().texture(EchoThemeTextureKey.PROGRESS_BAR);
            case SCREENCORE_FOCUS_RING, SCREENCORE_CORNER_CUTS, SCREENCORE_EDGE_RAILS,
                 SCREENCORE_PANEL_SHEEN, SCREENCORE_MICRO_TICKS ->
                theme.uiAssets().texture(EchoThemeTextureKey.EDGE_GLOW);
            default -> Optional.empty();
        };
    }

    public static Optional<Identifier> getClientItemIconReplacement(Identifier itemOrIconId) {
        return itemIconReplacement(getClientTheme(), itemOrIconId);
    }

    public static Optional<Identifier> getItemIconReplacement(Player player, Identifier itemOrIconId) {
        return itemIconReplacement(getTheme(player), itemOrIconId);
    }

    public static Optional<Identifier> itemIconReplacement(EchoTheme theme, Identifier itemOrIconId) {
        if (theme == null || itemOrIconId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(theme.metadata().get("item_icon.replacement." + itemOrIconId))
                .map(Identifier::tryParse);
    }

    public static Optional<Identifier> getSound(Player player, EchoThemeSoundKey key) {
        return getTheme(player).soundProfile().sound(key);
    }

    public static ThemeTransition getTransition(Identifier fromTheme, Identifier toTheme) {
        EchoTheme target = ThemeRegistry.get(toTheme);
        EchoThemeRenderProfile render = target.renderProfile();
        return new ThemeTransition(
            fromTheme,
            target.id(),
            render.transitionStyle(),
            ThemeRegistry.transitionTicks(),
            render.hologramColor(),
            render.hologramSecondary(),
            render.edgeGlowStrength(),
            render.particleIntensity()
        );
    }

    public static void playThemeTransition(Player player, Identifier newTheme) {
        ThemeRegistry.setPlayerTheme(player.getUUID(), newTheme);
    }
}
