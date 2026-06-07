package com.knoxhack.echo.npcore.conversion;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record EchoNpcReplacementMapping(
        Identifier id,
        Map<Identifier, Identifier> replace,
        Map<Identifier, Identifier> entityTypes) {
    public EchoNpcReplacementMapping {
        replace = Map.copyOf(replace == null ? Map.of() : replace);
        entityTypes = Map.copyOf(entityTypes == null ? Map.of() : entityTypes);
    }
}
