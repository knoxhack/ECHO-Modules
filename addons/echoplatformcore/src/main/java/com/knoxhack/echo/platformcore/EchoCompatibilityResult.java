package com.knoxhack.echo.platformcore;

import java.util.Set;

public record EchoCompatibilityResult(
        boolean compatible,
        EchoIntegrationStatus status,
        Set<EchoCompatibilityIssue> issues
) {
    public EchoCompatibilityResult {
        status = status == null ? EchoIntegrationStatus.ACTIVE : status;
        issues = EchoContractGuards.immutableSet(issues);
    }

    public static EchoCompatibilityResult ok() {
        return new EchoCompatibilityResult(true, EchoIntegrationStatus.ACTIVE, Set.of());
    }

    public static EchoCompatibilityResult degraded(Set<EchoCompatibilityIssue> issues) {
        return new EchoCompatibilityResult(true, EchoIntegrationStatus.DEGRADED, issues);
    }

    public static EchoCompatibilityResult incompatible(Set<EchoCompatibilityIssue> issues) {
        return new EchoCompatibilityResult(false, EchoIntegrationStatus.INCOMPATIBLE, issues);
    }
}
