package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echocore.api.IRuntimeBudgetService;
import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.ProfilerEntry;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardBudgetCategories;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuntimeBudgetCoreService implements IRuntimeBudgetService {
    public static final RuntimeBudgetCoreService INSTANCE = new RuntimeBudgetCoreService();

    private RuntimeBudgetCoreService() {
    }

    @Override
    public boolean available() {
        return RuntimeGuardConfig.enabled();
    }

    @Override
    public double currentMs(String category) {
        String safeCategory = normalize(category);
        RuntimeMetricsSnapshot metrics = RuntimeProfilerService.INSTANCE.lastSnapshot();
        return switch (safeCategory) {
            case RuntimeGuardBudgetCategories.SERVER_TICK -> metrics.currentMspt();
            case RuntimeGuardBudgetCategories.CLIENT_FRAME -> 0.0D;
            case RuntimeGuardBudgetCategories.PROFILED_WORK -> RuntimeGuardProfiler.getTopCosts().stream()
                    .mapToDouble(ProfilerEntry::maxMillis)
                    .max()
                    .orElse(0.0D);
            case RuntimeGuardBudgetCategories.WORLDGEN -> topCostMillis("worldgen");
            case RuntimeGuardBudgetCategories.MULTIBLOCK_VALIDATION -> topCostMillis("multiblock_validation");
            case RuntimeGuardBudgetCategories.HOLOMAP_REFRESH -> topCostMillis("holomap");
            case RuntimeGuardBudgetCategories.LENS_SCAN -> topCostMillis("lens");
            case RuntimeGuardBudgetCategories.BLOCK_ENTITY -> topCostMillis("block_entity");
            case RuntimeGuardBudgetCategories.ENTITY_AI -> topCostMillis("entity_ai");
            case RuntimeGuardBudgetCategories.NETWORK, RuntimeGuardBudgetCategories.PARTICLES -> 0.0D;
            default -> 0.0D;
        };
    }

    @Override
    public double budgetMs(String category) {
        String safeCategory = normalize(category);
        return switch (safeCategory) {
            case RuntimeGuardBudgetCategories.SERVER_TICK ->
                    1000.0D / Math.max(1.0D, RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.WARNING_TPS, 18.0D));
            case RuntimeGuardBudgetCategories.CLIENT_FRAME ->
                    1000.0D / Math.max(1, RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARNING_FPS, 50));
            case RuntimeGuardBudgetCategories.WORLDGEN ->
                    RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARN_FEATURE_GEN_MS, 50);
            default -> Double.MAX_VALUE;
        };
    }

    @Override
    public boolean isOverBudget(String category) {
        double budget = budgetMs(category);
        return budget < Double.MAX_VALUE && currentMs(category) > budget;
    }

    @Override
    public Map<String, Double> snapshot() {
        Map<String, Double> snapshot = new LinkedHashMap<>();
        for (String category : RuntimeGuardBudgetCategories.ALL) {
            snapshot.put(category, currentMs(category));
        }
        return Map.copyOf(snapshot);
    }

    @Override
    public List<String> categories() {
        return RuntimeGuardBudgetCategories.ALL;
    }

    private static String normalize(String category) {
        return category == null || category.isBlank() ? RuntimeGuardBudgetCategories.PROFILED_WORK : category.trim();
    }

    private static double topCostMillis(String pathFragment) {
        return RuntimeGuardProfiler.getTopCosts().stream()
                .filter(entry -> entry.id().getPath().contains(pathFragment))
                .mapToDouble(ProfilerEntry::maxMillis)
                .max()
                .orElse(0.0D);
    }
}
