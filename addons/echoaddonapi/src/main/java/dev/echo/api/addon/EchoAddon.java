package dev.echo.api.addon;

import dev.echo.api.context.EchoContext;
import dev.echo.api.context.EchoRegistryContext;
import dev.echo.api.lifecycle.EchoLifecyclePhase;
import dev.echo.api.lifecycle.EchoLifecycleResult;

public interface EchoAddon {
    EchoAddonDescriptor descriptor();

    default void register(EchoRegistryContext context) {
    }

    default EchoLifecycleResult onLifecycle(EchoLifecyclePhase phase, EchoContext context) {
        return EchoLifecycleResult.pass();
    }
}
