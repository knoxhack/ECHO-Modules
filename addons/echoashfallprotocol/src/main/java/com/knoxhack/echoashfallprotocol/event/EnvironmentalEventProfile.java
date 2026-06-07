package com.knoxhack.echoashfallprotocol.event;

/**
 * Immutable tuning and presentation contract for one environmental event.
 */
public record EnvironmentalEventProfile(
        EnvironmentalEventType type,
        String commandAlias,
        String hudLabel,
        int durationTicks,
        WeatherMode weatherMode,
        int overlayColor,
        int particleColor,
        int particleBudget,
        float normalWeight,
        float restoredWeight,
        float destroyedWeight,
        float controlledWeight
) {
    public enum WeatherMode {
        NONE,
        DRY,
        RAIN,
        THUNDER,
        BLACKOUT
    }

    public boolean forcesVanillaWeather() {
        return weatherMode == WeatherMode.RAIN || weatherMode == WeatherMode.THUNDER;
    }

    public boolean shouldThunder() {
        return weatherMode == WeatherMode.THUNDER;
    }

    public float weightFor(com.knoxhack.echoashfallprotocol.world.NexusWorldData nexusData) {
        if (nexusData == null) {
            return normalWeight;
        }
        if (nexusData.isRestored()) {
            return restoredWeight;
        }
        if (nexusData.isDestroyed()) {
            return destroyedWeight;
        }
        if (nexusData.isControlled()) {
            return controlledWeight;
        }
        return normalWeight;
    }
}
