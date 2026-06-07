package com.knoxhack.echo.scriptcore.authoring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import com.knoxhack.echo.scriptcore.loader.EchoScriptParser;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloader;
import com.knoxhack.echo.scriptcore.validation.EchoScriptValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoScriptAuthoringService {
    public static final EchoScriptAuthoringService INSTANCE = new EchoScriptAuthoringService();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final EchoScriptParser parser = new EchoScriptParser();

    private EchoScriptAuthoringService() {
    }

    public boolean createDraftDefinition(String type, Identifier id) {
        if (!ScriptCoreConfig.draftWritesAllowed() || id == null) {
            return false;
        }
        JsonObject draft = new JsonObject();
        draft.addProperty("schema_version", 1);
        draft.addProperty("pack", id.getNamespace());
        draft.addProperty("id", id.toString());
        draft.addProperty("type", type == null || type.isBlank() ? "generic" : type);
        draft.addProperty("title", readable(id.getPath()));
        return saveDraft(id, draft);
    }

    public boolean saveDraft(Identifier id, JsonObject json) {
        if (!ScriptCoreConfig.draftWritesAllowed() || id == null) {
            return false;
        }
        Path file = draftFile(id);
        try {
            Files.createDirectories(file.getParent());
            if (json == null && Files.exists(file)) {
                return true;
            }
            Files.writeString(file, GSON.toJson(json == null ? new JsonObject() : json));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public List<EchoScriptDiagnostic> validateDraft(Identifier id) {
        Path file = draftFile(id);
        if (!Files.exists(file)) {
            return List.of(new EchoScriptDiagnostic(
                    EchoScriptDiagnostic.Severity.ERROR,
                    "SCRIPTCORE_JSON_PARSE_ERROR",
                    "Draft does not exist: " + id,
                    java.util.Optional.of(file),
                    java.util.Optional.ofNullable(id),
                    java.util.Optional.of("$"),
                    java.util.Optional.of("Create the draft first.")));
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            var definition = parser.parse(json, file, id.getNamespace());
            return EchoScriptValidator.INSTANCE.validate(List.of(definition));
        } catch (RuntimeException | IOException exception) {
            return List.of(new EchoScriptDiagnostic(
                    EchoScriptDiagnostic.Severity.ERROR,
                    "SCRIPTCORE_JSON_PARSE_ERROR",
                    "Draft " + id + " is invalid: " + exception.getMessage(),
                    java.util.Optional.of(file),
                    java.util.Optional.of(id),
                    java.util.Optional.of("$"),
                    java.util.Optional.of("Fix the draft JSON before exporting.")));
        }
    }

    public boolean exportDefinition(Identifier id, Path targetFile) {
        if (!ScriptCoreConfig.draftWritesAllowed() || id == null || targetFile == null) {
            return false;
        }
        Path source = draftFile(id);
        Path target = targetFile.toAbsolutePath().normalize();
        Path scriptsRoot = EchoScriptReloader.scriptsRoot();
        if (!target.startsWith(scriptsRoot)) {
            return false;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public boolean saveDraftToScripts(Identifier id) {
        if (!ScriptCoreConfig.draftWritesAllowed() || id == null) {
            return false;
        }
        Optional<EchoScriptDefinitionView> definition = readDraftDefinition(id);
        if (definition.isEmpty()) {
            return false;
        }
        List<EchoScriptDiagnostic> diagnostics = EchoScriptValidator.INSTANCE.validate(List.of(definition.get()));
        if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == EchoScriptDiagnostic.Severity.ERROR)) {
            return false;
        }
        Path target = scriptFile(definition.get());
        return exportDefinition(id, target);
    }

    public boolean deleteDraft(Identifier id) {
        if (!ScriptCoreConfig.draftWritesAllowed() || id == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(draftFile(id));
        } catch (IOException exception) {
            return false;
        }
    }

    public List<Identifier> listDrafts() {
        Path dir = draftsRoot();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", "").replace("__", ":"))
                    .map(Identifier::tryParse)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    public JsonObject getSchemaForType(String type) {
        JsonObject schema = new JsonObject();
        schema.addProperty("schema_version", 1);
        schema.addProperty("type", type == null || type.isBlank() ? "generic" : type);
        schema.addProperty("required", "schema_version,id,type");
        return schema;
    }

    private static Path draftFile(Identifier id) {
        return draftsRoot().resolve(id.toString().replace(':', '_').replace('/', '_') + ".json").toAbsolutePath().normalize();
    }

    private Optional<EchoScriptDefinitionView> readDraftDefinition(Identifier id) {
        Path file = draftFile(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return Optional.of(parser.parse(json, file, id.getNamespace()));
        } catch (RuntimeException | IOException exception) {
            return Optional.empty();
        }
    }

    private static Path scriptFile(EchoScriptDefinitionView definition) {
        String pack = definition.pack() == null || definition.pack().isBlank() || "unknown".equals(definition.pack())
                ? definition.id().getNamespace()
                : definition.pack();
        String folder = folderForType(definition.type());
        return EchoScriptReloader.scriptsRoot()
                .resolve(pack)
                .resolve(folder)
                .resolve(definition.id().getPath() + ".json")
                .toAbsolutePath()
                .normalize();
    }

    private static String folderForType(String type) {
        return switch (type == null ? "generic" : type) {
            case "mission" -> "missions";
            case "archive_entry", "archive", "lore" -> "archive";
            case "lens_scan" -> "lens";
            case "holomap_layer", "holomap_marker" -> "holomap";
            case "weather_event" -> "weather";
            case "faction" -> "factions";
            case "world_state" -> "world_state";
            case "tutorial_hint" -> "tutorials";
            case "dialogue" -> "dialogue";
            case "ending" -> "endings";
            case "recipe_unlock" -> "recipes";
            case "loot_profile" -> "loot";
            default -> "generic";
        };
    }

    private static Path draftsRoot() {
        return EchoScriptReloader.scriptsRoot().resolve(".drafts").toAbsolutePath().normalize();
    }

    private static String readable(String path) {
        StringBuilder builder = new StringBuilder();
        for (String part : path.replace('/', '_').split("_")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.length() > 1 ? part.substring(1) : "");
        }
        return builder.isEmpty() ? path : builder.toString();
    }
}
