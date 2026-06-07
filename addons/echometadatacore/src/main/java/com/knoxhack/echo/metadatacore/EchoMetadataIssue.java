package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.List;
import java.util.Objects;

public record EchoMetadataIssue(
        String code,
        EchoDiagnosticSeverity severity,
        EchoModuleId moduleId,
        EchoFeatureId featureId,
        String summary,
        List<String> likelyFiles,
        String suggestedFix,
        String suggestedAgentLane,
        boolean blocking
) {
    public EchoMetadataIssue {
        code = MetadataContractGuards.requireText(code, "metadata issue code");
        severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
        summary = MetadataContractGuards.requireText(summary, "metadata issue summary");
        likelyFiles = MetadataContractGuards.immutableList(likelyFiles);
        suggestedFix = MetadataContractGuards.optionalText(suggestedFix);
        suggestedAgentLane = MetadataContractGuards.optionalText(suggestedAgentLane);
        blocking = blocking || severity.blocking();
    }

    public static EchoMetadataIssue of(
            String code,
            EchoDiagnosticSeverity severity,
            EchoModuleId moduleId,
            String summary,
            List<String> likelyFiles
    ) {
        return new EchoMetadataIssue(
                code,
                severity,
                Objects.requireNonNull(moduleId, "moduleId"),
                null,
                summary,
                likelyFiles,
                "",
                "diagnostics_agent",
                severity != null && severity.blocking()
        );
    }
}
