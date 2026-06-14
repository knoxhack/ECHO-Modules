package com.knoxhack.echo.hazardcore.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player hazard state. Currently in-memory; persistence will be added once
 * the data attachment contract is finalized platform-wide.
 */
public final class HazardPlayerData {
    private static final Map<UUID, HazardPlayerData> PLAYERS = new ConcurrentHashMap<>();

    private double lastDepth = Double.MAX_VALUE;
    private float corruption = 0.0f;
    private float decompressionSeverity = 0.0f;

    public static HazardPlayerData get(ServerPlayer player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), uuid -> new HazardPlayerData());
    }

    public double getLastDepth() {
        return lastDepth;
    }

    public void updateLastDepth(double depth) {
        this.lastDepth = depth;
    }

    public float getCorruption() {
        return corruption;
    }

    public void addCorruption(float amount) {
        this.corruption = Math.min(100.0f, Math.max(0.0f, this.corruption + amount));
    }

    public void removeCorruption(float amount) {
        this.corruption = Math.max(0.0f, this.corruption - amount);
    }

    public float getDecompressionSeverity() {
        return decompressionSeverity;
    }

    public void addDecompressionSeverity(float amount) {
        this.decompressionSeverity = Math.min(100.0f, Math.max(0.0f, this.decompressionSeverity + amount));
    }

    public void reduceDecompressionSeverity(float amount) {
        this.decompressionSeverity = Math.max(0.0f, this.decompressionSeverity - amount);
    }
}
