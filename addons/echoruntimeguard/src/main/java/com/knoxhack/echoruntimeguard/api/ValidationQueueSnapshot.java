package com.knoxhack.echoruntimeguard.api;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record ValidationQueueSnapshot(
        int queued,
        int dirtyPositions,
        int ranLastTick,
        int mergedRequests,
        Map<Identifier, Integer> bySystem) {
    public ValidationQueueSnapshot {
        bySystem = bySystem == null || bySystem.isEmpty() ? Map.of() : Map.copyOf(bySystem);
    }
}
