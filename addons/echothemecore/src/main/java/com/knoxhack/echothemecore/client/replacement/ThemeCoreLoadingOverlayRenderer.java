package com.knoxhack.echothemecore.client.replacement;

import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.client.ClientThemeState;
import com.knoxhack.echothemecore.client.NativeLoaderTextIdentity;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

public final class ThemeCoreLoadingOverlayRenderer {
    private ThemeCoreLoadingOverlayRenderer() {
    }

    public static boolean enabled() {
        return ThemeCoreConfig.loadingReplacementEnabled();
    }

    public static void render(GuiGraphicsExtractor graphics, Minecraft minecraft, float partialTick, float progress, float alpha) {
        if (!enabled()) {
            return;
        }
        EchoTheme theme = ClientThemeState.currentTheme();
        EchoThemeColors colors = theme.colors();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int ticks = (int) (Util.getMillis() / 50L);
        graphics.fill(0, 0, width, height, fade(colors.background(), alpha));

        int margin = clamp(width / 24, 16, 48);
        int panelWidth = Math.max(240, Math.min(width - margin * 2, width < 640 ? width - margin * 2 : 680));
        int panelHeight = height < 260 ? 132 : 168;
        int left = Math.max(margin, (width - panelWidth) / 2);
        int top = clamp(height / 2 - panelHeight / 2, 24, Math.max(24, height - panelHeight - 24));
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, fade(colors.panel(), alpha));
        graphics.outline(left, top, panelWidth, panelHeight, fade(colors.border(), alpha));
        graphics.fill(left + 1, top + 1, right - 1, top + 26, fade(colors.glass(), alpha));
        graphics.fill(left + 14, top + 28, right - 14, top + 29,
                fade(pulse(ticks, colors.primary(), colors.selection(), 64), alpha));

        Font font = minecraft.font;
        boolean nativeLoader = NativeLoaderTextIdentity.active();
        String title = nativeLoader ? NativeLoaderTextIdentity.label().toUpperCase() + " // RESOURCE LOAD" : theme.displayName().toUpperCase() + " // RESOURCE LOAD";
        String percent = Math.min(100, Math.max(0, Math.round(progress * 100.0F))) + "%";
        graphics.text(font, clip(font, title, panelWidth - 112), left + 16, top + 9, fade(colors.primary(), alpha), false);
        graphics.text(font, percent, right - 16 - font.width(percent), top + 9, fade(colors.success(), alpha), false);
        graphics.text(font, clip(font, nativeLoader ? NativeLoaderTextIdentity.productLabel() : "LOCAL THEME ENGINE", panelWidth - 32), left + 16, top + 42, fade(colors.text(), alpha), false);
        graphics.text(font, statusLine(progress, nativeLoader), left + 16, top + 58, fade(statusColor(colors, progress), alpha), false);

        int barX = left + 16;
        int barY = bottom - 36;
        int barW = panelWidth - 32;
        graphics.outline(barX, barY, barW, 10, fade(colors.borderSoft(), alpha));
        int fillW = Math.max(1, Math.round((barW - 4) * Math.max(0.0F, Math.min(1.0F, progress))));
        graphics.fill(barX + 2, barY + 2, barX + 2 + fillW, barY + 8, fade(colors.glow(), alpha));

        if (panelHeight > 144) {
            String modules = "VANILLA UI / SCREENCORE / HUD / RENDERCORE / ITEM ICON CHROME";
            graphics.text(font, clip(font, modules, panelWidth - 32), left + 16, bottom - 58,
                    fade(colors.mutedText(), alpha), false);
        }
        if (width > 520 && height > 210) {
            String footer = nativeLoader ? "WINDOW: " + NativeLoaderTextIdentity.windowTitle() : "CLIENT-LOCAL THEME: " + theme.id();
            graphics.text(font, clip(font, footer, width - margin * 2), margin, height - 22,
                    fade(colors.borderSoft(), alpha), false);
        }
    }

    private static int fade(int color, float alpha) {
        int baseAlpha = (color >>> 24) & 0xFF;
        return ThemeCoreReplacementRenderer.alpha(color, Math.round(baseAlpha * Math.max(0.0F, Math.min(1.0F, alpha))));
    }

    private static int pulse(int ticks, int first, int second, int period) {
        float phase = (ticks % Math.max(1, period)) / (float) Math.max(1, period);
        float amount = phase < 0.5F ? phase * 2.0F : (1.0F - phase) * 2.0F;
        return ThemeCoreReplacementRenderer.blend(first, second, amount);
    }

    private static String statusLine(float progress, boolean nativeLoader) {
        String prefix = nativeLoader ? "ECHO NATIVE LOADER // " : "";
        if (progress < 0.20F) {
            return prefix + "MOUNTING RESOURCE PACKS";
        }
        if (progress < 0.48F) {
            return prefix + "BAKING THEMED TEXTURES";
        }
        if (progress < 0.75F) {
            return prefix + "RESOLVING SCREEN TOKENS";
        }
        if (progress < 0.98F) {
            return prefix + "VERIFYING FALLBACK RENDERERS";
        }
        return prefix + "TEXT IDENTITY READY";
    }

    private static int statusColor(EchoThemeColors colors, float progress) {
        if (progress < 0.20F) {
            return colors.warning();
        }
        if (progress < 0.98F) {
            return colors.primary();
        }
        return colors.success();
    }

    private static String clip(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int allowed = Math.max(0, maxWidth - font.width(ellipsis));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(builder.toString() + c) > allowed) {
                break;
            }
            builder.append(c);
        }
        return builder + ellipsis;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
