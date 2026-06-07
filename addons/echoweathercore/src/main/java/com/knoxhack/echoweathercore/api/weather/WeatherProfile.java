package com.knoxhack.echoweathercore.api.weather;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record WeatherProfile(
    Identifier id,
    String displayName,
    WeatherType type,
    WeatherSeverity defaultSeverity,
    WeatherScope scope,
    int durationTicks,
    int warningTicks,
    int weight,
    int cooldownTicks,
    Set<Identifier> allowedDimensions,
    Set<Identifier> allowedBiomes,
    Set<Identifier> allowedRegionTags,
    Set<Identifier> disallowedBiomes,
    int minimumProgression,
    int earliestGameDay,
    WeatherEffectModifiers effects,
    List<Identifier> recommendedGear,
    List<Identifier> possibleResources,
    String terminalWarning,
    List<String> echoLines,
    String holomapOverlayMetadata,
    String lensScanText,
    String soundCoreAmbienceId,
    String particleVisualProfileId,
    boolean enabled
) {
    public WeatherProfile {
        if (allowedDimensions == null) allowedDimensions = Set.of();
        if (allowedBiomes == null) allowedBiomes = Set.of();
        if (allowedRegionTags == null) allowedRegionTags = Set.of();
        if (disallowedBiomes == null) disallowedBiomes = Set.of();
        if (recommendedGear == null) recommendedGear = List.of();
        if (possibleResources == null) possibleResources = List.of();
        if (echoLines == null) echoLines = List.of();
        if (effects == null) effects = WeatherEffectModifiers.DEFAULT;
    }
}
