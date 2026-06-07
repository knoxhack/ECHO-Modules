package com.knoxhack.echoashfallprotocol.gameplay;

import com.knoxhack.echoashfallprotocol.Config;
import net.minecraft.world.level.Level;

/**
 * Global difficulty profile that scales survival and machine gameplay.
 * Config-only for this release.
 */
public enum DifficultyProfile {
    CASUAL(0.5F, 0.5F, 0.5F, 0.5F, "Casual - Relaxed survival, reduced radiation, faster machines"),
    NORMAL(1.0F, 1.0F, 1.0F, 1.0F, "Normal - Balanced gameplay as designed"),
    HARD(1.5F, 1.25F, 1.5F, 1.25F, "Hard - Increased survival pressure, slower machines, more radiation"),
    NIGHTMARE(2.0F, 2.0F, 2.5F, 1.5F, "Nightmare - Extreme survival, severe radiation, machine failures");

    // Multipliers applied to various systems
    private final float radiationMultiplier;    // Incoming radiation damage
    private final float hydrationMultiplier;    // Hydration drain rate
    private final float survivalHazardMult;     // Cold/heat/toxic damage
    private final float machineSpeedMult;       // Machine processing speed (inverse - higher = slower)
    private final String description;

    DifficultyProfile(float radiation, float hydration, float hazard, float machineSpeed, String desc) {
        this.radiationMultiplier = radiation;
        this.hydrationMultiplier = hydration;
        this.survivalHazardMult = hazard;
        this.machineSpeedMult = machineSpeed;
        this.description = desc;
    }

    public float getRadiationMultiplier() { return radiationMultiplier; }
    public float getHydrationMultiplier() { return hydrationMultiplier; }
    public float getSurvivalHazardMultiplier() { return survivalHazardMult; }
    public float getMachineSpeedMultiplier() { return machineSpeedMult; }
    public String getDescription() { return description; }

    public float scaleRadiation(float baseAmount) {
        return baseAmount * radiationMultiplier;
    }

    public float scaleHydrationDecay(float baseAmount) {
        return baseAmount * hydrationMultiplier;
    }

    public float scaleSurvivalHazard(float baseAmount) {
        return baseAmount * survivalHazardMult;
    }

    public float scaleMachineSpeed(float baseSpeed) {
        return baseSpeed / machineSpeedMult;
    }

    /**
     * Gets the effective difficulty for a level from the pack config.
     */
    public static DifficultyProfile getEffective(Level level) {
        return Config.DEFAULT_DIFFICULTY.get();
    }

    /**
     * Applies difficulty scaling to base radiation amount.
     */
    public static float scaleRadiation(Level level, float baseAmount) {
        return getEffective(level).scaleRadiation(baseAmount);
    }

    /**
     * Applies difficulty scaling to hydration decay.
     */
    public static float scaleHydrationDecay(Level level, float baseAmount) {
        return getEffective(level).scaleHydrationDecay(baseAmount);
    }

    /**
     * Applies difficulty scaling to survival hazard damage or pressure.
     */
    public static float scaleSurvivalHazard(Level level, float baseAmount) {
        return getEffective(level).scaleSurvivalHazard(baseAmount);
    }

    /**
     * Applies difficulty scaling to machine speed (returns slower speed for higher difficulty).
     */
    public static float scaleMachineSpeed(Level level, float baseSpeed) {
        return getEffective(level).scaleMachineSpeed(baseSpeed);
    }

    /**
     * Returns true if this difficulty enables hardcore features (perma-death risk).
     */
    public boolean isHardcore() {
        return this == NIGHTMARE;
    }
}
