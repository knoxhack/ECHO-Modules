package com.knoxhack.echo.validationcore;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record EchoDiagnosticReport(
        String title,
        EchoDiagnosticContext context,
        Instant generatedAt,
        List<EchoDiagnostic> diagnostics
) {
    public EchoDiagnosticReport {
        title = ValidationContractGuards.requireText(title, "report title");
        context = context == null ? EchoDiagnosticContext.workspace() : context;
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        diagnostics = ValidationContractGuards.immutableList(diagnostics);
    }

    public static EchoDiagnosticReport empty(String title, EchoDiagnosticContext context) {
        return new EchoDiagnosticReport(title, context, Instant.now(), List.of());
    }

    public static EchoDiagnosticReport of(String title, EchoDiagnosticContext context, List<EchoDiagnostic> diagnostics) {
        return new EchoDiagnosticReport(title, context, Instant.now(), diagnostics);
    }

    public boolean hasBlockingDiagnostics() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }

    public EchoDiagnosticSeverity highestSeverity() {
        return diagnostics.stream()
                .map(EchoDiagnostic::severity)
                .max(Comparator.comparingInt(EchoDiagnosticSeverity::rank))
                .orElse(EchoDiagnosticSeverity.INFO);
    }
}
