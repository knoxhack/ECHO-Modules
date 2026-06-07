package com.knoxhack.echo.scriptcore.api;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoScriptDiagnostic(
        Severity severity,
        String code,
        String message,
        Optional<Path> file,
        Optional<Identifier> definitionId,
        Optional<String> jsonPath,
        Optional<String> suggestion) {
    public EchoScriptDiagnostic {
        severity = severity == null ? Severity.INFO : severity;
        code = code == null || code.isBlank() ? "SCRIPTCORE_INFO" : code;
        message = message == null ? "" : message;
        file = file == null ? Optional.empty() : file;
        definitionId = definitionId == null ? Optional.empty() : definitionId;
        jsonPath = jsonPath == null ? Optional.empty() : jsonPath;
        suggestion = suggestion == null ? Optional.empty() : suggestion;
    }

    public static EchoScriptDiagnostic info(String code, String message) {
        return new EchoScriptDiagnostic(Severity.INFO, code, message, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
