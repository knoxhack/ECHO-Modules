package com.knoxhack.echo.platformcore;

public record EchoCompatibilityIssue(
        String code,
        String title,
        String summary,
        EchoIntegrationStatus status,
        EchoRuntimeSide side,
        boolean blocking
) {
    public EchoCompatibilityIssue {
        code = EchoContractGuards.requireText(code, "compatibility issue code");
        title = EchoContractGuards.requireText(title, "compatibility issue title");
        summary = EchoContractGuards.optionalText(summary);
        status = status == null ? EchoIntegrationStatus.DEGRADED : status;
        side = side == null ? EchoRuntimeSide.COMMON : side;
    }
}
