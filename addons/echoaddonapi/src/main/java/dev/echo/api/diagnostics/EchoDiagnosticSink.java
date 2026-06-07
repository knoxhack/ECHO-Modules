package dev.echo.api.diagnostics;

public interface EchoDiagnosticSink {
    void emit(EchoDiagnostic diagnostic);

    default void info(String code, String message) {
        emit(new EchoDiagnostic(EchoDiagnosticSeverity.INFO, code, message, java.util.Map.of()));
    }

    default void warning(String code, String message) {
        emit(new EchoDiagnostic(EchoDiagnosticSeverity.WARNING, code, message, java.util.Map.of()));
    }

    default void error(String code, String message) {
        emit(new EchoDiagnostic(EchoDiagnosticSeverity.ERROR, code, message, java.util.Map.of()));
    }
}
