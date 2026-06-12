package com.echoplatform.echocore.api;

import java.util.Locale;

public enum EchoFactionStanding {
    HOSTILE("Hostile", 0xFFE05252),
    NEUTRAL("Neutral", 0xFF9AA6B2),
    KNOWN("Known", 0xFF7DD3FC),
    TRUSTED("Trusted", 0xFF92F7A6),
    ALIGNED("Aligned", 0xFFFFD166);

    private final String displayName;
    private final int accentColor;

    EchoFactionStanding(String displayName, int accentColor) {
        this.displayName = displayName;
        this.accentColor = accentColor;
    }

    public String displayName() {
        return displayName;
    }

    public int accentColor() {
        return accentColor;
    }

    public static EchoFactionStanding fromReputation(int reputation) {
        if (reputation >= 60) {
            return ALIGNED;
        }
        if (reputation >= 30) {
            return TRUSTED;
        }
        if (reputation >= 5) {
            return KNOWN;
        }
        if (reputation <= -25) {
            return HOSTILE;
        }
        return NEUTRAL;
    }

    public static EchoFactionStanding fromName(String value) {
        if (value == null || value.isBlank()) {
            return NEUTRAL;
        }
        String normalized = value.strip().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        try {
            return EchoFactionStanding.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return NEUTRAL;
        }
    }
}
