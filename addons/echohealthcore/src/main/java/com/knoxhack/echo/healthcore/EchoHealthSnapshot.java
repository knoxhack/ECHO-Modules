package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record EchoHealthSnapshot(
        String snapshotId,
        long capturedAtEpochMillis,
        EchoHealthStatus status,
        List<EchoHealthMetric> metrics,
        List<EchoRuntimeObservation> observations,
        List<EchoBudgetViolation> budgetViolations,
        List<EchoDegradedFeature> degradedFeatures,
        EchoCrashContext crashContext,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoHealthSnapshot {
        snapshotId = HealthContractGuards.requireText(snapshotId, "health snapshot id");
        capturedAtEpochMillis = HealthContractGuards.nonNegative(capturedAtEpochMillis, "snapshot timestamp");
        metrics = HealthContractGuards.immutableList(metrics);
        observations = HealthContractGuards.immutableList(observations);
        budgetViolations = HealthContractGuards.immutableList(budgetViolations);
        degradedFeatures = HealthContractGuards.immutableList(degradedFeatures);
        crashContext = crashContext == null ? EchoCrashContext.none() : crashContext;
        diagnostics = HealthContractGuards.immutableList(diagnostics);
        attributes = HealthContractGuards.immutableMap(attributes);
        status = status == null ? deriveStatus(metrics, observations, budgetViolations, degradedFeatures, crashContext) : status;
    }

    public List<EchoDiagnostic> allDiagnostics() {
        List<EchoDiagnostic> all = new ArrayList<>(diagnostics);
        budgetViolations.stream().map(EchoBudgetViolation::toDiagnostic).forEach(all::add);
        degradedFeatures.stream().map(EchoDegradedFeature::toDiagnostic).forEach(all::add);
        observations.stream().flatMap(observation -> observation.diagnostics().stream()).forEach(all::add);
        return List.copyOf(all);
    }

    private static EchoHealthStatus deriveStatus(
            List<EchoHealthMetric> metrics,
            List<EchoRuntimeObservation> observations,
            List<EchoBudgetViolation> budgetViolations,
            List<EchoDegradedFeature> degradedFeatures,
            EchoCrashContext crashContext
    ) {
        List<EchoHealthStatus> statuses = new ArrayList<>();
        metrics.stream().map(EchoHealthMetric::status).forEach(statuses::add);
        observations.stream().map(EchoRuntimeObservation::status).forEach(statuses::add);
        budgetViolations.stream().map(EchoBudgetViolation::status).forEach(statuses::add);
        degradedFeatures.stream().map(EchoDegradedFeature::status).forEach(statuses::add);
        if (crashContext != null && !"none".equals(crashContext.crashId())) {
            statuses.add(EchoHealthStatus.CRITICAL);
        }
        return statuses.isEmpty() ? EchoHealthStatus.UNKNOWN : EchoHealthStatus.worst(statuses);
    }

    public static EchoHealthSnapshot empty(String snapshotId) {
        return new EchoHealthSnapshot(snapshotId, 0L, EchoHealthStatus.UNKNOWN, List.of(), List.of(), List.of(), List.of(), EchoCrashContext.none(), List.of(), Map.of());
    }
}
