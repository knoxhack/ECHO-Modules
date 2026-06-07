package dev.echo.api.lifecycle;

import dev.echo.api.diagnostics.EchoDiagnostic;
import java.util.List;

public record EchoLifecycleResult(boolean successful, List<EchoDiagnostic> diagnostics) {
    public EchoLifecycleResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public static EchoLifecycleResult pass() {
        return new EchoLifecycleResult(true, List.of());
    }

    public static EchoLifecycleResult fail(List<EchoDiagnostic> diagnostics) {
        return new EchoLifecycleResult(false, diagnostics);
    }
}
