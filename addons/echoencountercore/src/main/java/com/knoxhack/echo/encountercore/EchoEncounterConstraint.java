package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoEncounterConstraint(
        String constraintId,
        EchoEncounterConstraintKind kind,
        EchoContentReference reference,
        EchoContentGate gate,
        double minValue,
        double maxValue,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoEncounterConstraint {
        constraintId = EncounterContractGuards.id(constraintId, "encounter constraint id");
        kind = kind == null ? EchoEncounterConstraintKind.UNKNOWN : kind;
        gate = gate == null ? EchoContentGate.open() : gate;
        minValue = EncounterContractGuards.nonNegative(minValue, "constraint min value");
        maxValue = EncounterContractGuards.nonNegative(maxValue, "constraint max value");
        if (maxValue != 0.0D && maxValue < minValue) {
            throw new IllegalArgumentException("constraint max value must be zero or greater than or equal to min value");
        }
        diagnostics = EncounterContractGuards.immutableList(diagnostics);
        attributes = EncounterContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return gate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
