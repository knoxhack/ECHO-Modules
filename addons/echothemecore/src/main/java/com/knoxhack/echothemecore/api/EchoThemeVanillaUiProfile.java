package com.knoxhack.echothemecore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoThemeVanillaUiProfile(
    Identifier backgroundTexture,
    Identifier panelTexture,
    Identifier buttonTexture,
    Identifier hotbarTexture,
    Identifier tooltipTexture,
    Identifier toastTexture,
    Identifier bossBarTexture,
    int backgroundTint,
    int panelTint,
    int tooltipTint,
    int widgetAccent,
    int hotbarAccent,
    int chatAccent,
    float overlayOpacity,
    float panelOpacity,
    float edgeGlowStrength,
    boolean reduceVanillaBrown
) {
    public Optional<Identifier> texture(EchoThemeTextureKey key) {
        return Optional.ofNullable(switch (key) {
            case VANILLA_BACKGROUND -> backgroundTexture;
            case VANILLA_PANEL -> panelTexture;
            case VANILLA_BUTTON -> buttonTexture;
            case VANILLA_HOTBAR -> hotbarTexture;
            case VANILLA_TOOLTIP -> tooltipTexture;
            case VANILLA_TOAST -> toastTexture;
            case VANILLA_BOSS_BAR -> bossBarTexture;
            case VANILLA_TOOLTIP_PANEL -> tooltipTexture;
            case VANILLA_TOAST_ACCENT -> toastTexture;
            case VANILLA_BOSS_BAR_ACCENT -> bossBarTexture;
            default -> null;
        });
    }

    public static EchoThemeVanillaUiProfile fromThemeFallback(EchoTheme theme) {
        return fromParts(theme.colors(), theme.uiAssets(), theme.renderProfile());
    }

    public static EchoThemeVanillaUiProfile fromParts(EchoThemeColors colors, EchoThemeUiAssets ui, EchoThemeRenderProfile renderProfile) {
        return new EchoThemeVanillaUiProfile(
            ui.backgroundTexture(),
            ui.panelTexture(),
            ui.buttonTexture(),
            ui.statusChipTexture(),
            ui.panelAltTexture(),
            ui.statusChipTexture(),
            ui.progressBarTexture(),
            EchoThemeColors.withAlpha(colors.background(), 105),
            EchoThemeColors.withAlpha(colors.panel(), 160),
            EchoThemeColors.withAlpha(colors.glass(), 210),
            colors.border(),
            colors.selection(),
            colors.glow(),
            0.34F,
            0.58F,
            renderProfile.edgeGlowStrength(),
            true
        );
    }
}
