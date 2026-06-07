package com.knoxhack.echo.creatorcore.adapter;

import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorExportResult;
import com.knoxhack.echo.creatorcore.api.CreatorFormField;
import com.knoxhack.echo.creatorcore.api.CreatorFormFieldKind;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.draft.CreatorDraftTemplateFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class ScriptCoreCreatorAdapter extends ModPresenceCreatorAdapter {
    private static final String API_CLASS = "com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi";
    private static final String AUTHORING_CLASS = "com.knoxhack.echo.scriptcore.authoring.EchoScriptAuthoringService";
    private static final Set<String> KNOWN_DRAFT_TYPES = Set.of(
            "mission",
            "archive_entry",
            "lens_scan",
            "holomap_layer",
            "holomap_marker",
            "weather_event",
            "faction",
            "world_state",
            "tutorial_hint",
            "dialogue",
            "ending",
            "recipe_unlock",
            "loot_profile",
            "generic");

    public ScriptCoreCreatorAdapter() {
        super("scriptcore", "echoscriptcore", "ECHO: ScriptCore",
                API_CLASS,
                Set.of("definitions", "diagnostics", "drafts", "templates", "export", "reload"),
                "ScriptCore not installed or API unavailable; CreatorCore is running in dashboard-only mode.",
                "ScriptCore API detected; CreatorCore can read definitions, diagnostics, and request reloads.",
                false);
    }

    @Override
    public String status() {
        if (!detectedMod()) {
            return "ScriptCore not installed. CreatorCore is running in dashboard/draft foundation mode.";
        }
        if (!apiWired()) {
            return "ScriptCore detected, but EchoScriptCoreApi is unavailable. CreatorCore is using dashboard-only mode.";
        }
        int definitions = listDefinitions().size();
        int diagnostics = scriptDiagnostics().size();
        return "ScriptCore bridge ready: " + definitions + " definition(s), " + diagnostics + " diagnostic(s).";
    }

    @Override
    public List<CreatorDefinitionSummary> listDefinitions() {
        if (!isAvailable()) {
            return List.of();
        }
        try {
            List<CreatorDefinitionSummary> summaries = new ArrayList<>();
            for (Object definition : iterable(invoke(invoke(api(), "registry"), "all"))) {
                Identifier id = value(definition, "id", Identifier.class).orElse(EchoCreatorCore.id("scriptcore_unknown"));
                String type = value(definition, "type", String.class).orElse("unknown");
                String title = optionalString(invoke(definition, "title")).orElse(id.toString());
                String pack = value(definition, "pack", String.class).orElse("runtime");
                summaries.add(new CreatorDefinitionSummary(id, type, title, id().toString(), pack, "scriptcore"));
            }
            return List.copyOf(summaries);
        } catch (RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore could not read ScriptCore definitions.", exception);
            return List.of();
        }
    }

    @Override
    public Optional<CreatorDefinitionDetail> definitionDetail(Identifier id) {
        if (!isAvailable() || id == null) {
            return Optional.empty();
        }
        try {
            Object registry = invoke(api(), "registry");
            Object optional = invoke(registry, "get", new Class<?>[] {Identifier.class}, id);
            if (optional instanceof Optional<?> found && found.isPresent()) {
                return Optional.of(toDefinitionDetail(found.get()));
            }
        } catch (RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore could not read ScriptCore definition detail for {}.", id, exception);
        }
        return Optional.empty();
    }

    @Override
    public List<CreatorDiagnostic> diagnostics() {
        if (!detectedMod()) {
            return List.of(CreatorDiagnostic.warning("creatorcore.scriptcore.missing",
                    "ScriptCore is missing. CreatorCore is running in dashboard/draft foundation mode.",
                    displayName(), "Install ScriptCore to read live ECHO definitions and diagnostics."));
        }
        if (!apiWired()) {
            return List.of(CreatorDiagnostic.warning("creatorcore.scriptcore.api_missing",
                    "ScriptCore is installed, but EchoScriptCoreApi is unavailable.",
                    displayName(), "Update ScriptCore or keep using CreatorCore's dashboard-only draft foundation."));
        }
        return scriptDiagnostics();
    }

    @Override
    public boolean supportsDraftType(String type) {
        return isAvailable() && type != null && KNOWN_DRAFT_TYPES.contains(type);
    }

    @Override
    public Optional<CreatorDraft> createDraft(String type, Identifier id) {
        if (!supportsDraftType(type) || id == null) {
            return Optional.empty();
        }
        try {
            Object authoring = authoringService();
            Object created = invoke(authoring, "createDraftDefinition",
                    new Class<?>[] {String.class, Identifier.class}, type, id);
            if (created instanceof Boolean ok && ok) {
                return Optional.of(CreatorDraftTemplateFactory.create(type, id, id.getNamespace(), "scriptcore"));
            }
        } catch (RuntimeException exception) {
            EchoCreatorCore.LOGGER.debug("CreatorCore ScriptCore draft creation hook failed for {}.", id, exception);
        }
        return Optional.empty();
    }

    @Override
    public List<CreatorFormSchema> formSchemas() {
        if (!detectedMod()) {
            return List.of();
        }
        return List.of(
                schema("mission", "Mission", "ScriptCore mission draft.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:repair_radio"),
                        CreatorFormField.text("title", "Title", true, "Repair the Radio"),
                        CreatorFormField.select("role", "Role", false, List.of("main", "optional", "hidden", "repeatable")),
                        json("objectives", "Objectives", true, "[{\"id\":\"collect\",\"type\":\"collect_item\"}]"),
                        json("rewards", "Rewards", false, "[{\"type\":\"noop\"}]"),
                        json("conditions", "Conditions", false, "[{\"type\":\"always\"}]"),
                        json("actions", "Actions", false, "[{\"type\":\"noop\"}]")),
                schema("archive_entry", "Archive Entry", "Readable lore/archive entry.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:first_signal"),
                        CreatorFormField.text("category", "Category", false, "lore"),
                        CreatorFormField.text("title", "Title", true, "First Signal"),
                        json("content", "Content Lines", true, "[\"Write archive text here.\"]"),
                        json("unlock_conditions", "Unlock Conditions", false, "[{\"type\":\"always\"}]")),
                schema("lens_scan", "Lens Scan", "Lens scan result definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:broken_generator"),
                        CreatorFormField.text("target", "Target", true, "minecraft:stone"),
                        CreatorFormField.select("target_type", "Target Type", true, List.of("block", "entity", "item", "fluid", "poi", "region", "custom")),
                        CreatorFormField.text("summary", "Summary", false, "Scan summary"),
                        json("details", "Details", false, "[\"Detail line\"]")),
                schema("holomap_layer", "HoloMap Layer", "HoloMap layer definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:hazards"),
                        CreatorFormField.text("title", "Title", true, "Hazards"),
                        json("markers", "Inline Markers", false, "[]")),
                schema("holomap_marker", "HoloMap Marker", "HoloMap marker definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:shelter"),
                        CreatorFormField.text("dimension", "Dimension", true, "minecraft:overworld"),
                        CreatorFormField.text("layer", "Layer", false, "example:hazards"),
                        field("x", "X", CreatorFormFieldKind.NUMBER, true, "0"),
                        field("y", "Y", CreatorFormFieldKind.NUMBER, false, "64"),
                        field("z", "Z", CreatorFormFieldKind.NUMBER, true, "0")),
                schema("weather_event", "Weather Event", "Weather event definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:storm"),
                        field("duration_ticks", "Duration Ticks", CreatorFormFieldKind.NUMBER, true, "6000"),
                        field("warning_seconds", "Warning Seconds", CreatorFormFieldKind.NUMBER, false, "10"),
                        json("effects", "Effects", false, "[{\"type\":\"noop\"}]")),
                schema("faction", "Faction", "Faction and reputation draft.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:settlers"),
                        CreatorFormField.text("display_name", "Display Name", true, "Settlers"),
                        field("starting_reputation", "Starting Reputation", CreatorFormFieldKind.NUMBER, false, "0"),
                        json("ranks", "Ranks", true, "[{\"name\":\"Neutral\",\"min\":0}]")),
                schema("world_state", "World State", "World state flag definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:radio_restored"),
                        json("set_by", "Set By", false, "[{\"type\":\"always\"}]"),
                        json("effects", "Effects", false, "[{\"type\":\"noop\"}]")),
                schema("tutorial_hint", "Tutorial Hint", "Tutorial hint definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:first_warning"),
                        CreatorFormField.text("message", "Message", true, "Hint message."),
                        field("priority", "Priority", CreatorFormFieldKind.NUMBER, false, "0"),
                        json("trigger_conditions", "Trigger Conditions", false, "[{\"type\":\"always\"}]")),
                schema("dialogue", "Dialogue", "Branching dialogue definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:intro"),
                        CreatorFormField.text("speaker", "Speaker", true, "Guide"),
                        json("lines", "Lines", true, "[\"Dialogue line\"]"),
                        json("choices", "Choices", false, "[{\"id\":\"continue\",\"label\":\"Continue\"}]")),
                schema("ending", "Ending", "Ending resolver definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:safe_haven"),
                        CreatorFormField.text("description", "Description", true, "Ending description."),
                        field("priority", "Priority", CreatorFormFieldKind.NUMBER, false, "0"),
                        json("conditions", "Conditions", true, "[{\"type\":\"always\"}]")),
                schema("recipe_unlock", "Recipe Unlock", "Recipe unlock definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:bread_unlock"),
                        CreatorFormField.text("recipe", "Recipe", true, "minecraft:bread"),
                        json("unlock_conditions", "Unlock Conditions", false, "[{\"type\":\"always\"}]")),
                schema("loot_profile", "Loot Profile", "Loot profile definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:starter_loot"),
                        CreatorFormField.text("table", "Loot Table", true, "minecraft:chests/simple_dungeon"),
                        json("entries", "Entries", false, "[]")),
                schema("generic", "Generic", "Generic ScriptCore definition.",
                        field("id", "Id", CreatorFormFieldKind.RESOURCE_LOCATION, true, "example:custom"),
                        CreatorFormField.text("title", "Title", false, "Custom Definition"),
                        json("conditions", "Conditions", false, "[{\"type\":\"always\"}]"),
                        json("actions", "Actions", false, "[{\"type\":\"noop\"}]")));
    }

    @Override
    public CreatorExportResult exportDraft(CreatorDraft draft, Path targetPath) {
        if (!isAvailable() || draft == null || draft.id() == null || !authoringWired()) {
            return CreatorExportResult.failed("ScriptCore authoring service is unavailable.", targetPath == null ? "" : targetPath.toString());
        }
        try {
            Object authoring = authoringService();
            Object saved = invoke(authoring, "saveDraft",
                    new Class<?>[] {Identifier.class, JsonObject.class}, draft.id(), draft.content());
            if (!(saved instanceof Boolean ok) || !ok) {
                return CreatorExportResult.failed(
                        "ScriptCore refused the draft save. Check ScriptCore draft write settings.",
                        targetPath == null ? "" : targetPath.toString());
            }
            List<CreatorDiagnostic> diagnostics = toCreatorDiagnostics(invoke(authoring, "validateDraft",
                    new Class<?>[] {Identifier.class}, draft.id()));
            boolean hasErrors = diagnostics.stream()
                    .anyMatch(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR);
            if (hasErrors) {
                return new CreatorExportResult(false, targetPath == null ? "" : targetPath.toString(),
                        diagnostics, "ScriptCore validation blocked export.", 0);
            }
            Object exported = targetPath == null
                    ? invoke(authoring, "saveDraftToScripts", new Class<?>[] {Identifier.class}, draft.id())
                    : invoke(authoring, "exportDefinition",
                            new Class<?>[] {Identifier.class, Path.class}, draft.id(), targetPath);
            if (exported instanceof Boolean exportedOk && exportedOk) {
                String target = targetPath == null ? "ScriptCore scripts root" : targetPath.toString();
                return new CreatorExportResult(true, target, diagnostics,
                        "ScriptCore exported draft " + draft.id() + ".", 1);
            }
            return new CreatorExportResult(false, targetPath == null ? "" : targetPath.toString(), diagnostics,
                    "ScriptCore export hook returned false. Check ScriptCore write settings and export root.", 0);
        } catch (RuntimeException exception) {
            return CreatorExportResult.failed("ScriptCore export failed: " + exception.getMessage(),
                    targetPath == null ? "" : targetPath.toString());
        }
    }

    @Override
    public void reload() {
        if (!isAvailable()) {
            return;
        }
        try {
            invoke(api(), "reloadAll");
        } catch (RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore could not request ScriptCore reload.", exception);
        }
    }

    public boolean reloadPack(String pack) {
        if (!isAvailable() || pack == null || pack.isBlank()) {
            return false;
        }
        try {
            invoke(api(), "reloadPack", new Class<?>[] {String.class}, pack);
            return true;
        } catch (RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore could not request ScriptCore pack reload for {}.", pack, exception);
            return false;
        }
    }

    public boolean reloadType(String type) {
        if (!isAvailable() || type == null || type.isBlank()) {
            return false;
        }
        try {
            invoke(api(), "reloadType", new Class<?>[] {String.class}, type);
            return true;
        } catch (RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore could not request ScriptCore type reload for {}.", type, exception);
            return false;
        }
    }

    public List<CreatorDiagnostic> validateDraft(Identifier id) {
        if (!isAvailable() || id == null || !authoringWired()) {
            return List.of(CreatorDiagnostic.warning("creatorcore.scriptcore.authoring_unavailable",
                    "ScriptCore authoring service is unavailable.", displayName(),
                    "Install/enable ScriptCore and check authoring settings."));
        }
        return toCreatorDiagnostics(invoke(authoringService(), "validateDraft", new Class<?>[] {Identifier.class}, id));
    }

    @Override
    public JsonObject debugInfo() {
        JsonObject object = super.debugInfo();
        object.addProperty("definitionCount", isAvailable() ? listDefinitions().size() : 0);
        object.addProperty("diagnosticCount", isAvailable() ? scriptDiagnostics().size() : 0);
        object.addProperty("authoringClass", AUTHORING_CLASS);
        object.addProperty("authoringWired", authoringWired());
        return object;
    }

    private List<CreatorDiagnostic> scriptDiagnostics() {
        try {
            return toCreatorDiagnostics(invoke(api(), "validateAll"));
        } catch (RuntimeException exception) {
            return List.of(CreatorDiagnostic.warning("creatorcore.scriptcore.diagnostics_unavailable",
                    "CreatorCore could not read ScriptCore diagnostics: " + exception.getMessage(),
                    displayName(), "Check the ScriptCore log and reload the pack."));
        }
    }

    private CreatorDiagnostic toCreatorDiagnostic(Object diagnostic) {
        CreatorDiagnostic.Severity severity = severity(value(invoke(diagnostic, "severity"), Object.class)
                .map(Object::toString)
                .orElse("INFO"));
        String code = value(diagnostic, "code", String.class).orElse("SCRIPTCORE_INFO");
        String message = value(diagnostic, "message", String.class).orElse("No ScriptCore diagnostic message.");
        Optional<Identifier> definitionId = nestedOptional(invoke(diagnostic, "definitionId"), Identifier.class);
        Optional<String> file = optionalToString(invoke(diagnostic, "file"));
        Optional<String> jsonPath = nestedOptional(invoke(diagnostic, "jsonPath"), String.class);
        Optional<String> suggestion = nestedOptional(invoke(diagnostic, "suggestion"), String.class);
        return new CreatorDiagnostic(severity, code, message, displayName(), definitionId, file, jsonPath,
                suggestion, false, Optional.of(id()));
    }

    private CreatorDefinitionDetail toDefinitionDetail(Object definition) {
        Identifier definitionId = value(definition, "id", Identifier.class).orElse(EchoCreatorCore.id("scriptcore_unknown"));
        String type = value(definition, "type", String.class).orElse("unknown");
        String title = optionalString(invoke(definition, "title")).orElse(definitionId.toString());
        String description = optionalString(invoke(definition, "description")).orElse("");
        String pack = value(definition, "pack", String.class).orElse(definitionId.getNamespace());
        Optional<String> sourceFile = nestedOptional(invoke(definition, "sourceFile"), Path.class).map(Path::toString);
        List<String> tags = strings(iterable(invoke(definition, "tags")));
        JsonObject rawJson = value(invoke(definition, "rawJson"), JsonObject.class)
                .map(JsonObject::deepCopy)
                .orElseGet(JsonObject::new);
        Map<String, String> metadata = stringMap(invoke(definition, "metadata"));
        List<CreatorDiagnostic> diagnostics = scriptDiagnostics().stream()
                .filter(diagnostic -> diagnostic.definitionId().filter(definitionId::equals).isPresent())
                .toList();
        List<String> preview = new ArrayList<>();
        preview.add("ScriptCore type: " + type);
        preview.add("Pack: " + pack);
        optionalString(invoke(definition, "source")).ifPresent(source -> preview.add("Source: " + source));
        if (!tags.isEmpty()) {
            preview.add("Tags: " + String.join(", ", tags));
        }
        preview.add("Raw JSON fields: " + rawJson.entrySet().size());
        return new CreatorDefinitionDetail(definitionId, type, title, description, id().toString(), pack,
                diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR)
                        ? "error" : "scriptcore",
                sourceFile, tags, rawJson, metadata, diagnostics, preview, true);
    }

    private List<CreatorDiagnostic> toCreatorDiagnostics(Object diagnosticsValue) {
        List<CreatorDiagnostic> diagnostics = new ArrayList<>();
        for (Object diagnostic : iterable(diagnosticsValue)) {
            diagnostics.add(toCreatorDiagnostic(diagnostic));
        }
        return List.copyOf(diagnostics);
    }

    private static CreatorFormSchema schema(String type, String title, String description, CreatorFormField... fields) {
        return new CreatorFormSchema(type, "ScriptCore " + title, description, List.of(fields), false);
    }

    private static CreatorFormField field(String name, String label, CreatorFormFieldKind kind, boolean required, String placeholder) {
        return new CreatorFormField(name, label, kind, required, List.of(), placeholder, false);
    }

    private static CreatorFormField json(String name, String label, boolean required, String placeholder) {
        return new CreatorFormField(name, label, CreatorFormFieldKind.JSON, required, List.of(), placeholder, false);
    }

    private Object api() {
        return invoke(load(API_CLASS), "get");
    }

    private Object authoringService() {
        try {
            Field field = load(AUTHORING_CLASS).getField("INSTANCE");
            return field.get(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("ScriptCore authoring service is unavailable.", exception);
        }
    }

    private boolean authoringWired() {
        try {
            authoringService();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            throw new IllegalStateException("Class unavailable: " + className, exception);
        }
    }

    private static Object invoke(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on null target.");
        }
        try {
            Method method = target instanceof Class<?> clazz
                    ? clazz.getMethod(methodName, parameterTypes)
                    : target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target instanceof Class<?> ? null : target, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause()
                    : exception;
            throw new IllegalStateException("ScriptCore reflection call failed: " + methodName, cause);
        }
    }

    private static List<Object> iterable(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            return values;
        }
        return List.of();
    }

    private static Optional<String> optionalString(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.filter(String.class::isInstance).map(String.class::cast);
        }
        return Optional.empty();
    }

    private static List<String> strings(List<Object> values) {
        return values.stream().map(Object::toString).filter(value -> !value.isBlank()).toList();
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> strings = new LinkedHashMap<>();
        map.forEach((key, entry) -> {
            if (key != null && entry != null) {
                strings.put(key.toString(), entry.toString());
            }
        });
        return Map.copyOf(strings);
    }

    private static <T> Optional<T> nestedOptional(Object value, Class<T> type) {
        if (value instanceof Optional<?> optional) {
            return optional.filter(type::isInstance).map(type::cast);
        }
        return Optional.empty();
    }

    private static Optional<String> optionalToString(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(Object::toString);
        }
        return Optional.empty();
    }

    private static <T> Optional<T> value(Object target, String methodName, Class<T> type) {
        return value(invoke(target, methodName), type);
    }

    private static <T> Optional<T> value(Object value, Class<T> type) {
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    private static CreatorDiagnostic.Severity severity(String name) {
        return switch (name) {
            case "ERROR" -> CreatorDiagnostic.Severity.ERROR;
            case "WARNING" -> CreatorDiagnostic.Severity.WARNING;
            default -> CreatorDiagnostic.Severity.INFO;
        };
    }
}
