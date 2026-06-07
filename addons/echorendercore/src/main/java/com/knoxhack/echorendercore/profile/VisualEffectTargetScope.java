package com.knoxhack.echorendercore.profile;

import java.util.Locale;

/**
 * Runtime target filter for advanced visual effects.
 */
public enum VisualEffectTargetScope {
    PROFILE("profile", true),
    ENTITY("entity", true),
    BLOCK("block", true),
    GLOBAL("global", true),
    UNSUPPORTED("unsupported", false);

    private final String id;
    private final boolean supported;

    VisualEffectTargetScope(String id, boolean supported) {
        this.id = id;
        this.supported = supported;
    }

    public String id() {
        return this.id;
    }

    public boolean supported() {
        return this.supported;
    }

    public static VisualEffectTargetScope byName(String name) {
        if (name == null || name.isBlank()) {
            return PROFILE;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        for (VisualEffectTargetScope scope : values()) {
            if (scope.id.equals(normalized)) {
                return scope;
            }
        }
        return UNSUPPORTED;
    }
}
