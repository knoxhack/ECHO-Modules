package dev.echo.api.diagnostics;

import java.util.List;

public record EchoParityEvidence(String behaviorId, boolean matched, List<EchoRuntimeEvidence> runtimeEvidence) {
    public EchoParityEvidence {
        runtimeEvidence = List.copyOf(runtimeEvidence);
    }
}
