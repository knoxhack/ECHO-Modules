package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoLockfileVerificationIssue(
        String code,
        String severity,
        String summary,
        String moduleId,
        String featureId,
        String fileName,
        boolean blocking,
        boolean repairable,
        List<String> likelyFiles
) {
    public EchoLockfileVerificationIssue {
        code = PackContractGuards.requireText(code, "lockfile verification issue code");
        severity = PackContractGuards.requireText(severity, "lockfile verification issue severity");
        summary = PackContractGuards.optionalText(summary);
        moduleId = PackContractGuards.optionalText(moduleId);
        featureId = PackContractGuards.optionalText(featureId);
        fileName = PackContractGuards.optionalText(fileName);
        likelyFiles = PackContractGuards.immutableList(likelyFiles);
    }
}
