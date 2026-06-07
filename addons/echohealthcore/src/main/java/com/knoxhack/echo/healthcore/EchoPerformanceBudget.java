package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record EchoPerformanceBudget(
        String id,
        String name,
        EchoRuntimeSide side,
        List<EchoHealthBudget> budgets,
        Map<String, String> attributes
) {
    public EchoPerformanceBudget {
        id = HealthContractGuards.requireText(id, "performance budget id");
        name = HealthContractGuards.requireText(name, "performance budget name");
        side = side == null ? EchoRuntimeSide.COMMON : side;
        budgets = HealthContractGuards.immutableList(budgets);
        attributes = HealthContractGuards.immutableMap(attributes);
    }

    public List<EchoBudgetViolation> evaluate(List<EchoHealthMetric> metrics) {
        Map<EchoHealthMetricId, EchoHealthMetric> byMetricId = HealthContractGuards.immutableList(metrics)
                .stream()
                .collect(Collectors.toMap(EchoHealthMetric::id, Function.identity(), (left, right) -> left));
        List<EchoBudgetViolation> violations = new ArrayList<>();
        for (EchoHealthBudget budget : budgets) {
            EchoHealthMetric metric = byMetricId.get(budget.metricId());
            if (metric == null) {
                continue;
            }
            EchoHealthStatus status = budget.evaluate(metric.value());
            if (status.requiresAttention()) {
                violations.add(new EchoBudgetViolation(
                        budget,
                        metric,
                        status,
                        metric.name() + " is outside the " + budget.name() + " budget.",
                        false,
                        List.of()
                ));
            }
        }
        return List.copyOf(violations);
    }
}
