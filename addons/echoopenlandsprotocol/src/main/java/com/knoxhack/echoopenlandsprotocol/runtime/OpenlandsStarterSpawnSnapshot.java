package com.knoxhack.echoopenlandsprotocol.runtime;

public record OpenlandsStarterSpawnSnapshot(
        String biomeId,
        int starterResourceRadiusBlocks,
        int visibleLandmarkDistanceBlocks,
        int nearestHostileDistanceBlocks,
        boolean hasWoodSource,
        boolean hasLooseStone,
        boolean hasFiberSource,
        boolean hasStarterFood,
        boolean hasWaterOrWellHint,
        boolean hasExplorationHook
) {
    public OpenlandsStarterSpawnSnapshot {
        biomeId = biomeId == null ? "" : biomeId;
        starterResourceRadiusBlocks = Math.max(0, starterResourceRadiusBlocks);
        visibleLandmarkDistanceBlocks = Math.max(0, visibleLandmarkDistanceBlocks);
        nearestHostileDistanceBlocks = Math.max(0, nearestHostileDistanceBlocks);
    }
}
