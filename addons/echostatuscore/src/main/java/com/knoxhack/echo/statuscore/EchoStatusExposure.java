package com.knoxhack.echo.statuscore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoStatusExposure(
        String exposureId,
        EchoStatusId statusId,
        EchoStatusKind kind,
        EchoContentReference sourceReference,
        double intensity,
        int durationTicks,
        double accumulationPerSecond,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoStatusExposure {
        exposureId = StatusContractGuards.requireText(exposureId, "status exposure id");
        Objects.requireNonNull(statusId, "statusId");
        kind = kind == null ? EchoStatusKind.UNKNOWN : kind;
        intensity = StatusContractGuards.nonNegative(intensity, "status exposure intensity");
        durationTicks = StatusContractGuards.nonNegative(durationTicks, "status exposure duration ticks");
        accumulationPerSecond = StatusContractGuards.nonNegative(accumulationPerSecond, "status exposure accumulation per second");
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = StatusContractGuards.immutableList(diagnostics);
        attributes = StatusContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
