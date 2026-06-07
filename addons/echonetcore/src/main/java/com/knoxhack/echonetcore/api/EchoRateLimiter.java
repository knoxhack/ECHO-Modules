package com.knoxhack.echonetcore.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class EchoRateLimiter {
    private static final int CLEANUP_THRESHOLD = 4096;
    private static final int CLEANUP_AGE_TICKS = 1200;
    private static final Map<Key, Long> LAST_ACCEPTED_TICK = new ConcurrentHashMap<>();

    private EchoRateLimiter() {
    }

    public static boolean tryAcquire(ServerPlayer player, Identifier packetId, EchoRateLimitPolicy policy) {
        if (player == null || packetId == null || policy == null || !policy.enabled()) {
            return true;
        }
        long now = player.level().getGameTime();
        Key key = new Key(player.getUUID(), packetId, policy.scope());
        Long previous = LAST_ACCEPTED_TICK.get(key);
        if (previous != null && now - previous < policy.ticks()) {
            return false;
        }
        LAST_ACCEPTED_TICK.put(key, now);
        cleanupIfNeeded(now);
        return true;
    }

    public static void clearForTests() {
        LAST_ACCEPTED_TICK.clear();
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            clearPlayer(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            LAST_ACCEPTED_TICK.keySet().removeIf(key -> playerId.equals(key.playerId()));
        }
    }

    public static void onServerStopping() {
        LAST_ACCEPTED_TICK.clear();
    }

    private static void cleanupIfNeeded(long now) {
        if (LAST_ACCEPTED_TICK.size() < CLEANUP_THRESHOLD) {
            return;
        }
        LAST_ACCEPTED_TICK.entrySet().removeIf(entry -> now - entry.getValue() > CLEANUP_AGE_TICKS);
    }

    private record Key(UUID playerId, Identifier packetId, String scope) {
    }
}
