package dev.echo.api.block;

import dev.echo.api.context.EchoPlayerContext;
import dev.echo.api.context.EchoWorldContext;

public record EchoBlockUseContext(EchoPlayerContext player, EchoWorldContext world, EchoBlockStateView state) {
}
