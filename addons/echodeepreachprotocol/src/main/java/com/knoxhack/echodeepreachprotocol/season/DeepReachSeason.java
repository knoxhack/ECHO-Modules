package com.knoxhack.echodeepreachprotocol.season;

/**
 * Abyssal season in the Deep Reach cycle.
 */
public enum DeepReachSeason {
    STILL(
            "The Still",
            48000,
            "Calm water, predictable pressure. The safest window for shallow expeditions.",
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
    SURGE(
            "The Surge",
            36000,
            "Currents strengthen and pressure gradients steepen. Deeper zones become more dangerous.",
            1.25f, 1.0f, 1.15f, 1.0f, 1.2f),
    BLOOM(
            "The Bloom",
            36000,
            "Bioluminescent life flourishes. Oxygen demand rises but corruption seems muted.",
            1.0f, 1.2f, 1.0f, 0.8f, 1.0f),
    HUNGER(
            "The Hunger",
            36000,
            "Predators swarm. Creature activity peaks and thermal vents run hot.",
            1.0f, 1.0f, 1.2f, 1.0f, 1.5f),
    TREMOR(
            "The Tremor",
            24000,
            "Seismic instability spikes. Pressure and structural hazards reach their worst.",
            1.35f, 1.1f, 1.1f, 1.1f, 1.0f);

    private final String displayName;
    private final int durationTicks;
    private final String description;
    private final float pressureMultiplier;
    private final float oxygenMultiplier;
    private final float thermalMultiplier;
    private final float corruptionMultiplier;
    private final float spawnMultiplier;

    DeepReachSeason(String displayName, int durationTicks, String description,
                    float pressureMultiplier, float oxygenMultiplier, float thermalMultiplier,
                    float corruptionMultiplier, float spawnMultiplier) {
        this.displayName = displayName;
        this.durationTicks = durationTicks;
        this.description = description;
        this.pressureMultiplier = pressureMultiplier;
        this.oxygenMultiplier = oxygenMultiplier;
        this.thermalMultiplier = thermalMultiplier;
        this.corruptionMultiplier = corruptionMultiplier;
        this.spawnMultiplier = spawnMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public String description() {
        return description;
    }

    public float pressureMultiplier() {
        return pressureMultiplier;
    }

    public float oxygenMultiplier() {
        return oxygenMultiplier;
    }

    public float thermalMultiplier() {
        return thermalMultiplier;
    }

    public float corruptionMultiplier() {
        return corruptionMultiplier;
    }

    public float spawnMultiplier() {
        return spawnMultiplier;
    }
}
