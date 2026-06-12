package com.knoxhack.echocore.client.ui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class EchoCyberGlassUi {
    private static final Palette DEFAULT_PALETTE = new Palette(
            0xEE071014,
            0xFF66E8FF,
            0xFFFF45F6,
            0xFFE9FBFF,
            0xFF8CA7B5,
            0x6638DFF4);

    private EchoCyberGlassUi() {
    }

    public static Palette palette() {
        return DEFAULT_PALETTE;
    }

    public static int color(String token, int fallback) {
        return fallback;
    }

    public static Identifier texture(String token) {
        String safe = token == null || token.isBlank() ? "missing" : token.replace('.', '/');
        return Identifier.fromNamespaceAndPath("echocore", "textures/gui/" + safe + ".png");
    }

    public static int alpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        panel(graphics, x, y, w, h, DEFAULT_PALETTE.panel(), DEFAULT_PALETTE.accent());
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int border) {
        graphics.fill(x, y, x + w, y + h, fill);
        frame(graphics, x, y, w, h, border);
    }

    public static void calmPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int border) {
        panel(graphics, x, y, w, h, fill, border);
    }

    public static void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.outline(x, y, w, h, color);
    }

    public static void calmFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        frame(graphics, x, y, w, h, color);
    }

    public static void quietGrid(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        for (int gx = x; gx < x + w; gx += 12) {
            graphics.fill(gx, y, gx + 1, y + h, color);
        }
        for (int gy = y; gy < y + h; gy += 12) {
            graphics.fill(x, gy, x + w, gy + 1, color);
        }
    }

    public static void quietGrid(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int spacing, int color) {
        int step = Math.max(4, spacing);
        for (int gx = x; gx < x + w; gx += step) {
            graphics.fill(gx, y, gx + 1, y + h, color);
        }
        for (int gy = y; gy < y + h; gy += step) {
            graphics.fill(x, gy, x + w, gy + 1, color);
        }
    }

    public static void slot(GuiGraphicsExtractor graphics, int x, int y, int fill) {
        graphics.fill(x, y, x + 18, y + 18, fill);
        frame(graphics, x, y, 18, 18, 0x6638DFF4);
    }

    public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h,
            String label, double mouseX, double mouseY, boolean enabled) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        button(graphics, font, x, y, w, h, label, hovered, enabled, DEFAULT_PALETTE.accent());
    }

    public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h,
            String label, boolean hovered, boolean enabled, int accent) {
        int fill = enabled ? hovered ? alpha(accent, 76) : 0xAA071421 : 0x6611161B;
        int border = enabled ? hovered ? accent : alpha(accent, 150) : 0x66445158;
        graphics.fill(x, y, x + w, y + h, fill);
        frame(graphics, x, y, w, h, border);
        graphics.centeredText(font, trimToWidth(font, label, Math.max(0, w - 6)), x + w / 2,
                y + Math.max(1, (h - 8) / 2), enabled ? DEFAULT_PALETTE.text() : DEFAULT_PALETTE.muted());
    }

    public static void glassButtonSurface(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            int fill, int border, int accent, boolean hover, boolean enabled) {
        graphics.fill(x, y, x + w, y + h, enabled && hover ? alpha(accent, 64) : fill);
        frame(graphics, x, y, w, h, enabled ? border : alpha(border, 96));
    }

    public static void meter(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int filled, int color) {
        graphics.fill(x, y, x + w, y + h, 0xAA061016);
        graphics.fill(x, y, x + Math.max(0, Math.min(w, filled)), y + h, color);
        frame(graphics, x, y, w, h, alpha(color, 160));
    }

    public static void statusPill(GuiGraphicsExtractor graphics, Font font, int x, int y, int w,
            String label, int color, boolean active) {
        int fill = active ? alpha(color, 48) : 0x6611161B;
        int border = active ? alpha(color, 184) : alpha(color, 132);
        graphics.fill(x, y, x + w, y + 14, fill);
        frame(graphics, x, y, w, 14, border);
        graphics.centeredText(font, trimToWidth(font, label, Math.max(0, w - 6)), x + w / 2, y + 3, color);
    }

    public static void tooltipPanel(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
            int left, int top, int width, int height, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int panelW = Math.min(220, Math.max(90, lines.stream().mapToInt(font::width).max().orElse(90) + 12));
        int panelH = lines.size() * 11 + 8;
        int x = Math.min(left + width - panelW, mouseX + 10);
        int y = Math.min(top + height - panelH, mouseY + 10);
        panel(graphics, x, y, panelW, panelH, 0xEE071014, DEFAULT_PALETTE.accent());
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, trimToWidth(font, lines.get(i), panelW - 10), x + 5, y + 5 + i * 11,
                    DEFAULT_PALETTE.text(), false);
        }
    }

    public static void screenBackdrop(GuiGraphicsExtractor graphics, int width, int height, Surface surface) {
        graphics.fill(0, 0, width, height, surface == Surface.MENU ? 0xCC03080D : 0xDD050B10);
    }

    public static void screenChrome(GuiGraphicsExtractor graphics, int width, int height, boolean corners) {
        frame(graphics, 2, 2, Math.max(0, width - 4), Math.max(0, height - 4), DEFAULT_PALETTE.border());
    }

    public static void focusVeil(GuiGraphicsExtractor graphics, int width, int height, int x, int y, int w, int h,
            int alpha) {
        graphics.fill(0, 0, width, y, alpha(0x000000, alpha));
        graphics.fill(0, y + h, width, height, alpha(0x000000, alpha));
        graphics.fill(0, y, x, y + h, alpha(0x000000, alpha));
        graphics.fill(x + w, y, width, y + h, alpha(0x000000, alpha));
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0x33000000);
        frame(graphics, x, y, w, h, DEFAULT_PALETTE.border());
    }

    public static void blitContain(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h) {
        blit(graphics, texture, x, y, w, h);
    }

    public static String trimToWidth(Font font, String text, int width) {
        String value = text == null ? "" : text;
        if (font.width(value) <= width) {
            return value;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width(suffix))) + suffix;
    }

    public enum Surface {
        CONTAINER,
        ECHO_APP,
        MENU,
        OVERLAY_DRAWER
    }

    public record Palette(int panel, int accent, int accentSecondary, int text, int muted, int border) {
        public int success() {
            return 0xFF8AF6B6;
        }

        public int warning() {
            return 0xFFFFD166;
        }
    }
}
