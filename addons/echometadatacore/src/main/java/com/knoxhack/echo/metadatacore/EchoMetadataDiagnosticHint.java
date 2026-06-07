package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.validationcore.EchoDiagnosticCode;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.Objects;

public record EchoMetadataDiagnosticHint(
        EchoDiagnosticCode code,
        EchoDiagnosticSeverity severity,
        EchoValidationCategory category,
        String summary,
        String usualFix
) {
    public EchoMetadataDiagnosticHint {
        Objects.requireNonNull(code, "code");
        severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
        category = category == null ? EchoValidationCategory.UNKNOWN : category;
        summary = MetadataContractGuards.optionalText(summary);
        usualFix = MetadataContractGuards.optionalText(usualFix);
    }
}
