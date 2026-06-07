package com.knoxhack.echoplayercore.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerTravelData {
    private final UUID playerId;
    private final Map<String, HomeLocation> homes = new LinkedHashMap<>();
    private TeleportLocation lastBackLocation;
    private TeleportLocation lastDeathLocation;
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();
    private TeleportLocation lastRtpLocation;

    public PlayerTravelData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    public synchronized Map<String, HomeLocation> homes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(homes));
    }

    public synchronized Optional<HomeLocation> home(String name) {
        return Optional.ofNullable(homes.get(name));
    }

    public synchronized boolean setHome(HomeLocation home) {
        if (home == null) {
            return false;
        }
        homes.put(home.name(), home);
        return true;
    }

    public synchronized boolean deleteHome(String name) {
        return homes.remove(name) != null;
    }

    public synchronized int homeCount() {
        return homes.size();
    }

    public synchronized Optional<TeleportLocation> lastBackLocation() {
        return Optional.ofNullable(lastBackLocation);
    }

    public synchronized void setLastBackLocation(TeleportLocation location) {
        this.lastBackLocation = location;
    }

    public synchronized Optional<TeleportLocation> lastDeathLocation() {
        return Optional.ofNullable(lastDeathLocation);
    }

    public synchronized void setLastDeathLocation(TeleportLocation location) {
        this.lastDeathLocation = location;
    }

    public synchronized Optional<TeleportLocation> lastRtpLocation() {
        return Optional.ofNullable(lastRtpLocation);
    }

    public synchronized void setLastRtpLocation(TeleportLocation location) {
        this.lastRtpLocation = location;
    }

    public synchronized Map<String, Long> cooldowns() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(cooldowns));
    }

    public synchronized long getCooldown(String key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public synchronized void setCooldown(String key, long timestamp) {
        cooldowns.put(key, timestamp);
    }

    public synchronized void clearCooldown(String key) {
        cooldowns.remove(key);
    }
}
