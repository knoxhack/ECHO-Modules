package com.knoxhack.echo.powercore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoPowerInstability(
        EchoPowerInstabilityKind kind,
        double severity,
        String summary,
        EchoContentReference weatherEventReference,
        EchoContentReference nexusCorruptionReference,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoPowerInstability {
        kind = kind == null ? EchoPowerInstabilityKind.UNKNOWN : kind;
        severity = PowerContractGuards.clamped01(severity);
        summary = PowerContractGuards.optionalText(summary);
        diagnostics = PowerContractGuards.immutableList(diagnostics);
        attributes = PowerContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return severity >= 1.0D || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
