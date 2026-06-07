package com.knoxhack.echo.validationcore;

import java.util.List;
import java.util.Objects;

public record EchoValidationResult(
        EchoValidationTarget target,
        List<EchoDiagnostic> diagnostics
) {
    public EchoValidationResult {
        Objects.requireNonNull(target, "target");
        diagnostics = ValidationContractGuards.immutableList(diagnostics);
    }

    public static EchoValidationResult passed(EchoValidationTarget target) {
        return new EchoValidationResult(target, List.of());
    }

    public static EchoValidationResult of(EchoValidationTarget target, List<EchoDiagnostic> diagnostics) {
        return new EchoValidationResult(target, diagnostics);
    }

    public boolean valid() {
        return diagnostics.stream().noneMatch(EchoDiagnostic::blocking);
    }
}
