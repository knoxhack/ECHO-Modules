package com.knoxhack.echo.packcore;

public record EchoInstallStateIssue(
        String code,
        String severity,
        boolean blocking,
        String summary,
        String moduleId,
        String fileName
) {
    public EchoInstallStateIssue {
        code = PackContractGuards.requireText(code, "install-state issue code");
        severity = PackContractGuards.requireText(severity, "install-state issue severity");
        summary = PackContractGuards.optionalText(summary);
        moduleId = PackContractGuards.optionalText(moduleId);
        fileName = PackContractGuards.optionalText(fileName);
    }
}
