package com.knoxhack.echo.creatorcore.api;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public interface CreatorAdapter {
    Identifier id();

    String displayName();

    boolean isAvailable();

    String status();

    Set<String> capabilities();

    default List<CreatorDefinitionSummary> listDefinitions() {
        return List.of();
    }

    default Optional<CreatorDefinitionDetail> definitionDetail(Identifier id) {
        return Optional.empty();
    }

    default List<CreatorPreviewSummary> previewSummaries() {
        return List.of();
    }

    default List<CreatorFormSchema> formSchemas() {
        return List.of();
    }

    default List<CreatorDiagnostic> diagnostics() {
        return List.of();
    }

    default Optional<CreatorPanelProvider> panelProvider() {
        return Optional.empty();
    }

    default boolean supportsDraftType(String type) {
        return false;
    }

    default Optional<CreatorDraft> createDraft(String type, Identifier id) {
        return Optional.empty();
    }

    default CreatorExportResult exportDraft(CreatorDraft draft) {
        return CreatorExportResult.failed("Adapter does not export drafts.", "");
    }

    default CreatorExportResult exportDraft(CreatorDraft draft, Path targetPath) {
        return exportDraft(draft);
    }

    default void reload() {
    }

    default JsonObject debugInfo() {
        JsonObject object = new JsonObject();
        object.addProperty("id", id().toString());
        object.addProperty("displayName", displayName());
        object.addProperty("available", isAvailable());
        object.addProperty("status", status());
        object.addProperty("capabilities", String.join(",", capabilities()));
        return object;
    }
}
