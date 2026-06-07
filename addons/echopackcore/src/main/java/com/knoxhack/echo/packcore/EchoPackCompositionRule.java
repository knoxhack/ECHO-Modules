package com.knoxhack.echo.packcore;

import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;

public record EchoPackCompositionRule(
        String id,
        EchoValidationCategory category,
        EchoDiagnosticSeverity severity,
        String summary,
        List<String> requiredWhen,
        List<String> conflictsWith
) {
    public EchoPackCompositionRule {
        id = PackContractGuards.requireText(id, "composition rule id");
        category = category == null ? EchoValidationCategory.PACK_PROFILE : category;
        severity = severity == null ? EchoDiagnosticSeverity.WARNING : severity;
        summary = PackContractGuards.optionalText(summary);
        requiredWhen = PackContractGuards.immutableList(requiredWhen);
        conflictsWith = PackContractGuards.immutableList(conflictsWith);
    }
}
