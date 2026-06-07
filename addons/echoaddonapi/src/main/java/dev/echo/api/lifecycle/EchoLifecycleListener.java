package dev.echo.api.lifecycle;

import dev.echo.api.context.EchoContext;

@FunctionalInterface
public interface EchoLifecycleListener {
    EchoLifecycleResult onLifecycle(EchoLifecyclePhase phase, EchoContext context);
}
