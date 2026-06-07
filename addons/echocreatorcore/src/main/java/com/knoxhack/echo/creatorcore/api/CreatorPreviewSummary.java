package com.knoxhack.echo.creatorcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record CreatorPreviewSummary(
        Identifier id,
        String type,
        String title,
        String sourceAdapter,
        String scope,
        List<String> lines,
        boolean readOnly) {
    public CreatorPreviewSummary {
        type = safe(type, "preview");
        title = safe(title, id == null ? "Preview" : id.toString());
        sourceAdapter = safe(sourceAdapter, "unknown");
        scope = safe(scope, "runtime");
        lines = List.copyOf(lines == null ? List.of() : lines);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

