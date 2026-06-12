package com.knoxhack.echoopenlandsprotocol.runtime;

public record OpenlandsShelterSnapshot(
        double roofCoverage,
        double wallEnclosure,
        boolean entryClosed,
        boolean bedrollPresent,
        boolean lightOrFirePresent,
        int nearestHostileDistanceBlocks
) {
    public OpenlandsShelterSnapshot {
        roofCoverage = clamp01(roofCoverage);
        wallEnclosure = clamp01(wallEnclosure);
        nearestHostileDistanceBlocks = Math.max(0, nearestHostileDistanceBlocks);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
