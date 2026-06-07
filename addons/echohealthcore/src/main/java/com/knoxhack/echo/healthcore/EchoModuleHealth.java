package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record EchoModuleHealth(
        EchoModuleId moduleId,
        String moduleName,
        EchoHealthStatus status,
        List<EchoHealthMetric> metrics,
        List<EchoBudgetViolation> budgetViolations,
        List<EchoDegradedFeature> degradedFeatures,
        List<EchoRuntimeObservation> observations,
        List<EchoDiagnostic> diagnostics
) {
    public EchoModuleHealth {
        Objects.requireNonNull(moduleId, "moduleId");
        moduleName = HealthContractGuards.optionalText(moduleName);
        metrics = HealthContractGuards.immutableList(metrics);
        budgetViolations = HealthContractGuards.immutableList(budgetViolations);
        degradedFeatures = HealthContractGuards.immutableList(degradedFeatures);
        observations = HealthContractGuards.immutableList(observations);
        diagnostics = HealthContractGuards.immutableList(diagnostics);
        status = status == null ? deriveStatus(metrics, budgetViolations, degradedFeatures, observations) : status;
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
            List<EchoBudgetViolation> budgetViolations,
            List<EchoDegradedFeature> degradedFeatures,
            List<EchoRuntimeObservation> observations
    ) {
        List<EchoHealthStatus> statuses = new ArrayList<>();
        metrics.stream().map(EchoHealthMetric::status).forEach(statuses::add);
        budgetViolations.stream().map(EchoBudgetViolation::status).forEach(statuses::add);
        degradedFeatures.stream().map(EchoDegradedFeature::status).forEach(statuses::add);
        observations.stream().map(EchoRuntimeObservation::status).forEach(statuses::add);
        return statuses.isEmpty() ? EchoHealthStatus.UNKNOWN : EchoHealthStatus.worst(statuses);
    }
}
