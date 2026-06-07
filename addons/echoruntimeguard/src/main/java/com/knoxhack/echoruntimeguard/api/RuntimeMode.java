package com.knoxhack.echoruntimeguard.api;

import java.util.Locale;

public enum RuntimeMode {
    POTATO("potato", "Potato"),
    BALANCED("balanced", "Balanced"),
    CINEMATIC("cinematic", "Cinematic"),
    SERVER("server", "Server"),
    DEBUG("debug", "Debug"),
    EMERGENCY("emergency", "Emergency");

    private final String id;
    private final String displayName;

    RuntimeMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static RuntimeMode byId(String value, RuntimeMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        for (RuntimeMode mode : values()) {
            if (mode.id.equals(normalized) || mode.name().equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return fallback;
    }
}
