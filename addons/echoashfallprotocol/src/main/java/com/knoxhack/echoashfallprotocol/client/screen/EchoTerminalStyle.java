package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.Config;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared client-side ECHO terminal styling used by startup and menu screens.
 */
public final class EchoTerminalStyle {
    public static final int BG = 0xFF03070B;
    public static final int PANEL = 0xE80A1318;
    public static final int PANEL_SOFT = 0xBE0B1D24;
    public static final int LINE = 0x9A38DFF4;
    public static final int LINE_DIM = 0x4238DFF4;
    public static final int CYAN = 0xFF66E8FF;
    public static final int CYAN_DIM = 0xFF2E8E9D;
    public static final int GREEN = 0xFF76F7A2;
    public static final int AMBER = 0xFFFFD36A;
    public static final int RED = 0xFFFF6A6A;
    public static final int TEXT = 0xFFE6FBFF;
    public static final int MUTED = 0xFF8EA7AD;

    private EchoTerminalStyle() {
    }

    public static void renderTerminalBackground(GuiGraphicsExtractor graphics, int width, int height, int ticks, float partialTick) {
        renderTerminalBackground(graphics, width, height, ticks, partialTick, 1.0F);
    }

    public static void renderTerminalBackground(GuiGraphicsExtractor graphics, int width, int height, int ticks, float partialTick, float alpha) {
        float clampedAlpha = clamp(alpha, 0.0F, 1.0F);
        if (clampedAlpha <= 0.0F) {
            return;
        }

        graphics.fill(0, 0, width, height, fade(BG, clampedAlpha));
        renderTerminalOverlay(graphics, width, height, ticks, partialTick, clampedAlpha);
    }

    public static void renderTerminalOverlay(GuiGraphicsExtractor graphics, int width, int height, int ticks, float partialTick, float alpha) {
        float clampedAlpha = clamp(alpha, 0.0F, 1.0F);
        if (clampedAlpha <= 0.0F) {
            return;
        }

        double scanlineIntensity = scanlineIntensity();
        boolean animate = terminalAnimation() && scanlineAnimation();
        int drift = animate ? (int) ((ticks + partialTick) % 5) : 0;
        int gridColor = fade(alphaRgb((int) (0x13 * scanlineIntensity), 0x2CE7F7), clampedAlpha);
        for (int x = 0; x < width; x += 32) {
            graphics.fill(x, 0, x + 1, height, gridColor);
        }
        for (int y = 0; y < height; y += 24) {
            graphics.fill(0, y, width, y + 1, gridColor);
        }
        if (scanlineIntensity > 0.0D) {
            int scanlineColor = fade(alphaRgb((int) (0x1F * scanlineIntensity), 0x000000), clampedAlpha);
            for (int y = -5 + drift; y < height; y += 5) {
                graphics.fill(0, y, width, y + 1, scanlineColor);
            }
        }

        int framePulse = pulseColor(ticks, 0x6038DFF4, 0xA038DFF4, 36);
        graphics.outline(12, 12, Math.max(1, width - 24), Math.max(1, height - 24), fade(framePulse, clampedAlpha));
        graphics.fill(24, 24, 88, 25, fade(LINE, clampedAlpha));
        graphics.fill(width - 88, height - 25, width - 24, height - 24, fade(LINE, clampedAlpha));
    }

