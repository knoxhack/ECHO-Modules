package com.knoxhack.echoweathercore.api.weather;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record ActiveWeatherEvent(
    UUID eventId,
    Identifier profileId,
    WeatherType type,
    WeatherSeverity severity,
    WeatherScope scope,
    WeatherPhase phase,
    long startTick,
    long endTick,
    long warningStartTick,
    BlockPos centerPos,
    int radius,
    Identifier regionId,
    String movementDirection,
    String sourceReason,
    List<Identifier> generatedResources,
    String debugMetadata
) {
    public ActiveWeatherEvent {
        if (generatedResources == null) generatedResources = List.of();
    }

    public boolean isActive(long currentTick) {
        return phase != WeatherPhase.ENDED && currentTick >= startTick && currentTick < endTick;
    }

    public boolean isWarningPhase(long currentTick) {
        return currentTick >= warningStartTick && currentTick < startTick;
    }

    public boolean affectsPosition(BlockPos pos) {
        if (scope == WeatherScope.GLOBAL) return true;
        if (centerPos == null) return false;
        return centerPos.distToCenterSqr(pos.getX(), pos.getY(), pos.getZ()) <= (double) radius * radius;
    }
}
