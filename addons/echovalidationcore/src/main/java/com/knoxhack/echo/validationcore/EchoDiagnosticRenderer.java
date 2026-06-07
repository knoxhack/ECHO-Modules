package com.knoxhack.echo.validationcore;

public final class EchoDiagnosticRenderer {
    private EchoDiagnosticRenderer() {
    }

    public static String render(EchoDiagnostic diagnostic) {
        StringBuilder text = new StringBuilder();
        text.append("[")
                .append(diagnostic.severity().serializedName())
                .append("] ")
                .append(diagnostic.code().value())
                .append(" - ")
                .append(diagnostic.title());
        if (!diagnostic.summary().isBlank()) {
            text.append(System.lineSeparator()).append(diagnostic.summary());
        }
        if (diagnostic.moduleId() != null) {
            text.append(System.lineSeparator()).append("Module: ").append(diagnostic.moduleId().value());
        }
        if (diagnostic.packId() != null) {
            text.append(System.lineSeparator()).append("Pack: ").append(diagnostic.packId().value());
        }
        text.append(System.lineSeparator()).append("Category: ").append(diagnostic.category().serializedName());
        if (!diagnostic.cause().isBlank()) {
            text.append(System.lineSeparator()).append("Cause: ").append(diagnostic.cause());
        }
        if (!diagnostic.playerFix().isBlank()) {
            text.append(System.lineSeparator()).append("Player fix: ").append(diagnostic.playerFix());
        }
        if (!diagnostic.developerDetails().isBlank()) {
            text.append(System.lineSeparator()).append("Developer details: ").append(diagnostic.developerDetails());
        }
        if (!diagnostic.suggestedAgentLane().isBlank()) {
            text.append(System.lineSeparator()).append("Agent lane: ").append(diagnostic.suggestedAgentLane());
        }
        if (!diagnostic.relatedDocs().isEmpty()) {
            text.append(System.lineSeparator()).append("Docs: ").append(String.join(", ", diagnostic.relatedDocs()));
        }
        return text.toString();
    }

    public static String renderReport(EchoDiagnosticReport report) {
        StringBuilder text = new StringBuilder(report.title())
                .append(System.lineSeparator())
                .append("Generated: ")
                .append(report.generatedAt())
                .append(System.lineSeparator())
                .append("Highest severity: ")
                .append(report.highestSeverity().serializedName())
                .append(System.lineSeparator())
                .append("Blocking: ")
                .append(report.hasBlockingDiagnostics());
        for (EchoDiagnostic diagnostic : report.diagnostics()) {
            text.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(render(diagnostic));
        }
        return text.toString();
    }
}
