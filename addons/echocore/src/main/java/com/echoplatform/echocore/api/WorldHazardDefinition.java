package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record WorldHazardDefinition(
        Identifier id,
        String displayName,
        String summary,
        int defaultSeverity,
        boolean ticking) {
    public WorldHazardDefinition {
        displayName = displayName == null ? "" : displayName;
        summary = summary == null ? "" : summary;
        defaultSeverity = Math.max(0, defaultSeverity);
    }
}
