package com.knoxhack.echothemecore.api;

import java.util.Locale;

public enum HologramStyle {
    CYBER_GLASS,
    CLEAN_GRID,
    CYBER_NEON,
    NEXUS_DISTORTED,
    BLACKBOX_ARCHIVE,
    ORBITAL_TELEMETRY,
    RECLAMATION_SOFT,
    FLAT_GRID,
    NONE,
    CRT,
    RUNE_FIELD;

    public static HologramStyle byName(String value, HologramStyle fallback) {
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
