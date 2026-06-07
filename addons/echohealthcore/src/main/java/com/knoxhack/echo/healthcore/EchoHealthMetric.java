package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Map;
import java.util.Objects;

public record EchoHealthMetric(
        EchoHealthMetricId id,
        String name,
        String summary,
        double value,
        EchoHealthMetricUnit unit,
        EchoHealthStatus status,
        EchoModuleId moduleId,
        EchoFeatureId featureId,
        EchoRuntimeSide side,
        long capturedAtEpochMillis,
        Map<String, String> tags
) {
    public EchoHealthMetric {
        Objects.requireNonNull(id, "id");
        name = HealthContractGuards.requireText(name, "health metric name");
        summary = HealthContractGuards.optionalText(summary);
        value = HealthContractGuards.nonNegative(value, "health metric value");
        unit = unit == null ? EchoHealthMetricUnit.COUNT : unit;
        status = status == null ? EchoHealthStatus.UNKNOWN : status;
        side = side == null ? EchoRuntimeSide.COMMON : side;
        capturedAtEpochMillis = HealthContractGuards.nonNegative(capturedAtEpochMillis, "captured timestamp");
        tags = HealthContractGuards.immutableMap(tags);
    }

    public static EchoHealthMetric of(EchoHealthMetricId id, String name, double value, EchoHealthMetricUnit unit, EchoHealthStatus status) {
        return new EchoHealthMetric(id, name, "", value, unit, status, null, null, EchoRuntimeSide.COMMON, 0L, Map.of());
    }
}
