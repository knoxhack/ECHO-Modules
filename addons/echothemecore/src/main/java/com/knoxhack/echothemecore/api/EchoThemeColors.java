package com.knoxhack.echothemecore.api;

public record EchoThemeColors(
    int primary,
    int secondary,
    int accent,
    int background,
    int panel,
    int panelAlt,
    int glass,
    int border,
    int borderSoft,
    int text,
    int mutedText,
    int success,
    int warning,
    int error,
    int locked,
    int glow,
    int selection
) {
    public int color(EchoThemeColorKey key) {
        return switch (key) {
            case PRIMARY -> primary;
            case SECONDARY -> secondary;
            case ACCENT -> accent;
            case BACKGROUND -> background;
            case PANEL -> panel;
            case PANEL_ALT -> panelAlt;
            case GLASS -> glass;
            case BORDER -> border;
            case BORDER_SOFT -> borderSoft;
            case TEXT -> text;
            case MUTED_TEXT -> mutedText;
            case SUCCESS -> success;
            case WARNING -> warning;
            case ERROR -> error;
            case LOCKED -> locked;
            case GLOW -> glow;
            case SELECTION -> selection;
        };
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 255) << 24) | ((red & 255) << 16) | ((green & 255) << 8) | (blue & 255);
    }

    public static int rgb(int red, int green, int blue) {
        return argb(255, red, green, blue);
    }

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 255) << 24) | (argb & 0x00FFFFFF);
    }

    public static int alpha(int argb) {
        return (argb >>> 24) & 255;
    }

    public static int red(int argb) {
        return (argb >>> 16) & 255;
    }

    public static int green(int argb) {
        return (argb >>> 8) & 255;
    }

    public static int blue(int argb) {
        return argb & 255;
    }

    public static int parseHex(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            if (value.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(value, 16);
            }
            if (value.length() == 8) {
                int rgba = (int) Long.parseLong(value, 16);
                int red = (rgba >>> 24) & 255;
                int green = (rgba >>> 16) & 255;
                int blue = (rgba >>> 8) & 255;
                int alpha = rgba & 255;
                return argb(alpha, red, green, blue);
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }

    public static String toHex(int argb) {
        return String.format("#%02X%02X%02X%02X", red(argb), green(argb), blue(argb), alpha(argb));
    }
}
