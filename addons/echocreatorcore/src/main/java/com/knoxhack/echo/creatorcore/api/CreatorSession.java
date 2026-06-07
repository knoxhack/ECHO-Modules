package com.knoxhack.echo.creatorcore.api;

import java.time.Instant;
import java.util.UUID;

public record CreatorSession(
        UUID playerId,
        String playerName,
        CreatorPermission permission,
        CreatorMode mode,
        Instant openedAt,
        Instant lastActiveAt) {
    public CreatorSession {
        playerName = playerName == null || playerName.isBlank() ? "unknown" : playerName;
        permission = permission == null ? CreatorPermission.BLOCKED : permission;
        mode = mode == null ? CreatorMode.DISABLED : mode;
        openedAt = openedAt == null ? Instant.now() : openedAt;
        lastActiveAt = lastActiveAt == null ? openedAt : lastActiveAt;
    }

    public CreatorSession touch() {
        return new CreatorSession(playerId, playerName, permission, mode, openedAt, Instant.now());
    }
}
