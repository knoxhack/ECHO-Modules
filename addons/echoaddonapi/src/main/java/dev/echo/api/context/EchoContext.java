package dev.echo.api.context;

import dev.echo.api.diagnostics.EchoDiagnosticSink;
import dev.echo.api.platform.EchoPlatform;
import dev.echo.api.platform.EchoRuntimeSide;

public interface EchoContext {
    EchoPlatform platform();

    EchoRuntimeSide side();

    EchoDiagnosticSink diagnostics();
}
