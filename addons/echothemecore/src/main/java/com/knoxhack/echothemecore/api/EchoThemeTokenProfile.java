package com.knoxhack.echothemecore.api;

import java.util.Map;

public record EchoThemeTokenProfile(
    String family,
    String packTheme,
    EchoThemeDensity density,
    boolean standaloneFallback,
    int safeAreaMargin,
    int panelRadius,
    int cardRadius,
    int buttonRadius,
    int minButtonHeight,
    int minListRowHeight,
    float minTextContrast,
    Map<String, Integer> rarityColors,
    Map<String, Integer> statusColors,
    Map<String, Integer> spacing,
    Map<String, Float> animation
) {
    public EchoThemeTokenProfile {
        family = family == null || family.isBlank() ? "cyberglass" : family;
        packTheme = packTheme == null || packTheme.isBlank() ? family : packTheme;
        density = density == null ? EchoThemeDensity.STANDARD : density;
        rarityColors = rarityColors == null ? Map.of() : Map.copyOf(rarityColors);
        statusColors = statusColors == null ? Map.of() : Map.copyOf(statusColors);
        spacing = spacing == null ? Map.of() : Map.copyOf(spacing);
        animation = animation == null ? Map.of() : Map.copyOf(animation);
    }

    public static EchoThemeTokenProfile fromTheme(EchoTheme theme) {
        EchoThemeColors colors = colors(theme);
        Map<String, String> metadata = theme == null ? Map.of() : theme.metadata();
        EchoThemeDensity density = EchoThemeDensity.byName(metadata.get("tokens.density"), EchoThemeDensity.STANDARD);
        return new EchoThemeTokenProfile(
            metadata.getOrDefault("family", "cyberglass"),
            metadata.getOrDefault("tokens.pack_theme", metadata.getOrDefault("family", "cyberglass")),
            density,
            boolToken(metadata, "tokens.standalone_fallback", true),
            intToken(metadata, "tokens.safe_area_margin", 12),
            intToken(metadata, "tokens.panel_radius", 6),
            intToken(metadata, "tokens.card_radius", 6),
            intToken(metadata, "tokens.button_radius", 4),
            intToken(metadata, "tokens.min_button_height", density.buttonHeight()),
            intToken(metadata, "tokens.min_list_row_height", density.listRowHeight()),
            floatToken(metadata, "tokens.min_text_contrast", 4.5F),
            Map.ofEntries(
                Map.entry("common", colorToken(metadata, "tokens.rarity.common", colors.mutedText())),
                Map.entry("uncommon", colorToken(metadata, "tokens.rarity.uncommon", colors.success())),
                Map.entry("rare", colorToken(metadata, "tokens.rarity.rare", colors.primary())),
                Map.entry("epic", colorToken(metadata, "tokens.rarity.epic", colors.secondary())),
                Map.entry("legendary", colorToken(metadata, "tokens.rarity.legendary", colors.warning())),
                Map.entry("relic", colorToken(metadata, "tokens.rarity.relic", colors.accent()))
            ),
            Map.ofEntries(
                Map.entry("info", colorToken(metadata, "tokens.status.info", colors.primary())),
                Map.entry("active", colorToken(metadata, "tokens.status.active", colors.selection())),
                Map.entry("ready", colorToken(metadata, "tokens.status.ready", colors.success())),
                Map.entry("success", colorToken(metadata, "tokens.status.success", colors.success())),
                Map.entry("warning", colorToken(metadata, "tokens.status.warning", colors.warning())),
                Map.entry("danger", colorToken(metadata, "tokens.status.danger", colors.error())),
                Map.entry("locked", colorToken(metadata, "tokens.status.locked", colors.locked()))
            ),
            Map.ofEntries(
                Map.entry("xs", intToken(metadata, "tokens.spacing.xs", Math.max(2, density.panelGap() / 2))),
                Map.entry("sm", intToken(metadata, "tokens.spacing.sm", density.panelGap())),
                Map.entry("md", intToken(metadata, "tokens.spacing.md", density.panelGap() + 4)),
                Map.entry("lg", intToken(metadata, "tokens.spacing.lg", density.panelGap() + 10)),
                Map.entry("safe_area", intToken(metadata, "tokens.safe_area_margin", 12))
            ),
            Map.ofEntries(
                Map.entry("enter_ms", floatToken(metadata, "tokens.animation.enter_ms", 120.0F)),
                Map.entry("exit_ms", floatToken(metadata, "tokens.animation.exit_ms", 90.0F)),
                Map.entry("transition_ms", floatToken(metadata, "tokens.animation.transition_ms", 180.0F)),
                Map.entry("pulse_speed", floatToken(metadata, "tokens.animation.pulse_speed", 0.85F))
            )
        );
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

    private static int intToken(Map<String, String> metadata, String key, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(metadata.getOrDefault(key, Integer.toString(fallback))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float floatToken(Map<String, String> metadata, String key, float fallback) {
        try {
            return Math.max(0.0F, Float.parseFloat(metadata.getOrDefault(key, Float.toString(fallback))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int colorToken(Map<String, String> metadata, String key, int fallback) {
        return EchoThemeColors.parseHex(metadata.get(key), fallback);
    }

    private static boolean boolToken(Map<String, String> metadata, String key, boolean fallback) {
        String value = metadata.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
