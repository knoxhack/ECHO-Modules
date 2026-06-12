package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class IndexThemeStyle {
    static final int FALLBACK_BG = 0xF2060D13;
    static final int FALLBACK_PANEL = 0xF00B151D;
    static final int FALLBACK_ROW = 0xAA102630;
    static final int FALLBACK_SLOT_BG = 0x33102630;
    static final int FALLBACK_SLOT_BG_HOVER = 0x66102630;
    static final int FALLBACK_SLOT_OUTLINE = 0x4438DFF4;
    static final int FALLBACK_SLOT_OUTLINE_HOVER = 0xCC66E8FF;
    static final int FALLBACK_ACCENT = 0xFF66E8FF;
    static final int FALLBACK_TEXT = 0xFFE9FBFF;
    static final int FALLBACK_MUTED = 0xFF8CA7B5;
    static final int FALLBACK_WARNING = 0xFFFFD166;
    static final int FALLBACK_ERROR = 0xFFFF6B6B;
    static final int FALLBACK_SUCCESS = 0xFFA8F7C5;
    static final int FALLBACK_SECTION_BG = 0x44102630;
    static final int FALLBACK_SECTION_BORDER = 0x6638DFF4;

    private IndexThemeStyle() {
    }

    record Palette(
        int background,
        int panel,
        int panelAlt,
        int row,
        int slotBg,
        int slotBgHover,
        int slotOutline,
        int slotOutlineHover,
        int accent,
        int accentSecondary,
        int text,
        int muted,
        int warning,
        int error,
        int success,
        int border,
        int borderSoft,
        int sectionBg,
        int sectionBorder
    ) {
    }

    static Palette palette() {
        int background = color("index.background", FALLBACK_BG);
        int panel = color("index.panel", FALLBACK_PANEL);
        int panelAlt = color("index.panel_alt", 0xF00D1A2E);
        int row = color("index.row", FALLBACK_ROW);
        int border = color("index.border", FALLBACK_ACCENT);
        int borderSoft = color("index.border_soft", FALLBACK_SECTION_BORDER);
        int accent = color("index.accent", FALLBACK_ACCENT);
        int accentSecondary = color("index.accent_secondary", 0xFFFF2BD6);
        return new Palette(
            alpha(background, 242),
            alpha(panel, 240),
            alpha(panelAlt, 238),
            alpha(row, 170),
            alpha(row, 51),
            alpha(row, 102),
            alpha(border, 68),
            alpha(accent, 204),
            accent,
            accentSecondary,
            color("index.text", FALLBACK_TEXT),
            color("index.muted", FALLBACK_MUTED),
            color("index.warning", FALLBACK_WARNING),
            color("index.error", FALLBACK_ERROR),
            color("index.success", FALLBACK_SUCCESS),
            border,
            borderSoft,
            alpha(row, 68),
            alpha(border, 102)
        );
    }

    static int color(String token, int fallback) {
        return EchoCyberGlassUi.color(token, fallback);
    }

    static Optional<Identifier> texture(String token) {
        return Optional.of(EchoCyberGlassUi.texture(token));
    }

    static void icon(GuiGraphicsExtractor graphics, int x, int y, int size) {
        texture("index.icon").ifPresent(texture -> EchoCyberGlassUi.blit(graphics, texture, x, y, size, size));
    }

    static void backdrop(GuiGraphicsExtractor graphics, int width, int height) {
        EchoCyberGlassUi.screenBackdrop(graphics, width, height, EchoCyberGlassUi.Surface.ECHO_APP);
        graphics.fill(0, 0, width, height, alpha(color("index.background", 0xDD02070A), 150));
    }

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x, y, x + width, y + height, alpha(fill, 202));
        if (width > 10 && height > 10) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + Math.min(y + height - 1, y + 18), alpha(border, 18));
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, width, height, border);
        cornerCuts(graphics, x, y, width, height, alpha(border, 130));
    }

    static void card(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected) {
        Palette palette = palette();
        graphics.fill(x, y, x + width, y + height,
                selected ? alpha(palette.panelAlt(), 166) : alpha(palette.panel(), 146));
        if (selected && width > 8 && height > 8) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, alpha(palette.accent(), 20));
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, width, height,
                selected ? alpha(palette.accent(), 210) : alpha(palette.borderSoft(), 84));
    }

    static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, boolean hover, boolean enabled, int accent) {
        if (graphics == null || font == null || width <= 0 || height <= 0) {
            return;
        }
        Palette palette = palette();
        hover = enabled && hover;
        int fill = enabled ? alpha(palette.panelAlt(), hover ? 206 : 158) : alpha(0xFF101417, 156);
        int border = enabled ? (hover ? alpha(accent, 225) : alpha(accent, 142)) : 0xFF273136;
        EchoCyberGlassUi.glassButtonSurface(graphics, x, y, width, height, fill, border, accent, hover, enabled);
        int text = enabled ? palette.text() : 0xFF66777D;
        String safeLabel = EchoCyberGlassUi.trimToWidth(font, label, width - 6);
        int labelWidth = font.width(safeLabel);
        graphics.text(font, Component.literal(safeLabel), x + Math.max(3, (width - labelWidth) / 2),
                y + Math.max(4, (height - 8) / 2), text, false);
    }

    static void chip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, boolean selected, boolean hover) {
        if (graphics == null || font == null || width <= 0 || height <= 0) {
            return;
        }
        Palette palette = palette();
        int accent = selected ? palette.accent() : palette.border();
        int fill = selected ? alpha(palette.panelAlt(), 192) : hover ? alpha(palette.row(), 146) : alpha(palette.panel(), 132);
        int border = selected ? alpha(palette.accent(), 210) : hover ? alpha(palette.border(), 126) : alpha(palette.border(), 72);
        EchoCyberGlassUi.glassButtonSurface(graphics, x, y, width, height, fill, border, accent, hover || selected, true);
        String safeLabel = EchoCyberGlassUi.trimToWidth(font, label, width - 6);
        graphics.centeredText(font, safeLabel, x + width / 2, y + Math.max(4, height / 2 - 3),
                selected ? palette.text() : palette.muted());
    }

    static void scrollbar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int thumbX, int thumbY, int thumbWidth, int thumbHeight) {
        Palette palette = palette();
        graphics.fill(x, y, x + width, y + height, alpha(palette.background(), 132));
        graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, alpha(palette.accent(), 166));
        EchoCyberGlassUi.calmFrame(graphics, thumbX, thumbY, thumbWidth, thumbHeight, alpha(palette.accent(), 150));
    }

    static void progressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int filled, int color) {
        Palette palette = palette();
        graphics.fill(x, y, x + width, y + height, alpha(palette.row(), 118));
        int clamped = Math.max(0, Math.min(width, filled));
        if (clamped > 0) {
            graphics.fill(x, y, x + clamped, y + height, color);
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, width, height, alpha(palette.border(), 92));
    }

    static int alpha(int color, int alpha) {
        return EchoCyberGlassUi.alpha(color, alpha);
    }

    private static void cornerCuts(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        int cut = Math.min(12, Math.max(4, Math.min(width, height) / 10));
        graphics.fill(x + 2, y + 2, x + cut, y + 3, color);
        graphics.fill(x + 2, y + 2, x + 3, y + cut, color);
        graphics.fill(x + width - cut, y + 2, x + width - 2, y + 3, color);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + cut, color);
        graphics.fill(x + 2, y + height - 3, x + cut, y + height - 2, color);
        graphics.fill(x + 2, y + height - cut, x + 3, y + height - 2, color);
        graphics.fill(x + width - cut, y + height - 3, x + width - 2, y + height - 2, color);
        graphics.fill(x + width - 3, y + height - cut, x + width - 2, y + height - 2, color);
    }
}
