package com.knoxhack.echo.scriptcore.api;

import java.nio.file.Path;
import java.util.List;

public record EchoScriptLoadResult(
        int loadedCount,
        int failedCount,
        int warningCount,
        int errorCount,
        List<EchoScriptDefinitionView> definitions,
        List<EchoScriptDiagnostic> diagnostics,
        List<Path> loadedFiles,
        List<Path> failedFiles,
        long durationMs) {
    public EchoScriptLoadResult {
        definitions = List.copyOf(definitions == null ? List.of() : definitions);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        loadedFiles = List.copyOf(loadedFiles == null ? List.of() : loadedFiles);
        failedFiles = List.copyOf(failedFiles == null ? List.of() : failedFiles);
    }

    public static EchoScriptLoadResult empty() {
        return new EchoScriptLoadResult(0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), 0L);
    }
}
