package com.knoxhack.echobasegrid.api;

import java.util.Locale;

public enum ClaimPermission {
    BUILD("Build"),
    INTERACT("Interact"),
    CONTAINERS("Containers"),
    MANAGE("Manage");

    private final String label;

    ClaimPermission(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ClaimPermission fromId(String id) {
        if (id == null || id.isBlank()) {
            return BUILD;
        }
        try {
            return ClaimPermission.valueOf(id.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BUILD;
        }
    }
}
