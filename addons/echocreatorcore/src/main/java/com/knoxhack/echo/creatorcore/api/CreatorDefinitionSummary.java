package com.knoxhack.echo.creatorcore.api;

import net.minecraft.resources.Identifier;

public record CreatorDefinitionSummary(
        Identifier id,
        String type,
        String title,
        String sourceAdapter,
        String pack,
        String status) {
    public CreatorDefinitionSummary {
        type = safe(type, "unknown");
        title = safe(title, id == null ? "Untitled" : id.toString());
        sourceAdapter = safe(sourceAdapter, "unknown");
        pack = safe(pack, "runtime");
        status = safe(status, "unknown");
    }

    public static CreatorDefinitionSummary of(Identifier id, String type, String title, String adapter) {
        return new CreatorDefinitionSummary(id, type, title, adapter, "runtime", "available");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
