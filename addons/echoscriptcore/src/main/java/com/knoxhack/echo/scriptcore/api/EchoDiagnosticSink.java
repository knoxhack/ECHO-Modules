package com.knoxhack.echo.scriptcore.api;

@FunctionalInterface
public interface EchoDiagnosticSink {
    void report(EchoScriptDiagnostic diagnostic);
}
