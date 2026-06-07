package com.knoxhack.echotextureforge.common.review;

import java.util.Locale;

public enum TextureReviewStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    NEEDS_REGEN("needs_regen"),
    APPLIED("applied");

    private final String id;

    TextureReviewStatus(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TextureReviewStatus byId(String raw, TextureReviewStatus fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        for (TextureReviewStatus status : values()) {
            if (status.id.equals(normalized) || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return fallback;
    }
}
