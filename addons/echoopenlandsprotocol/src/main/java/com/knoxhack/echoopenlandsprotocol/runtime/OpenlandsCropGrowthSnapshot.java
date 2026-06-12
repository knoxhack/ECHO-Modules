package com.knoxhack.echoopenlandsprotocol.runtime;

public record OpenlandsCropGrowthSnapshot(
        String cropId,
        int currentStage,
        int elapsedMinutes,
        boolean watered,
        boolean composted,
        boolean hardlands
) {
    public OpenlandsCropGrowthSnapshot {
        if (cropId == null || cropId.isBlank()) {
            throw new IllegalArgumentException("cropId must not be blank");
        }
        currentStage = Math.max(0, currentStage);
        elapsedMinutes = Math.max(0, elapsedMinutes);
    }
}
