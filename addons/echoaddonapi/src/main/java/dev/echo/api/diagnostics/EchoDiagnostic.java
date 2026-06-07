package dev.echo.api.diagnostics;

import java.util.Map;
import java.util.Objects;

public record EchoDiagnostic(
        EchoDiagnosticSeverity severity,
        String code,
        String message,
        Map<String, String> details
) {
    public EchoDiagnostic {
        Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code").trim();
        message = Objects.requireNonNull(message, "message").trim();
        details = Map.copyOf(details);
        if (code.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("diagnostic code and message must not be blank");
        }
    }
}
