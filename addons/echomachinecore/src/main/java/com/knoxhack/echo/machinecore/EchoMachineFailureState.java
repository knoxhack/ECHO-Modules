package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoMachineFailureState(
        EchoMachineFailureKind kind,
        double severity,
        String playerSummary,
        String developerDetails,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoMachineFailureState {
        kind = kind == null ? EchoMachineFailureKind.UNKNOWN : kind;
        severity = MachineContractGuards.clamped01(severity);
        playerSummary = MachineContractGuards.optionalText(playerSummary);
        developerDetails = MachineContractGuards.optionalText(developerDetails);
        diagnostics = MachineContractGuards.immutableList(diagnostics);
        attributes = MachineContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return severity >= 1.0D || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
