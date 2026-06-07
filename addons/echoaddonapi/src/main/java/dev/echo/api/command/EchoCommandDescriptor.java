package dev.echo.api.command;

import java.util.List;

public record EchoCommandDescriptor(
        String id,
        String literal,
        EchoCommandPermission permission,
        List<String> aliases
) {
    public EchoCommandDescriptor {
        aliases = List.copyOf(aliases);
    }
}
