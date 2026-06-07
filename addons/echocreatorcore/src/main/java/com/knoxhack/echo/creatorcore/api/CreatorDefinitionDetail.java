package com.knoxhack.echo.creatorcore.api;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record CreatorDefinitionDetail(
        Identifier id,
        String type,
        String title,
        String description,
        String sourceAdapter,
        String pack,
        String status,
        Optional<String> sourceFile,
        List<String> tags,
        JsonObject rawJson,
        Map<String, String> metadata,
        List<CreatorDiagnostic> diagnostics,
        List<String> previewLines,
        boolean readOnly) {
    public CreatorDefinitionDetail {
        type = safe(type, "unknown");
        title = safe(title, id == null ? "Untitled definition" : id.toString());
        description = description == null ? "" : description;
        sourceAdapter = safe(sourceAdapter, "unknown");
        pack = safe(pack, "runtime");
        status = safe(status, "available");
        sourceFile = sourceFile == null ? Optional.empty() : sourceFile;
        tags = List.copyOf(tags == null ? List.of() : tags);
        rawJson = rawJson == null ? new JsonObject() : rawJson.deepCopy();
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        previewLines = List.copyOf(previewLines == null ? List.of() : previewLines);
    }

    public static CreatorDefinitionDetail fromSummary(CreatorDefinitionSummary summary, List<String> previewLines) {
        return new CreatorDefinitionDetail(
                summary == null ? null : summary.id(),
                summary == null ? "unknown" : summary.type(),
                summary == null ? "Untitled definition" : summary.title(),
                "",
                summary == null ? "unknown" : summary.sourceAdapter(),
                summary == null ? "runtime" : summary.pack(),
                summary == null ? "unknown" : summary.status(),
                Optional.empty(),
                List.of(),
                new JsonObject(),
                Map.of(),
                List.of(),
                previewLines,
                true);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

