package com.knoxhack.echoholomap.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record HoloMapQuery(
        ResourceKey<Level> dimension,
        double centerX,
        double centerY,
        double centerZ,
        int radius) {
    public HoloMapQuery {
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        radius = Math.max(0, radius);
    }

    public static HoloMapQuery from(Player player, int radius) {
        if (player == null) {
            return new HoloMapQuery(Level.OVERWORLD, 0.0D, 0.0D, 0.0D, radius);
        }
        return new HoloMapQuery(player.level().dimension(), player.getX(), player.getY(), player.getZ(), radius);
    }

    public boolean matchesDimension(ResourceKey<Level> other) {
        return dimension.equals(other == null ? Level.OVERWORLD : other);
    }

    public boolean intersectsPoint(ResourceKey<Level> pointDimension, double x, double z) {
        return matchesDimension(pointDimension) && distanceSquared(x, z) <= radiusSquared();
    }

    public boolean intersectsCircle(ResourceKey<Level> circleDimension, double x, double z, double circleRadius) {
        if (!matchesDimension(circleDimension)) {
            return false;
        }
        double combined = radius + Math.max(0.0D, circleRadius);
        return distanceSquared(x, z) <= combined * combined;
    }

    public boolean intersectsRect(ResourceKey<Level> rectDimension, double x, double z, double width, double depth) {
        double halfWidth = Math.max(0.0D, width) / 2.0D;
        double halfDepth = Math.max(0.0D, depth) / 2.0D;
        return intersectsBounds(rectDimension, x - halfWidth, z - halfDepth, x + halfWidth, z + halfDepth);
    }

    public boolean intersectsBounds(ResourceKey<Level> boundsDimension,
            double minX, double minZ, double maxX, double maxZ) {
        if (!matchesDimension(boundsDimension)) {
            return false;
        }
        double closestX = clamp(centerX, Math.min(minX, maxX), Math.max(minX, maxX));
        double closestZ = clamp(centerZ, Math.min(minZ, maxZ), Math.max(minZ, maxZ));
        return distanceSquared(closestX, closestZ) <= radiusSquared();
    }

    public double distanceSquared(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz;
    }

    private double radiusSquared() {
        return (double) radius * radius;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
