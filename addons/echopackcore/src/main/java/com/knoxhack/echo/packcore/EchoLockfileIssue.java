package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.List;
import java.util.Objects;

public record EchoLockfileIssue(
        String code,
        EchoDiagnosticSeverity severity,
        String title,
        String summary,
        EchoModuleId moduleId,
        EchoPackId packId,
        EchoFeatureId featureId,
        boolean blocking,
        List<String> likelyFiles,
        String suggestedFix
) {
    public EchoLockfileIssue {
        code = PackContractGuards.requireText(code, "lockfile issue code");
        severity = Objects.requireNonNullElse(severity, EchoDiagnosticSeverity.WARNING);
        title = PackContractGuards.optionalText(title);
        summary = PackContractGuards.optionalText(summary);
        blocking = blocking || severity.blocking();
        likelyFiles = PackContractGuards.immutableList(likelyFiles);
        suggestedFix = PackContractGuards.optionalText(suggestedFix);
    }
}