    public static void drawProgressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float progress, float alpha) {
        int fillWidth = Math.max(0, Math.min(width - 4, Math.round((width - 4) * clamp(progress, 0.0F, 1.0F))));
        graphics.fill(x, y, x + width, y + height, fade(0xB0081820, alpha));
        graphics.outline(x, y, width, height, fade(LINE, alpha));
        if (fillWidth > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, fade(CYAN, alpha));
            graphics.fill(x + 2, y + 2, x + 2 + Math.max(1, fillWidth / 3), y + height - 2, fade(0xFFB8FBFF, alpha));
        }
    }

    public static void pixelText(GuiGraphicsExtractor graphics, String value, int x, int y, int color, float alpha, int scale) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int drawColor = fade(color, alpha);
        if ((drawColor >>> 24) == 0) {
            return;
        }
        int px = x;
        int drawScale = Math.max(1, scale);
        for (int i = 0; i < value.length(); i++) {
            char ch = Character.toUpperCase(value.charAt(i));
            String[] glyph = pixelGlyph(ch);
            drawPixelGlyph(graphics, glyph, px, y, drawScale, drawColor);
            px += (glyph[0].length() + 1) * drawScale;
        }
    }

    public static void centeredPixelText(GuiGraphicsExtractor graphics, String value, int centerX, int y, int color, float alpha, int scale) {
        pixelText(graphics, value, centerX - pixelTextWidth(value, scale) / 2, y, color, alpha, scale);
    }

    public static String clipPixelText(String value, int width, int scale) {
        if (value == null || pixelTextWidth(value, scale) <= width) {
            return value == null ? "" : value;
        }
        String suffix = "...";
        int limit = Math.max(0, value.length() - 1);
        while (limit > 0 && pixelTextWidth(value.substring(0, limit) + suffix, scale) > width) {
            limit--;
        }
        return value.substring(0, limit) + suffix;
    }

    public static int pixelTextWidth(String value, int scale) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int drawScale = Math.max(1, scale);
        int width = 0;
        for (int i = 0; i < value.length(); i++) {
            width += (pixelGlyph(Character.toUpperCase(value.charAt(i)))[0].length() + 1) * drawScale;
        }
        return Math.max(0, width - drawScale);
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String value, int x, int y, int color, float alpha) {
        graphics.text(font, value, x, y, fade(color, alpha), false);
    }

    public static void centeredText(GuiGraphicsExtractor graphics, Font font, String value, int x, int y, int color, float alpha) {
        graphics.centeredText(font, value, x, y, fade(color, alpha));
    }

    public static String clipToWidth(Font font, String value, int width) {
        if (font.width(value) <= width) {
            return value;
        }
        String suffix = "...";
        int limit = Math.max(1, value.length() - 1);
        while (limit > 1 && font.width(value.substring(0, limit) + suffix) > width) {
            limit--;
        }
        return value.substring(0, limit) + suffix;
    }

    public static int pulseColor(int ticks, int low, int high, int period) {
        if (!terminalAnimation()) {
            return high;
        }
        return ((ticks / Math.max(1, period)) % 2) == 0 ? low : high;
    }

    public static int alphaRgb(int alpha, int rgb) {
        return (clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    public static int fade(int color, float alpha) {
        int baseAlpha = color >>> 24;
        int scaledAlpha = clamp(Math.round(baseAlpha * clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static void drawPixelGlyph(GuiGraphicsExtractor graphics, String[] glyph, int x, int y, int scale, int color) {
        for (int row = 0; row < glyph.length; row++) {
            String line = glyph[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) != ' ') {
                    int px = x + col * scale;
                    int py = y + row * scale;
                    graphics.fill(px, py, px + scale, py + scale, color);
                }
            }
        }
    }

    private static String[] pixelGlyph(char ch) {
        return switch (ch) {
            case 'A' -> new String[]{" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"};
            case 'B' -> new String[]{"#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### "};
            case 'C' -> new String[]{" ####", "#    ", "#    ", "#    ", "#    ", "#    ", " ####"};
            case 'D' -> new String[]{"#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "};
            case 'E' -> new String[]{"#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"};
            case 'F' -> new String[]{"#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    "};
            case 'G' -> new String[]{" ####", "#    ", "#    ", "# ###", "#   #", "#   #", " ####"};
            case 'H' -> new String[]{"#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"};
            case 'I' -> new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "#####"};
            case 'J' -> new String[]{"#####", "    #", "    #", "    #", "#   #", "#   #", " ### "};
            case 'K' -> new String[]{"#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"};
            case 'L' -> new String[]{"#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"};
            case 'M' -> new String[]{"#   #", "## ##", "# # #", "#   #", "#   #", "#   #", "#   #"};
            case 'N' -> new String[]{"#   #", "##  #", "# # #", "#  ##", "#   #", "#   #", "#   #"};
            case 'O' -> new String[]{" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "};
            case 'P' -> new String[]{"#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "};
            case 'Q' -> new String[]{" ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #"};
            case 'R' -> new String[]{"#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"};
            case 'S' -> new String[]{" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "};
            case 'T' -> new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "};
            case 'U' -> new String[]{"#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "};
            case 'V' -> new String[]{"#   #", "#   #", "#   #", "#   #", " # # ", " # # ", "  #  "};
            case 'W' -> new String[]{"#   #", "#   #", "#   #", "# # #", "# # #", "## ##", "#   #"};
            case 'X' -> new String[]{"#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #"};
            case 'Y' -> new String[]{"#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "};
            case 'Z' -> new String[]{"#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####"};
            case '0' -> new String[]{" ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### "};
            case '1' -> new String[]{"  #  ", " ##  ", "# #  ", "  #  ", "  #  ", "  #  ", "#####"};
            case '2' -> new String[]{" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"};
            case '3' -> new String[]{"#### ", "    #", "    #", " ### ", "    #", "    #", "#### "};
            case '4' -> new String[]{"#   #", "#   #", "#   #", "#####", "    #", "    #", "    #"};
            case '5' -> new String[]{"#####", "#    ", "#    ", "#### ", "    #", "    #", "#### "};
            case '6' -> new String[]{" ### ", "#    ", "#    ", "#### ", "#   #", "#   #", " ### "};
            case '7' -> new String[]{"#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   "};
            case '8' -> new String[]{" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "};
            case '9' -> new String[]{" ### ", "#   #", "#   #", " ####", "    #", "    #", " ### "};
            case '/' -> new String[]{"    #", "   # ", "   # ", "  #  ", " #   ", " #   ", "#    "};
            case ':' -> new String[]{" ", "#", " ", " ", " ", "#", " "};
            case '.' -> new String[]{" ", " ", " ", " ", " ", " ", "#"};
            case '-' -> new String[]{"     ", "     ", "     ", "#####", "     ", "     ", "     "};
            case '%' -> new String[]{"#   #", "    #", "   # ", "  #  ", " #   ", "#    ", "#   #"};
            case ' ' -> new String[]{"   ", "   ", "   ", "   ", "   ", "   ", "   "};
            default -> new String[]{" ### ", "#   #", "    #", "   # ", "  #  ", "     ", "  #  "};
        };
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean terminalAnimation() {
        try {
            return Config.TERMINAL_ANIMATION.get();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static boolean scanlineAnimation() {
        try {
            return Config.ECHO_SCANLINE_ANIMATION.get();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static double scanlineIntensity() {
        try {
            return Config.ECHO_SCANLINE_INTENSITY.get();
        } catch (RuntimeException ignored) {
            return 1.0D;
        }
    }
}
