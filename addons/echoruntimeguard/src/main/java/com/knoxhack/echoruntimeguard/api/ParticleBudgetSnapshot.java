package com.knoxhack.echoruntimeguard.api;

import java.util.EnumMap;
import java.util.Map;

public record ParticleBudgetSnapshot(
        int budget,
        int used,
        int denied,
        RuntimeMode mode,
        Map<ParticlePriority, Integer> byPriority) {
    public ParticleBudgetSnapshot {
        byPriority = byPriority == null || byPriority.isEmpty()
                ? Map.of()
                : Map.copyOf(new EnumMap<>(byPriority));
    }
}
