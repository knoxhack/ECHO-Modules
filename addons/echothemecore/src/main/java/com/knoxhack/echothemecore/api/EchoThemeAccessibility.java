package com.knoxhack.echothemecore.api;

public final class EchoThemeAccessibility {
    private EchoThemeAccessibility() {
    }

    public static EchoThemeContrastReport report(EchoTheme theme) {
        EchoThemeColors colors = colors(theme);
        int panel = composite(colors.panel(), colors.background());
        float text = contrastRatio(colors.text(), panel);
        float muted = contrastRatio(colors.mutedText(), panel);
        float accent = contrastRatio(colors.accent(), colors.background());
        float success = contrastRatio(colors.success(), panel);
        float warning = contrastRatio(colors.warning(), panel);
        float error = contrastRatio(colors.error(), panel);
        boolean readable = text >= 4.5F && muted >= 3.0F && accent >= 3.0F;
        String recommendation = readable
            ? "Theme passes the shared ECHO readability floor for dense cyberglass UI."
            : "Raise text, muted text, or accent contrast before using this theme on dense Terminal or Index surfaces.";
        return new EchoThemeContrastReport(text, muted, accent, success, warning, error, readable, recommendation);
    }

    public static boolean readableTextPair(int foreground, int background) {
        return contrastRatio(foreground, background) >= 4.5F;
    }

    public static float contrastRatio(int foreground, int background) {
        double lighter = Math.max(luminance(foreground), luminance(background));
        double darker = Math.min(luminance(foreground), luminance(background));
        return (float) ((lighter + 0.05D) / (darker + 0.05D));
    }

    private static EchoThemeColors colors(EchoTheme theme) {
        if (theme != null && theme.colors() != null) {
            return theme.colors();
        }
        return new EchoThemeColors(
            0xFF00E5FF, 0xFFB44CFF, 0xFFFF2BD6, 0xFF030711,
            0xCC08111F, 0xCC0D1A2E, 0x8810243A, 0xFF2BEAFF,
            0xFF1A6F8A, 0xFFEAFBFF, 0xFF8AAFC2, 0xFF45FFB0,
            0xFFFFD166, 0xFFFF4D6D, 0xFF3B4652, 0xFF00E5FF, 0xFFB44CFF
        );
    }

    private static int composite(int foreground, int background) {
        float alpha = EchoThemeColors.alpha(foreground) / 255.0F;
        int red = Math.round(EchoThemeColors.red(foreground) * alpha + EchoThemeColors.red(background) * (1.0F - alpha));
        int green = Math.round(EchoThemeColors.green(foreground) * alpha + EchoThemeColors.green(background) * (1.0F - alpha));
        int blue = Math.round(EchoThemeColors.blue(foreground) * alpha + EchoThemeColors.blue(background) * (1.0F - alpha));
        return EchoThemeColors.rgb(red, green, blue);
    }

    private static double luminance(int argb) {
        double red = linear(EchoThemeColors.red(argb) / 255.0D);
        double green = linear(EchoThemeColors.green(argb) / 255.0D);
        double blue = linear(EchoThemeColors.blue(argb) / 255.0D);
        return 0.2126D * red + 0.7152D * green + 0.0722D * blue;
    }

    private static double linear(double value) {
        return value <= 0.03928D ? value / 12.92D : Math.pow((value + 0.055D) / 1.055D, 2.4D);
    }
}
