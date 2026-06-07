package com.knoxhack.echo.inputcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoInputConflict(
        EchoInputBindingId primaryBinding,
        EchoInputBindingId conflictingBinding,
        String conflictReason,
        boolean blocking,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoInputConflict {
        conflictReason = InputContractGuards.optionalText(conflictReason);
        diagnostics = InputContractGuards.immutableList(diagnostics);
        attributes = InputContractGuards.immutableMap(attributes);
        blocking = blocking || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
