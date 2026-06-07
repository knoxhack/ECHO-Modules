package dev.echo.api.platform;

public interface EchoPlatform {
    EchoRuntimeKind runtimeKind();

    EchoRuntimeSide side();

    EchoCapabilitySet capabilities();

    String version();
}
