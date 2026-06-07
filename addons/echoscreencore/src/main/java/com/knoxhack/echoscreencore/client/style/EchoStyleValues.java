package com.knoxhack.echoscreencore.client.style;

import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.render.EchoThemeBridge;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoStyleValues {
    private EchoStyleValues() {
    }

    public static int length(EchoStyle style, String property, int parentSize, int fallback,
            EchoThemeBridge theme, EchoScreenDiagnostics diagnostics) {
        return style == null ? fallback : length(style.value(property, ""), parentSize, fallback, theme, diagnostics);
    }

    public static int length(String raw, int parentSize, int fallback, EchoThemeBridge theme, EchoScreenDiagnostics diagnostics) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("auto")) {
            return fallback;
        }
        String value = unwrapScale(raw.strip());
        try {
            if (value.endsWith("px")) {
                return Math.round(Float.parseFloat(value.substring(0, value.length() - 2).strip()));
            }
            if (value.endsWith("%")) {
                return Math.round(parentSize * Float.parseFloat(value.substring(0, value.length() - 1).strip()) / 100.0F);
            }
            if (value.startsWith("space(") && value.endsWith(")")) {
                String token = inner(value);
                if (!theme.hasSpacing(token) && diagnostics != null) {
                    diagnostics.warnOnce("invalid_theme_token", "space(" + token + ")");
                }
                return theme.spacing(token, fallback);
            }
            if (value.startsWith("font(") && value.endsWith(")")) {
                String token = inner(value);
                if (!theme.hasFont(token) && diagnostics != null) {
                    diagnostics.warnOnce("invalid_theme_token", "font(" + token + ")");
                }
                return theme.font(token, fallback);
            }
            if (value.startsWith("radius(") && value.endsWith(")")) {
                String token = inner(value);
                if (!theme.hasRadius(token) && diagnostics != null) {
                    diagnostics.warnOnce("invalid_theme_token", "radius(" + token + ")");
                }
                return theme.radius(token, fallback);
            }
            return Math.round(Float.parseFloat(value));
        } catch (NumberFormatException exception) {
            if (diagnostics != null) {
                diagnostics.warnOnce("invalid_unit", raw);
            }
            return fallback;
        }
    }

    public static int color(EchoStyle style, String property, EchoThemeBridge theme, int fallback) {
        return color(style, property, theme, fallback, null);
    }

    public static int color(EchoStyle style, String property, EchoThemeBridge theme, int fallback, EchoScreenDiagnostics diagnostics) {
        return style == null ? fallback : color(style.value(property, ""), theme, fallback, diagnostics);
    }

    public static int color(String raw, EchoThemeBridge theme, int fallback) {
        return color(raw, theme, fallback, null);
    }

    public static int color(String raw, EchoThemeBridge theme, int fallback, EchoScreenDiagnostics diagnostics) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.strip();
        if (value.startsWith("var(") && value.endsWith(")")) {
            value = variable(value, "transparent");
        }
        if ("transparent".equalsIgnoreCase(value)) {
            return 0x00000000;
        }
        if (value.startsWith("theme(") && value.endsWith(")")) {
            String token = inner(value);
            if (!theme.hasColor(token) && diagnostics != null) {
                diagnostics.warnOnce("invalid_theme_token", "theme(" + token + ")");
            }
            return theme.color(token, fallback);
        }
        if (value.startsWith("#")) {
            return parseHex(value, fallback);
        }
        return fallback;
    }

    public static int intValue(String raw, int fallback) {
        try {
            return raw == null || raw.isBlank() ? fallback : Math.round(Float.parseFloat(raw.replace("px", "").strip()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static String texture(EchoStyle style, String property, String fallback,
            EchoThemeBridge theme, EchoScreenDiagnostics diagnostics) {
        return style == null ? fallback : texture(style.value(property, ""), fallback, theme, diagnostics);
    }

    public static String texture(String raw, String fallback, EchoThemeBridge theme, EchoScreenDiagnostics diagnostics) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.strip();
        if (value.startsWith("theme-texture(") && value.endsWith(")")) {
            String token = inner(value);
            Optional<Identifier> resolved = theme == null ? Optional.empty() : theme.texture(token);
            if (resolved.isPresent()) {
                return resolved.get().toString();
            }
            if (diagnostics != null) {
                diagnostics.warnOnce("invalid_theme_texture", "theme-texture(" + token + ")");
            }
            return fallback;
        }
        return value;
    }

    public static Insets insets(EchoStyle style, String property, EchoThemeBridge theme, EchoScreenDiagnostics diagnostics) {
        String raw = style == null ? "" : style.value(property, "0");
        String[] parts = raw.strip().split("\\s+");
        int top;
        int right;
        int bottom;
        int left;
        if (parts.length == 1) {
            top = right = bottom = left = length(parts[0], 0, 0, theme, diagnostics);
        } else if (parts.length == 2) {
            top = bottom = length(parts[0], 0, 0, theme, diagnostics);
            right = left = length(parts[1], 0, 0, theme, diagnostics);
        } else if (parts.length == 3) {
            top = length(parts[0], 0, 0, theme, diagnostics);
            right = left = length(parts[1], 0, 0, theme, diagnostics);
            bottom = length(parts[2], 0, 0, theme, diagnostics);
        } else {
            top = length(parts.length > 0 ? parts[0] : "0", 0, 0, theme, diagnostics);
            right = length(parts.length > 1 ? parts[1] : "0", 0, 0, theme, diagnostics);
            bottom = length(parts.length > 2 ? parts[2] : "0", 0, 0, theme, diagnostics);
            left = length(parts.length > 3 ? parts[3] : "0", 0, 0, theme, diagnostics);
        }
        return new Insets(top, right, bottom, left);
    }

    private static String unwrapScale(String raw) {
        if (!raw.startsWith("scale(") || !raw.endsWith(")")) {
            return raw;
        }
        String inner = raw.substring("scale(".length(), raw.length() - 1);
        int comma = inner.lastIndexOf(',');
        if (comma < 0) {
            return inner;
        }
        String value = inner.substring(0, comma).strip();
        String amount = inner.substring(comma + 1).strip();
        int base = length(value, 0, 0, new EchoThemeBridge(), null);
        try {
            return String.valueOf(Math.round(base * Float.parseFloat(amount)));
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    private static String inner(String token) {
        return token.substring(token.indexOf('(') + 1, token.length() - 1).strip();
    }

    private static String variable(String token, String fallback) {
        String name = inner(token);
        return switch (name) {
            case "--echo-bg" -> "theme(background)";
            case "--echo-panel" -> "theme(panel)";
            case "--echo-border" -> "theme(borderMuted)";
            case "--echo-accent" -> "theme(accent)";
            case "--echo-accent-soft" -> "theme(accentDim)";
            case "--echo-danger" -> "theme(danger)";
            case "--echo-warning" -> "theme(warning)";
            case "--echo-success" -> "theme(success)";
            case "--echo-text" -> "theme(textPrimary)";
            case "--echo-text-muted" -> "theme(textMuted)";
            default -> fallback;
        };
    }

    private static int parseHex(String raw, int fallback) {
        String value = raw.substring(1).strip().toLowerCase(Locale.ROOT);
        try {
            if (value.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(value, 16);
            }
            if (value.length() == 8) {
                long parsed = Long.parseLong(value, 16);
                int red = (int) ((parsed >>> 24) & 255);
                int green = (int) ((parsed >>> 16) & 255);
                int blue = (int) ((parsed >>> 8) & 255);
                int alpha = (int) (parsed & 255);
                return (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        } catch (NumberFormatException exception) {
            return fallback;
        }
        return fallback;
    }

    public record Insets(int top, int right, int bottom, int left) {
        public int horizontal() {
            return left + right;
        }

        public int vertical() {
            return top + bottom;
        }
    }
}
