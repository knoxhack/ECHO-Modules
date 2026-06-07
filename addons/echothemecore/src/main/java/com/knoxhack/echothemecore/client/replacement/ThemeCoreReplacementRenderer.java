package com.knoxhack.echothemecore.client.replacement;

import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.client.ClientThemeState;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

public final class ThemeCoreReplacementRenderer {
    private ThemeCoreReplacementRenderer() {
    }

    public static boolean renderButton(Button button, GuiGraphicsExtractor graphics) {
        if (!ThemeCoreConfig.buttonReplacementEnabled() || button == null || graphics == null) {
            return false;
        }
        EchoTheme theme = ClientThemeState.currentTheme();
        EchoThemeColors colors = theme.colors();
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }

        boolean selected = button.isHoveredOrFocused();
        boolean active = button.active;
        int fill = active
            ? blend(alpha(colors.panel(), selected ? 0xDC : 0xB8), colors.background(), 0.18F)
            : alpha(colors.locked(), 0x74);
        int border = active ? (selected ? colors.selection() : colors.border()) : colors.borderSoft();
        int text = active ? (selected ? colors.text() : colors.primary()) : colors.mutedText();
        int accent = active && selected ? colors.glow() : colors.borderSoft();

        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, alpha(border, active ? 0xE8 : 0x96));
        if (width > 14 && height > 8) {
            graphics.fill(x + 2, y + 2, x + 4, y + height - 2, alpha(colors.primary(), active ? 0xB8 : 0x54));
            graphics.fill(x + width - 4, y + 2, x + width - 2, y + height - 2, alpha(colors.accent(), selected ? 0xB8 : 0x54));
            graphics.fill(x + 5, y + height - 3, x + width - 5, y + height - 2, alpha(accent, selected ? 0xB8 : 0x48));
        }

        Font font = Minecraft.getInstance().font;
        String label = clip(font, button.getMessage().getString(), Math.max(1, width - 14));
        int labelX = x + Math.max(7, (width - font.width(label)) / 2);
        int labelY = y + Math.max(1, (height - 8) / 2);
        graphics.text(font, label, labelX, labelY, text, false);
        return true;
    }

    static int alpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    static int blend(int foreground, int background, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int fr = (foreground >> 16) & 0xFF;
        int fg = (foreground >> 8) & 0xFF;
        int fb = foreground & 0xFF;
        int br = (background >> 16) & 0xFF;
        int bg = (background >> 8) & 0xFF;
        int bb = background & 0xFF;
        int r = Math.round(fr * (1.0F - clamped) + br * clamped);
        int g = Math.round(fg * (1.0F - clamped) + bg * clamped);
        int b = Math.round(fb * (1.0F - clamped) + bb * clamped);
        return (foreground & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static String clip(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
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
}
