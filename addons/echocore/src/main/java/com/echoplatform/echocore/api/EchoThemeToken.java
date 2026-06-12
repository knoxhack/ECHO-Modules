package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Map;

public final class EchoThemeToken {
    private static final Map<String, Integer> DEFAULT_DARK_COLORS = Map.ofEntries(
        Map.entry("background.primary", 0xFF030711),
        Map.entry("background.secondary", 0xFF08111F),
        Map.entry("panel.primary", 0xCC08111F),
        Map.entry("panel.secondary", 0xCC0D1A2E),
        Map.entry("panel.raised", 0xCC12324A),
        Map.entry("panel.warning", 0x44FFD166),
        Map.entry("panel.danger", 0x44FF4D6D),
        Map.entry("ui.panel", 0xCC08111F),
        Map.entry("ui.panel_alt", 0xCC0D1A2E),
        Map.entry("ui.glass", 0x6610243A),
        Map.entry("text.primary", 0xFFEAFBFF),
        Map.entry("text.muted", 0xFF8AAFC2),
        Map.entry("text.warning", 0xFFFFD166),
        Map.entry("text.success", 0xFF45FFB0),
        Map.entry("accent.primary", 0xFF00E5FF),
        Map.entry("accent.secondary", 0xFF5BC0EB),
        Map.entry("state.locked", 0xFF3B4652),
        Map.entry("state.ready", 0xFF45FFB0),
        Map.entry("state.active", 0xFF00E5FF),
        Map.entry("state.completed", 0xFF45FFB0),
        Map.entry("state.failed", 0xFFFF4D6D),
        Map.entry("state.success", 0xFF45FFB0),
        Map.entry("state.warning", 0xFFFFD166),
        Map.entry("state.error", 0xFFFF4D6D),
        Map.entry("border.primary", 0xFF2BEAFF),
        Map.entry("border.soft", 0xFF1A6F8A),
        Map.entry("border.selected", 0xFF00E5FF)
    );

    private EchoThemeToken() {
    }

    public static int resolveDefault(String token, int fallback) {
        if (token == null || token.isBlank()) {
            return fallback;
        }
        return DEFAULT_DARK_COLORS.getOrDefault(token, fallback);
    }

    public static List<String> defaultDarkColorTokens() {
        return List.copyOf(DEFAULT_DARK_COLORS.keySet());
    }
}
