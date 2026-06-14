package com.knoxhack.echodeepreachprotocol.season;

import com.knoxhack.echo.hazardcore.api.HazardType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the rotating abyssal season cycle for Deep Reach.
 *
 * <p>The season advances based on in-game time and modifies hazard intensities
 * through {@link #getMultiplier(HazardType)}. Packs and hazards call this method
 * to scale exposure without owning season timing logic.
 */
public final class DeepReachSeasonManager {
    public static final DeepReachSeasonManager INSTANCE = new DeepReachSeasonManager();

    private final AtomicInteger seasonIndex = new AtomicInteger(0);
    private final AtomicInteger ticksInSeason = new AtomicInteger(0);

    private DeepReachSeasonManager() {
    }

    public DeepReachSeason currentSeason() {
        return DeepReachSeason.values()[seasonIndex.get() % DeepReachSeason.values().length];
    }

    /**
     * Advance the season timer. Intended to be called once per server tick.
     */
    public void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        // Sync to overworld game time so season is global across dimensions.
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            return;
        }
        long dayTime = overworld.getOverworldClockTime();
        int cycleLength = totalCycleLength();
        int position = (int) (dayTime % cycleLength);
        int newIndex = 0;
        int accumulated = 0;
        for (DeepReachSeason season : DeepReachSeason.values()) {
            int next = accumulated + season.durationTicks();
            if (position >= accumulated && position < next) {
                seasonIndex.set(newIndex);
                ticksInSeason.set(position - accumulated);
                return;
            }
            accumulated = next;
            newIndex++;
        }
        // Fallback to Still if math drifts.
        seasonIndex.set(0);
        ticksInSeason.set(0);
    }

    public int ticksInSeason() {
        return ticksInSeason.get();
    }

    public int ticksRemainingInSeason() {
        DeepReachSeason season = currentSeason();
        return Math.max(0, season.durationTicks() - ticksInSeason.get());
    }

    public float getMultiplier(HazardType hazard) {
        DeepReachSeason season = currentSeason();
        if (hazard.equals(HazardType.PRESSURE)) {
            return season.pressureMultiplier();
        }
        if (hazard.equals(HazardType.OXYGEN_DEPRIVATION)) {
            return season.oxygenMultiplier();
        }
        if (hazard.equals(HazardType.COLD) || hazard.equals(HazardType.HEAT)) {
            return season.thermalMultiplier();
        }
        if (hazard.equals(HazardType.CORRUPTION)) {
            return season.corruptionMultiplier();
        }
        return 1.0f;
    }

    public float spawnMultiplier() {
        return currentSeason().spawnMultiplier();
    }

    private int totalCycleLength() {
        int total = 0;
        for (DeepReachSeason season : DeepReachSeason.values()) {
            total += season.durationTicks();
        }
        return total;
    }
}
