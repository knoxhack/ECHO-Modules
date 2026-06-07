package com.knoxhack.echoholomap.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapRoutePoint(
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        int order,
        String label,
        HoloMapPrecision precision) {
    public HoloMapRoutePoint {
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        label = label == null ? "" : label.strip();
        precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
    }
}
