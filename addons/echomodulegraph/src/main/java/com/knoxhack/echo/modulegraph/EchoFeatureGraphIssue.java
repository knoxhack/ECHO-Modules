package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.List;
import java.util.Objects;

public record EchoFeatureGraphIssue(
        String code,
        EchoDiagnosticSeverity severity,
        EchoFeatureId featureId,
        EchoModuleId moduleId,
        String summary,
        String suggestedFix,
        boolean blocking,
        List<String> likelyFiles
) {
    public EchoFeatureGraphIssue {
        code = ModuleGraphContractGuards.requireText(code, "feature graph issue code");
        severity = severity == null ? EchoDiagnosticSeverity.NOTICE : severity;
        Objects.requireNonNull(featureId, "featureId");
        summary = ModuleGraphContractGuards.optionalText(summary);
        suggestedFix = ModuleGraphContractGuards.optionalText(suggestedFix);
        likelyFiles = ModuleGraphContractGuards.immutableList(likelyFiles);
    }
}
