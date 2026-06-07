package com.knoxhack.echoruntimeguard.api;

import java.util.List;
import java.util.Map;

public record RuntimeGuardStatusSnapshot(
        boolean enabled,
        RuntimeMode configuredMode,
        RuntimeMode effectiveMode,
        String modeSource,
        boolean forcedEmergency,
        boolean automaticEmergency,
        RuntimeMetricsSnapshot metrics,
        ClientMetricsSnapshot clientMetrics,
        ParticleBudgetSnapshot particles,
        ValidationQueueSnapshot validations,
        NetworkSnapshot network,
        Map<RuntimeWorkType, Integer> workUsage,
        List<ProfilerEntry> topCosts,
        String entityGuardStatus,
        int trackedBlockEntities,
        String integrationStatus) {
    public RuntimeGuardStatusSnapshot {
        modeSource = modeSource == null || modeSource.isBlank() ? "configured" : modeSource;
        metrics = metrics == null ? RuntimeMetricsSnapshot.unavailable(effectiveMode, forcedEmergency || automaticEmergency) : metrics;
        clientMetrics = clientMetrics == null ? ClientMetricsSnapshot.unavailable(effectiveMode) : clientMetrics;
        workUsage = workUsage == null || workUsage.isEmpty() ? Map.of() : Map.copyOf(workUsage);
        topCosts = topCosts == null || topCosts.isEmpty() ? List.of() : List.copyOf(topCosts);
        entityGuardStatus = entityGuardStatus == null ? "" : entityGuardStatus;
        integrationStatus = integrationStatus == null ? "" : integrationStatus;
    }
}
