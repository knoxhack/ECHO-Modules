package com.knoxhack.echobasegrid.api;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record ClaimMember(UUID playerId, String playerName, ClaimRole role, Set<ClaimPermission> permissions) {
    public ClaimMember {
        playerName = playerName == null || playerName.isBlank() ? "Unknown" : playerName.strip();
        role = role == null ? ClaimRole.MEMBER : role;
        EnumSet<ClaimPermission> safePermissions = EnumSet.noneOf(ClaimPermission.class);
        if (permissions != null) {
            safePermissions.addAll(permissions);
        }
        permissions = Set.copyOf(safePermissions);
    }

    public ClaimMember withRole(ClaimRole nextRole) {
        ClaimRole safeRole = nextRole == null ? ClaimRole.MEMBER : nextRole;
        return new ClaimMember(playerId, playerName, safeRole, safeRole.defaultPermissions());
    }

    public ClaimMember withPermissionToggled(ClaimPermission permission) {
        if (permission == null) {
            return this;
        }
        EnumSet<ClaimPermission> next = EnumSet.noneOf(ClaimPermission.class);
        next.addAll(permissions);
        if (!next.remove(permission)) {
            next.add(permission);
        }
        return new ClaimMember(playerId, playerName, role, next);
    }
}
