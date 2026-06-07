package com.knoxhack.echotextureforge.api.spec;

import java.util.Locale;

public enum TextureSpecStatus {
    MISSING("missing"),
    NEEDS_FIX("needs_fix"),
    GENERATED_PENDING_REVIEW("generated_pending_review"),
    APPROVED("approved"),
    APPLIED("applied"),
    SKIPPED("skipped");

    private final String id;

    TextureSpecStatus(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TextureSpecStatus byId(String raw, TextureSpecStatus fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        for (TextureSpecStatus status : values()) {
            if (status.id.equals(normalized) || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return fallback;
    }
}
