package com.knoxhack.echoplayercore.data;

import java.util.UUID;

public record TpaRequest(
        UUID requesterId,
        UUID targetId,
        long createdAt,
        boolean here
) {
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - createdAt > timeoutMs;
    }
}
