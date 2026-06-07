package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import java.util.EnumMap;
import java.util.Map;

public final class PerformanceBudgetService {
    public static final PerformanceBudgetService INSTANCE = new PerformanceBudgetService();
    private final EnumMap<RuntimeWorkType, Integer> usage = new EnumMap<>(RuntimeWorkType.class);

    private PerformanceBudgetService() {
    }

    public synchronized boolean tryAcquire(RuntimeWorkType type, int cost) {
        if (!RuntimeGuardConfig.enabled()) {
            return true;
        }
        RuntimeWorkType safeType = type == null ? RuntimeWorkType.BLOCK_ENTITY : type;
        int budget = budgetFor(safeType);
        int current = usage.getOrDefault(safeType, 0);
        if (current + Math.max(1, cost) > budget) {
            return false;
        }
        usage.put(safeType, current + Math.max(1, cost));
        return true;
    }

    public synchronized void resetTick() {
        usage.clear();
    }

    public synchronized void reset() {
        usage.clear();
    }

    public synchronized Map<RuntimeWorkType, Integer> usageSnapshot() {
        return Map.copyOf(usage);
    }

    public int budgetFor(RuntimeWorkType type) {
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        int base = switch (type == null ? RuntimeWorkType.BLOCK_ENTITY : type) {
            case MULTIBLOCK_VALIDATION -> 512;
            case HOLOMAP_REFRESH -> 128;
            case LENS_SCAN -> 128;
            case PARTICLE -> 1200;
            case NETWORK_SYNC -> 300;
            case ROBOTICS_ANIMATION -> 80;
            case CONVOY_SIMULATION -> 64;
            case ORBITAL_TELEMETRY -> 64;
            case RECLAMATION_UPDATE -> 64;
            case NEXUS_STORM -> 64;
            case BLOCK_ENTITY -> 512;
        };
        return switch (mode) {
            case POTATO, SERVER -> Math.max(1, base / 2);
            case CINEMATIC, DEBUG -> base * 2;
            case EMERGENCY -> Math.max(1, base / 4);
            case BALANCED -> base;
        };
    }
}
