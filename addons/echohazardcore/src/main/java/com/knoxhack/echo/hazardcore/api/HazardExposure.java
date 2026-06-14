package com.knoxhack.echo.hazardcore.api;

/**
 * Immutable snapshot of how intensely a player is exposed to one hazard right now.
 */
public record HazardExposure(
    HazardType hazard,
    float intensity,
    float threshold,
    String sourceKey
) {
    public HazardExposure {
        intensity = Math.max(0.0f, intensity);
        threshold = Math.max(0.0f, threshold);
        if (sourceKey == null || sourceKey.isBlank()) {
            sourceKey = "unknown";
        }
    }

    public boolean isDangerous() {
        return intensity > threshold;
    }

    public float overflow() {
        return Math.max(0.0f, intensity - threshold);
    }

    public HazardExposure withIntensity(float intensity) {
        return new HazardExposure(hazard, intensity, threshold, sourceKey);
    }
}
