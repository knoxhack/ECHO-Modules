package com.knoxhack.echoarcanacore.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record AetherPlayerData(
        Map<AetherSignalType, Double> currentAether,
        Map<AetherSignalType, Double> maxAether,
        double regenerationPerSecond,
        Set<AetherSignalType> unlockedTypes,
        double contamination,
        double corruption,
        long lastCastGameTime,
        List<AetherModifier> activeModifiers) {
    public AetherPlayerData {
        currentAether = Map.copyOf(currentAether == null ? Map.of() : currentAether);
        maxAether = Map.copyOf(maxAether == null ? Map.of() : maxAether);
        regenerationPerSecond = Math.max(0.0D, regenerationPerSecond);
        unlockedTypes = Set.copyOf(unlockedTypes == null ? Set.of(AetherSignalType.RAW_AETHER) : unlockedTypes);
        contamination = Math.max(0.0D, contamination);
        corruption = Math.max(0.0D, corruption);
        activeModifiers = List.copyOf(activeModifiers == null ? List.of() : activeModifiers);
    }

    public static AetherPlayerData empty() {
        return new AetherPlayerData(Map.of(), Map.of(AetherSignalType.RAW_AETHER, 100.0D), 0.25D,
                Set.of(AetherSignalType.RAW_AETHER), 0.0D, 0.0D, 0L, List.of());
    }
}
