package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Objects;

public record EchoRegistryParityResult(
        boolean passed,
        List<String> passedChecks,
        List<String> failedChecks
) {
    public EchoRegistryParityResult {
        passedChecks = List.copyOf(Objects.requireNonNull(passedChecks, "passedChecks"));
        failedChecks = List.copyOf(Objects.requireNonNull(failedChecks, "failedChecks"));
        passed = failedChecks.isEmpty();
    }
}
