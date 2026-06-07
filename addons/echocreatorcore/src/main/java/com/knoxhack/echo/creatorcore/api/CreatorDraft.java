package com.knoxhack.echo.creatorcore.api;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;
import net.minecraft.resources.Identifier;

public record CreatorDraft(
        Identifier id,
        String type,
        String pack,
        String title,
        JsonObject content,
        String sourceAdapter,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        List<CreatorDiagnostic> diagnostics,
        DraftStatus status) {
    public CreatorDraft {
        type = safe(type, "unknown");
        pack = safe(pack, "default");
        title = safe(title, id == null ? "Untitled draft" : id.toString());
        content = content == null ? new JsonObject() : content;
        sourceAdapter = safe(sourceAdapter, "internal");
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        createdBy = safe(createdBy, "system");
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        status = status == null ? DraftStatus.NEW : status;
    }

    public CreatorDraft withDiagnostics(List<CreatorDiagnostic> diagnostics, DraftStatus status) {
        return new CreatorDraft(id, type, pack, title, content, sourceAdapter, createdAt, Instant.now(),
                createdBy, diagnostics, status);
    }

    public CreatorDraft asExported() {
        return new CreatorDraft(id, type, pack, title, content, sourceAdapter, createdAt, Instant.now(),
                createdBy, diagnostics, DraftStatus.EXPORTED);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public enum DraftStatus {
        NEW,
        VALID,
        WARNING,
        ERROR,
        EXPORTED
    }
}
