package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.validationcore.EchoAffectedFeature;
import com.knoxhack.echo.validationcore.EchoDiagnostic;
import com.knoxhack.echo.validationcore.EchoDiagnosticCode;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;
import java.util.Objects;

public record EchoBudgetViolation(
        EchoHealthBudget budget,
        EchoHealthMetric metric,
        EchoHealthStatus status,
        String summary,
        boolean repairable,
        List<String> suggestedActions
) {
    public EchoBudgetViolation {
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(metric, "metric");
        status = status == null ? budget.evaluate(metric.value()) : status;
        summary = HealthContractGuards.optionalText(summary);
        suggestedActions = HealthContractGuards.immutableList(suggestedActions);
        repairable = repairable || !suggestedActions.isEmpty();
    }

    public EchoDiagnostic toDiagnostic() {
        EchoDiagnosticSeverity severity = status == EchoHealthStatus.CRITICAL
                ? EchoDiagnosticSeverity.ERROR
                : EchoDiagnosticSeverity.WARNING;
        EchoDiagnostic.Builder builder = EchoDiagnostic.builder(
                        EchoDiagnosticCode.of("health.budget." + budget.metricId().value()),
                        severity,
                        budget.name() + " budget exceeded",
                        summary.isBlank() ? metric.name() + " is outside its runtime health budget." : summary
                )
                .category(EchoValidationCategory.RUNTIME_HEALTH)
                .moduleId(metric.moduleId())
                .developerDetails("Metric " + metric.id().value() + "=" + metric.value() + " " + metric.unit().serializedName())
                .repairable(repairable);
        if (metric.featureId() != null) {
            builder.affectedFeature(new EchoAffectedFeature(metric.featureId(), metric.featureId().value(), EchoValidationCategory.RUNTIME_HEALTH));
        }
        for (String action : suggestedActions) {
            builder.safeCommand(action);
        }
        return builder.build();
    }
}
