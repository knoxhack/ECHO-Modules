package com.knoxhack.echothemecore.client.vanilla;

import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;

final class VanillaUiColors {
    private VanillaUiColors() {
    }

    static int readableText(EchoThemeColors colors, int background) {
        if (!ThemeCoreConfig.preserveTextContrast()) {
            return colors.text();
        }
        return contrast(colors.text(), background) >= 4.5D ? colors.text() : 0xFFFFFFFF;
    }

    static int alpha(int color, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(255.0F * opacity)));
        return EchoThemeColors.withAlpha(color, alpha);
    }

    static int cappedAlpha(int color, int alpha) {
        int cap = ThemeCoreConfig.vanillaSafeMode() ? Math.min(alpha, 145) : alpha;
        return EchoThemeColors.withAlpha(color, cap);
    }

    private static double contrast(int foreground, int background) {
        double l1 = luminance(foreground) + 0.05D;
        double l2 = luminance(background) + 0.05D;
        return Math.max(l1, l2) / Math.min(l1, l2);
    }

    private static double luminance(int argb) {
        return 0.2126D * channel(EchoThemeColors.red(argb))
            + 0.7152D * channel(EchoThemeColors.green(argb))
            + 0.0722D * channel(EchoThemeColors.blue(argb));
    }

    private static double channel(int value) {
        double normalized = value / 255.0D;
        return normalized <= 0.03928D ? normalized / 12.92D : Math.pow((normalized + 0.055D) / 1.055D, 2.4D);
    }
}
