package com.knoxhack.echoscreencore.client.render;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.api.theme.EchoThemeTokenSnapshot;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoThemeBridge {
    private static final Identifier FALLBACK_ID = EchoScreenCoreMod.id("default_screen_core");

    private EchoThemeTokenSnapshot cached = fallbackTokens();

    public EchoThemeTokenSnapshot tokens(EchoAccessibilitySettings accessibility) {
        EchoThemeTokenSnapshot base = resolveThemeCoreTokens();
        cached = applyAccessibility(base, accessibility == null ? EchoAccessibilitySettings.DEFAULT : accessibility);
        return cached;
    }

    public int color(String token, int fallback) {
        return cached.colors().getOrDefault(token, fallback);
    }

    public boolean hasColor(String token) {
        return cached.colors().containsKey(token);
    }

    public int spacing(String token, int fallback) {
        return cached.spacing().getOrDefault(token, fallback);
    }

    public boolean hasSpacing(String token) {
        return cached.spacing().containsKey(token);
    }

    public int font(String token, int fallback) {
        return cached.font().getOrDefault(token, fallback);
    }

    public boolean hasFont(String token) {
        return cached.font().containsKey(token);
    }

    public int radius(String token, int fallback) {
        return cached.radius().getOrDefault(token, fallback);
    }

    public boolean hasRadius(String token) {
        return cached.radius().containsKey(token);
    }

    public Optional<Identifier> texture(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return EchoCoreServices.themeService().resolveTexture(token);
        } catch (LinkageError | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static EchoThemeTokenSnapshot resolveThemeCoreTokens() {
        EchoThemeTokenSnapshot fallback = fallbackTokens();
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Object theme;
            try {
                theme = api.getMethod("getClientTheme").invoke(null);
            } catch (NoSuchMethodException ignored) {
                theme = api.getMethod("getTheme").invoke(null);
            }
            Object colors = theme.getClass().getMethod("colors").invoke(theme);
            Identifier id = (Identifier) theme.getClass().getMethod("id").invoke(theme);
            Map<String, Integer> resolved = new LinkedHashMap<>(fallback.colors());
            Map<String, Integer> spacing = new LinkedHashMap<>(fallback.spacing());
            Map<String, Integer> radius = new LinkedHashMap<>(fallback.radius());
            resolved.put("background", invokeColor(colors, "background", resolved.get("background")));
            resolved.put("terminalBackground", invokeColor(colors, "background", resolved.get("terminalBackground")));
            resolved.put("panel", invokeColor(colors, "panel", resolved.get("panel")));
            resolved.put("card", invokeColor(colors, "panelAlt", resolved.get("card")));
            resolved.put("cardHover", invokeColor(colors, "glass", resolved.get("cardHover")));
            resolved.put("cardSelected", invokeColor(colors, "selection", resolved.get("cardSelected")));
            resolved.put("borderMuted", invokeColor(colors, "borderSoft", resolved.get("borderMuted")));
            resolved.put("borderStrong", invokeColor(colors, "border", resolved.get("borderStrong")));
            resolved.put("accent", invokeColor(colors, "primary", resolved.get("accent")));
            resolved.put("accentMuted", invokeColor(colors, "borderSoft", resolved.get("accentMuted")));
            resolved.put("accentDim", invokeColor(colors, "glass", resolved.get("accentDim")));
            resolved.put("success", invokeColor(colors, "success", resolved.get("success")));
            resolved.put("warning", invokeColor(colors, "warning", resolved.get("warning")));
            resolved.put("danger", invokeColor(colors, "error", resolved.get("danger")));
            resolved.put("info", invokeColor(colors, "secondary", resolved.get("info")));
            resolved.put("textPrimary", invokeColor(colors, "text", resolved.get("textPrimary")));
            resolved.put("textSecondary", invokeColor(colors, "mutedText", resolved.get("textSecondary")));
            resolved.put("textMuted", invokeColor(colors, "mutedText", resolved.get("textMuted")));
            resolved.put("disabled", invokeColor(colors, "locked", resolved.get("disabled")));
            resolved.put("glow", invokeColor(colors, "glow", resolved.get("glow")));
            resolved.put("buttonBg", invokeColor(colors, "panelAlt", resolved.get("buttonBg")));
            try {
                Object profile = api.getMethod("getClientTokenProfile").invoke(null);
                mergeIntMap(spacing, profile, "spacing", "");
                mergeIntMap(resolved, profile, "statusColors", "status.");
                mergeIntMap(resolved, profile, "rarityColors", "rarity.");
                radius.put("sm", invokeInt(profile, "buttonRadius", radius.get("sm")));
                radius.put("md", invokeInt(profile, "cardRadius", radius.get("md")));
                radius.put("lg", invokeInt(profile, "panelRadius", radius.get("lg")));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // Older ThemeCore builds expose only colors; ScreenCore keeps its fallback density.
            }
            return new EchoThemeTokenSnapshot(id == null ? FALLBACK_ID : id, Map.copyOf(resolved), Map.copyOf(spacing), fallback.font(), Map.copyOf(radius));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return fallback;
        }
    }

    private static int invokeColor(Object target, String method, int fallback) {
        try {
            Method resolved = target.getClass().getMethod(method);
            Object value = resolved.invoke(target);
            return value instanceof Integer color ? color : fallback;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return fallback;
        }
    }

    private static void mergeIntMap(Map<String, Integer> target, Object source, String method, String prefix) {
        if (target == null || source == null) {
            return;
        }
        try {
            Object value = source.getClass().getMethod(method).invoke(source);
            if (value instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Integer integer) {
                        target.put((prefix == null ? "" : prefix) + key, integer);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Token profile maps are optional.
        }
    }

    private static int invokeInt(Object source, String method, int fallback) {
        if (source == null) {
            return fallback;
        }
        try {
            Object value = source.getClass().getMethod(method).invoke(source);
            return value instanceof Integer integer ? integer : fallback;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static EchoThemeTokenSnapshot applyAccessibility(EchoThemeTokenSnapshot base, EchoAccessibilitySettings accessibility) {
        Map<String, Integer> colors = new LinkedHashMap<>(base.colors());
        Map<String, Integer> spacing = new LinkedHashMap<>(base.spacing());
        Map<String, Integer> font = new LinkedHashMap<>(base.font());
        if (accessibility.highContrast()) {
            colors.put("textPrimary", 0xFFFFFFFF);
            colors.put("textSecondary", 0xFFE7F8FF);
            colors.put("borderMuted", colors.getOrDefault("borderStrong", 0xFF2BEAFF));
        }
        if (accessibility.largeText()) {
            font.replaceAll((key, value) -> Math.max(value + 1, Math.round(value * accessibility.fontScale())));
        }
        float spacingScale = accessibility.spacingScale();
        if (spacingScale != 1.0F) {
            spacing.replaceAll((key, value) -> Math.max(1, Math.round(value * spacingScale)));
        }
        return new EchoThemeTokenSnapshot(base.themeId(), Map.copyOf(colors), Map.copyOf(spacing), Map.copyOf(font), base.radius());
    }

    private static EchoThemeTokenSnapshot fallbackTokens() {
        return new EchoThemeTokenSnapshot(
            FALLBACK_ID,
            Map.ofEntries(
                Map.entry("background", 0xFF030711),
                Map.entry("terminalBackground", 0xEE050B14),
                Map.entry("panel", 0xCC08111F),
                Map.entry("card", 0xCC0D1A2E),
                Map.entry("cardHover", 0xCC12324A),
                Map.entry("cardSelected", 0xDD123E58),
                Map.entry("borderMuted", 0xFF1A6F8A),
                Map.entry("borderStrong", 0xFF2BEAFF),
                Map.entry("accent", 0xFF00E5FF),
                Map.entry("accentMuted", 0xFF1A6F8A),
                Map.entry("accentDim", 0xFF0B3C4A),
                Map.entry("success", 0xFF45FFB0),
                Map.entry("warning", 0xFFFFD166),
                Map.entry("danger", 0xFFFF4D6D),
                Map.entry("info", 0xFF5BC0EB),
                Map.entry("textPrimary", 0xFFEAFBFF),
                Map.entry("textSecondary", 0xFFB7D7E3),
                Map.entry("textMuted", 0xFF8AAFC2),
                Map.entry("disabled", 0xFF3B4652),
                Map.entry("shadow", 0x99000000),
                Map.entry("glow", 0xAA00E5FF),
                Map.entry("overlay", 0x6610243A),
                Map.entry("buttonBg", 0xCC10243A)
            ),
            Map.of("xs", 4, "sm", 8, "md", 12, "lg", 16, "xl", 24, "xxl", 32),
            Map.of("tiny", 8, "small", 9, "body", 10, "bodyLarge", 12, "title", 14, "pageTitle", 18),
            Map.of("none", 0, "sm", 2, "md", 4, "lg", 6)
        );
    }
}
