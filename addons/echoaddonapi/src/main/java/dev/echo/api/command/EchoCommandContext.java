package dev.echo.api.command;

import dev.echo.api.context.EchoContext;
import java.util.List;
import java.util.Map;

public record EchoCommandContext(EchoContext context, String sourceId, List<String> arguments, Map<String, String> namedArguments) {
    public EchoCommandContext {
        arguments = List.copyOf(arguments);
        namedArguments = Map.copyOf(namedArguments);
    }
}
