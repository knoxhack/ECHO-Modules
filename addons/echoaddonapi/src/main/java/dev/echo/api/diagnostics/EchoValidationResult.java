package dev.echo.api.diagnostics;

import java.util.List;

public record EchoValidationResult(boolean valid, List<EchoDiagnostic> diagnostics) {
    public EchoValidationResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public static EchoValidationResult pass() {
        return new EchoValidationResult(true, List.of());
    }

    public static EchoValidationResult fail(List<EchoDiagnostic> diagnostics) {
        return new EchoValidationResult(false, diagnostics);
    }
}
