package com.knoxhack.echo.npcore.faction;

import net.minecraft.resources.Identifier;

public record EchoNpcFactionDefinition(Identifier id, String displayName, String shortName, String theme) {
    public EchoNpcFactionDefinition {
        displayName = displayName == null || displayName.isBlank() ? id.getPath() : displayName.trim();
        shortName = shortName == null || shortName.isBlank() ? displayName : shortName.trim();
        theme = theme == null ? "" : theme.trim();
    }
}
