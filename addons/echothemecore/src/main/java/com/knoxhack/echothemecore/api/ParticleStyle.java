package com.knoxhack.echothemecore.api;

import java.util.Locale;

public enum ParticleStyle {
    SOFT_GLINTS,
    CYBER_SPARKS,
    NEXUS_FRAGMENTS,
    ASH_EMBERS,
    ORBITAL_DUST,
    BLACKBOX_GLYPHS,
    RECLAMATION_SPORES,
    NONE;

    public static ParticleStyle byName(String value, ParticleStyle fallback) {
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
