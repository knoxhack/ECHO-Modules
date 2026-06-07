package com.knoxhack.echoweathercore.api.weather;

public record WeatherEffectModifiers(
    double visibilityMultiplier,
    double scannerRangeMultiplier,
    double scannerReliabilityMultiplier,
    double holomapReliabilityMultiplier,
    double filterDrainMultiplier,
    double radiationExposureMultiplier,
    double toxicExposureMultiplier,
    double coldExposureMultiplier,
    double heatExposureMultiplier,
    double hydrationDrainMultiplier,
    double solarPowerMultiplier,
    double powerGridInstabilityMultiplier,
    double batteryEfficiencyMultiplier,
    double machineHeatMultiplier,
    double droneScoutReliability,
    double droneRecallRisk,
    double mobSightMultiplier,
    double mobAggressionMultiplier,
    double factionPatrolActivityMultiplier,
    double routeRiskModifier
) {
    public static final WeatherEffectModifiers DEFAULT = new WeatherEffectModifiers(
        1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0
    );

    public WeatherEffectModifiers merge(WeatherEffectModifiers other) {
        return new WeatherEffectModifiers(
            multiply(this.visibilityMultiplier, other.visibilityMultiplier),
            multiply(this.scannerRangeMultiplier, other.scannerRangeMultiplier),
            multiply(this.scannerReliabilityMultiplier, other.scannerReliabilityMultiplier),
            multiply(this.holomapReliabilityMultiplier, other.holomapReliabilityMultiplier),
            multiply(this.filterDrainMultiplier, other.filterDrainMultiplier),
            multiply(this.radiationExposureMultiplier, other.radiationExposureMultiplier),
            multiply(this.toxicExposureMultiplier, other.toxicExposureMultiplier),
            multiply(this.coldExposureMultiplier, other.coldExposureMultiplier),
            multiply(this.heatExposureMultiplier, other.heatExposureMultiplier),
            multiply(this.hydrationDrainMultiplier, other.hydrationDrainMultiplier),
            multiply(this.solarPowerMultiplier, other.solarPowerMultiplier),
            multiply(this.powerGridInstabilityMultiplier, other.powerGridInstabilityMultiplier),
            multiply(this.batteryEfficiencyMultiplier, other.batteryEfficiencyMultiplier),
            multiply(this.machineHeatMultiplier, other.machineHeatMultiplier),
            multiply(this.droneScoutReliability, other.droneScoutReliability),
            multiply(this.droneRecallRisk, other.droneRecallRisk),
            multiply(this.mobSightMultiplier, other.mobSightMultiplier),
            multiply(this.mobAggressionMultiplier, other.mobAggressionMultiplier),
            multiply(this.factionPatrolActivityMultiplier, other.factionPatrolActivityMultiplier),
            multiply(this.routeRiskModifier, other.routeRiskModifier)
        );
    }

    private static double multiply(double a, double b) {
        return a * b;
    }
}
