package com.knoxhack.echo.creatorcore.draft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class CreatorDraftStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Path root() {
        return resolveRoot(CreatorCoreConfig.string(CreatorCoreConfig.DRAFT_ROOT, "config/echo/creatorcore/drafts"));
    }

    public List<CreatorDraft> loadDrafts() {
        Path root = root();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<CreatorDraft> drafts = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .limit(CreatorCoreConfig.integer(CreatorCoreConfig.MAX_DRAFTS, 500))
                    .forEach(path -> read(path).ifPresent(drafts::add));
        } catch (IOException exception) {
            drafts.add(errorDraft("creatorcore:store_error", "draft_store",
                    "Could not read draft store: " + exception.getMessage()));
        }
        return List.copyOf(drafts);
    }

    public Optional<CreatorDraft> read(Path path) {
        try {
            Path safePath = path.toAbsolutePath().normalize();
            if (!safePath.startsWith(root().toAbsolutePath().normalize())) {
                return Optional.empty();
            }
            long maxBytes = CreatorCoreConfig.integer(CreatorCoreConfig.MAX_DRAFT_FILE_SIZE_KB, 512) * 1024L;
            if (Files.size(safePath) > maxBytes) {
                return Optional.of(errorDraft("creatorcore:oversized", "draft_store",
                        "Draft file exceeds max size: " + safePath));
            }
            JsonObject json = JsonParser.parseString(Files.readString(safePath, StandardCharsets.UTF_8)).getAsJsonObject();
            return Optional.of(fromJson(json, safePath));
        } catch (RuntimeException | IOException exception) {
            return Optional.of(errorDraft("creatorcore:invalid_json", "draft_store",
                    "Invalid draft file " + path + ": " + exception.getMessage()));
        }
    }

    public Path save(CreatorDraft draft) throws IOException {
        Path path = draftPath(draft.pack(), draft.type(), draft.id());
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(toJson(draft)), StandardCharsets.UTF_8);
        return path;
    }

    public boolean delete(CreatorDraft draft) throws IOException {
        Path path = draftPath(draft.pack(), draft.type(), draft.id());
        return Files.deleteIfExists(path);
    }

    public Path draftPath(String pack, String type, Identifier id) throws IOException {
        if (id == null) {
            throw new IOException("Draft id is required.");
        }
        Path base = root().toAbsolutePath().normalize();
        Path target = base.resolve(segment(pack, "default"))
                .resolve(segment(type, "unknown"))
                .resolve(segment(id.getNamespace(), "minecraft"))
                .resolve(id.getPath() + ".json")
                .normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Refusing unsafe draft path: " + target);
        }
        return target;
    }

    public JsonObject toJson(CreatorDraft draft) {
        JsonObject root = new JsonObject();
        root.addProperty("id", draft.id().toString());
        root.addProperty("type", draft.type());
        root.addProperty("pack", draft.pack());
        root.addProperty("title", draft.title());
        root.addProperty("sourceAdapter", draft.sourceAdapter());
        root.addProperty("createdAt", draft.createdAt().toString());
        root.addProperty("updatedAt", draft.updatedAt().toString());
        root.addProperty("createdBy", draft.createdBy());
        root.addProperty("status", draft.status().name());
        root.add("content", draft.content());
        return root;
    }

    private CreatorDraft fromJson(JsonObject json, Path file) {
        Identifier id = Identifier.tryParse(string(json, "id", "creatorcore:invalid"));
        String type = string(json, "type", "unknown");
        String pack = string(json, "pack", "default");
        String title = string(json, "title", id == null ? file.getFileName().toString() : id.toString());
        JsonObject content = json.has("content") && json.get("content").isJsonObject()
                ? json.getAsJsonObject("content") : json;
        Instant createdAt = instant(string(json, "createdAt", Instant.now().toString()));
        Instant updatedAt = instant(string(json, "updatedAt", createdAt.toString()));
        CreatorDraft.DraftStatus status = status(string(json, "status", "NEW"));
        return new CreatorDraft(id == null ? Identifier.fromNamespaceAndPath("creatorcore", "invalid") : id,
                type, pack, title, content, string(json, "sourceAdapter", "file"),
                createdAt, updatedAt, string(json, "createdBy", "file"), List.of(), status);
    }

    private static Path resolveRoot(String configured) {
        Path path = Path.of(configured);
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    private static String segment(String value, String fallback) {
        String safe = value == null || value.isBlank() ? fallback : value;
        return safe.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private static Instant instant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return Instant.now();
        }
    }

    private static CreatorDraft.DraftStatus status(String value) {
        try {
            return CreatorDraft.DraftStatus.valueOf(value);
        } catch (RuntimeException exception) {
            return CreatorDraft.DraftStatus.NEW;
        }
    }

    private static CreatorDraft errorDraft(String id, String type, String message) {
        JsonObject content = new JsonObject();
        content.addProperty("error", message);
        return new CreatorDraft(Identifier.parse(id), type, "internal", "Draft store error",
                content, "draft_store", Instant.now(), Instant.now(), "system",
                List.of(CreatorDiagnostic.error("creatorcore.draft_store", message, "Draft Store", "Check the draft file JSON.")),
                CreatorDraft.DraftStatus.ERROR);
    }
}
