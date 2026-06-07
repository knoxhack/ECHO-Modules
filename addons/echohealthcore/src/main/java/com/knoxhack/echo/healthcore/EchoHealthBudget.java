package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Objects;

public record EchoHealthBudget(
        EchoHealthMetricId metricId,
        String name,
        String summary,
        double warningThreshold,
        double criticalThreshold,
        EchoHealthMetricUnit unit,
        boolean lowerIsBetter,
        EchoRuntimeSide side
) {
    public EchoHealthBudget {
        Objects.requireNonNull(metricId, "metricId");
        name = HealthContractGuards.requireText(name, "health budget name");
        summary = HealthContractGuards.optionalText(summary);
        warningThreshold = HealthContractGuards.nonNegative(warningThreshold, "warning threshold");
        criticalThreshold = HealthContractGuards.nonNegative(criticalThreshold, "critical threshold");
        unit = unit == null ? EchoHealthMetricUnit.COUNT : unit;
        side = side == null ? EchoRuntimeSide.COMMON : side;
    }

    public EchoHealthStatus evaluate(double value) {
        double safeValue = HealthContractGuards.nonNegative(value, "budget value");
        if (lowerIsBetter) {
            if (safeValue >= criticalThreshold) {
                return EchoHealthStatus.CRITICAL;
            }
            if (safeValue >= warningThreshold) {
                return EchoHealthStatus.WARNING;
            }
            return EchoHealthStatus.HEALTHY;
        }
        if (safeValue <= criticalThreshold) {
            return EchoHealthStatus.CRITICAL;
        }
        if (safeValue <= warningThreshold) {
            return EchoHealthStatus.WARNING;
        }
        return EchoHealthStatus.HEALTHY;
    }
}
