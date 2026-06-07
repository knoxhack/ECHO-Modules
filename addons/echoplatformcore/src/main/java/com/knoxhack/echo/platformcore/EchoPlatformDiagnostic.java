package com.knoxhack.echo.platformcore;

public record EchoPlatformDiagnostic(
        String code,
        String title,
        String summary,
        EchoIntegrationStatus status,
        EchoModuleId moduleId,
        EchoFeatureId featureId,
        boolean repairable,
        String recommendation
) {
    public EchoPlatformDiagnostic {
        code = EchoContractGuards.requireText(code, "diagnostic code");
        title = EchoContractGuards.requireText(title, "diagnostic title");
        summary = EchoContractGuards.optionalText(summary);
        status = status == null ? EchoIntegrationStatus.DEGRADED : status;
        recommendation = EchoContractGuards.optionalText(recommendation);
    }
}
