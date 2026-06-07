package com.knoxhack.echo.scriptcore.api;

import java.util.List;

public record EchoScriptRuntimeMigrationReport(
        boolean supported,
        boolean applied,
        int candidates,
        int copied,
        int skipped,
        List<EchoScriptRuntimeMigrationEntry> entries,
        List<EchoScriptDiagnostic> diagnostics) {
    public EchoScriptRuntimeMigrationReport {
        entries = List.copyOf(entries == null ? List.of() : entries);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }
}
