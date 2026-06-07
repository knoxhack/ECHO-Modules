package com.knoxhack.echobasegrid.api;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum ClaimRole {
    MEMBER("Member", EnumSet.of(ClaimPermission.INTERACT, ClaimPermission.CONTAINERS)),
    MANAGER("Manager", EnumSet.allOf(ClaimPermission.class));

    private final String label;
    private final Set<ClaimPermission> defaultPermissions;

    ClaimRole(String label, Set<ClaimPermission> defaultPermissions) {
        this.label = label;
        this.defaultPermissions = Set.copyOf(defaultPermissions);
    }

    public String label() {
        return label;
    }

    public Set<ClaimPermission> defaultPermissions() {
        return defaultPermissions;
    }

    public static ClaimRole fromId(String id) {
        if (id == null || id.isBlank()) {
            return MEMBER;
        }
        try {
            return ClaimRole.valueOf(id.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return MEMBER;
        }
    }
}
