package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoRuntimeObservation(
        String id,
        long observedAtEpochMillis,
        EchoModuleId moduleId,
        EchoFeatureId featureId,
        EchoRuntimeSide side,
        EchoHealthStatus status,
        String summary,
        List<EchoHealthMetric> metrics,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoRuntimeObservation {
        id = HealthContractGuards.requireText(id, "runtime observation id");
        observedAtEpochMillis = HealthContractGuards.nonNegative(observedAtEpochMillis, "observation timestamp");
        side = side == null ? EchoRuntimeSide.COMMON : side;
        status = status == null ? EchoHealthStatus.UNKNOWN : status;
        summary = HealthContractGuards.optionalText(summary);
        metrics = HealthContractGuards.immutableList(metrics);
        diagnostics = HealthContractGuards.immutableList(diagnostics);
        attributes = HealthContractGuards.immutableMap(attributes);
    }
}
