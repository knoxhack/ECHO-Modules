package com.knoxhack.echoopenlandsprotocol.runtime;

public record OpenlandsCropGrowthResult(
        String cropId,
        int beforeStage,
        int afterStage,
        boolean harvestReady,
        boolean paused,
        boolean failed,
        String reason,
        double growthMultiplier
) {
    public OpenlandsCropGrowthResult {
        if (cropId == null || cropId.isBlank()) {
            throw new IllegalArgumentException("cropId must not be blank");
        }
        beforeStage = Math.max(0, beforeStage);
        afterStage = Math.max(0, afterStage);
        growthMultiplier = Math.max(0.0, growthMultiplier);
    }
}
