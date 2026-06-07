package dev.echo.api.diagnostics;

import dev.echo.api.platform.EchoRuntimeKind;
import java.time.Instant;
import java.util.Map;

public record EchoRuntimeEvidence(
        EchoRuntimeKind runtime,
        String evidenceId,
        String status,
        Instant generatedAt,
        Map<String, String> values
) {
    public EchoRuntimeEvidence {
        values = Map.copyOf(values);
    }
}
