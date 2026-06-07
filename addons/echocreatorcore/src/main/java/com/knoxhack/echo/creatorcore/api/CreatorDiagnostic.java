package com.knoxhack.echo.creatorcore.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;

public record CreatorDiagnostic(
        Severity severity,
        String code,
        String message,
        String source,
        Optional<Identifier> definitionId,
        Optional<String> file,
        Optional<String> jsonPath,
        Optional<String> suggestion,
        boolean fixable,
        Optional<Identifier> relatedAdapter) {
    public CreatorDiagnostic {
        severity = severity == null ? Severity.INFO : severity;
        code = safe(code, "creatorcore.info");
        message = safe(message, "No diagnostic message.");
        source = safe(source, "CreatorCore");
        definitionId = definitionId == null ? Optional.empty() : definitionId;
        file = file == null ? Optional.empty() : file;
        jsonPath = jsonPath == null ? Optional.empty() : jsonPath;
        suggestion = suggestion == null ? Optional.empty() : suggestion;
        relatedAdapter = relatedAdapter == null ? Optional.empty() : relatedAdapter;
    }

    public static CreatorDiagnostic info(String code, String message, String source) {
        return new CreatorDiagnostic(Severity.INFO, code, message, source,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.empty());
    }

    public static CreatorDiagnostic warning(String code, String message, String source, String suggestion) {
        return new CreatorDiagnostic(Severity.WARNING, code, message, source,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(suggestion), false, Optional.empty());
    }

    public static CreatorDiagnostic error(String code, String message, String source, String suggestion) {
        return new CreatorDiagnostic(Severity.ERROR, code, message, source,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(suggestion), false, Optional.empty());
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
