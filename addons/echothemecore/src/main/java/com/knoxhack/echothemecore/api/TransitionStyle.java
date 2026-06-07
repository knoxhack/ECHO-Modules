package com.knoxhack.echothemecore.api;

import java.util.Locale;

public enum TransitionStyle {
    INSTANT,
    FADE,
    GLASS_FADE,
    GLITCH_SWAP,
    HOLOGRAM_BOOT,
    NEXUS_PHASE,
    GLITCH_CUT;

    public static TransitionStyle byName(String value, TransitionStyle fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
