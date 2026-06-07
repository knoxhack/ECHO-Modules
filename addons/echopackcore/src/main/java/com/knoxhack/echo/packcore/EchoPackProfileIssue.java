package com.knoxhack.echo.packcore;

import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.List;
import java.util.Map;

public record EchoPackProfileIssue(
        String code,
        EchoDiagnosticSeverity severity,
        String summary,
        EchoPackProfileSource source,
        List<String> likelyFiles,
        String suggestedFix,
        Map<String, String> attributes
) {
    public EchoPackProfileIssue {
        code = PackContractGuards.requireText(code, "pack profile issue code");
        severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
        summary = PackContractGuards.requireText(summary, "pack profile issue summary");
        likelyFiles = PackContractGuards.immutableList(likelyFiles);
        suggestedFix = PackContractGuards.optionalText(suggestedFix);
        attributes = PackContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return severity.blocking();
    }
}
