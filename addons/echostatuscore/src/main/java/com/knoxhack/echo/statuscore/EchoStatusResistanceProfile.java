package com.knoxhack.echo.statuscore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoStatusResistanceProfile(
        String resistanceId,
        EchoStatusId statusId,
        EchoContentReference providerReference,
        double mitigationRatio,
        double immunityThreshold,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoStatusResistanceProfile {
        resistanceId = StatusContractGuards.requireText(resistanceId, "status resistance id");
        Objects.requireNonNull(statusId, "statusId");
        mitigationRatio = StatusContractGuards.ratio(mitigationRatio, "status resistance mitigation ratio");
        immunityThreshold = StatusContractGuards.nonNegative(immunityThreshold, "status resistance immunity threshold");
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = StatusContractGuards.immutableList(diagnostics);
        attributes = StatusContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
