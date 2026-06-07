package com.knoxhack.echoholomap.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapZonePoint(
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        int order) {
    public HoloMapZonePoint {
        dimension = dimension == null ? Level.OVERWORLD : dimension;
    }
}
