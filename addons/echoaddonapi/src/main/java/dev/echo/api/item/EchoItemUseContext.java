package dev.echo.api.item;

import dev.echo.api.context.EchoPlayerContext;
import dev.echo.api.context.EchoWorldContext;

public record EchoItemUseContext(EchoPlayerContext player, EchoWorldContext world, EchoItemStackView stack) {
}
