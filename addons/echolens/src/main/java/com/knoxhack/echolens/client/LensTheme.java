package com.knoxhack.echolens.client;

import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.config.LensConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public record LensTheme(
        int panel,
        int header,
        int border,
        int glow,
        int text,
        int muted,
        int good,
        int warning,
        int danger,
        int echo) {
    public static LensTheme current() {
        LensTheme themeCore = themeCore();
        if (themeCore != null) {
            return themeCore;
        }
        return switch (LensConfig.value(LensConfig.THEME, LensConfig.LensThemeId.ECHO_DARK)) {
            case CLEAN_MINIMAL -> new LensTheme(0xE8F5F7F8, 0xEEFDFDFD, 0x88374550, 0x55374550,
                    0xFF1B2429, 0xFF59646A, 0xFF1E7F4F, 0xFF9D6A00, 0xFF9E3030, 0xFF256E8D);
            case VANILLA_COMPACT -> new LensTheme(0xD8202020, 0xE02A2A2A, 0xAA6F6F6F, 0x6655FFFF,
                    0xFFFFFFFF, 0xFFB0B0B0, 0xFF80FF80, 0xFFFFFF80, 0xFFFF8080, 0xFF80E8FF);
            case ASHFALL_HAZARD -> new LensTheme(0xEC130B0A, 0xEE24100D, 0xAAAD4238, 0x99FF6B35,
                    0xFFFFEFE6, 0xFFC9A79F, 0xFFA8E06D, 0xFFFFB04C, 0xFFFF5B48, 0xFFFF7442);
            case ECHO_DARK -> new LensTheme(0xE8071017, 0xF00B1720, 0x7738DFF4, 0x884CCBFF,
                    0xFFEAF8FF, 0xFF8FA7B0, 0xFFA6E22E, 0xFFFFD166, 0xFFFF5A6E, 0xFF66D9EF);
        };
    }

    private static LensTheme themeCore() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Object colors = api.getMethod("getColors", Player.class).invoke(null, player);
            Object render = api.getMethod("getRenderProfile", Player.class).invoke(null, player);
            int panel = withAlpha(color(colors, "panel", 0xE8071017), opacity(render, "glassOpacity", 0.90F));
            int header = withAlpha(color(colors, "panelAlt", 0xF00B1720), 0.94F);
            return new LensTheme(
                    panel,
                    header,
                    withAlpha(color(colors, "border", 0x7738DFF4), 0.66F),
                    withAlpha(color(colors, "glow", 0x884CCBFF), 0.74F),
                    color(colors, "text", 0xFFEAF8FF),
                    color(colors, "mutedText", 0xFF8FA7B0),
                    color(colors, "success", 0xFFA6E22E),
                    color(colors, "warning", 0xFFFFD166),
                    color(colors, "error", 0xFFFF5A6E),
                    color(colors, "primary", 0xFF66D9EF));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static int color(Object colors, String method, int fallback) throws ReflectiveOperationException {
        return ((Integer) colors.getClass().getMethod(method).invoke(colors)).intValue();
    }

    private static float opacity(Object render, String method, float fallback) {
        try {
            return ((Float) render.getClass().getMethod(method).invoke(render)).floatValue();
        } catch (ReflectiveOperationException exception) {
            return fallback;
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public int tone(LensTone tone) {
        return switch (tone == null ? LensTone.NEUTRAL : tone) {
            case GOOD -> good;
            case WARNING -> warning;
            case DANGER -> danger;
            case MUTED -> muted;
            case ECHO, INFO -> echo;
            case NEUTRAL -> text;
        };
    }

    public int alpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
