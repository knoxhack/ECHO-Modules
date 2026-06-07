package com.knoxhack.echo.creatorcore.api;

import java.util.List;

public record CreatorExportResult(
        boolean success,
        String targetPath,
        List<CreatorDiagnostic> diagnostics,
        String message,
        int exportedCount) {
    public CreatorExportResult {
        targetPath = targetPath == null ? "" : targetPath;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        message = message == null ? "" : message;
    }

    public static CreatorExportResult failed(String message, String targetPath) {
        return new CreatorExportResult(false, targetPath, List.of(), message, 0);
    }

    public static CreatorExportResult success(String message, String targetPath, int exportedCount) {
        return new CreatorExportResult(true, targetPath, List.of(), message, exportedCount);
    }
}
