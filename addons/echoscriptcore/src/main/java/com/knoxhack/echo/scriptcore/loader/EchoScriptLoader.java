package com.knoxhack.echo.scriptcore.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptLoadResult;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

public final class EchoScriptLoader {
    public static final EchoScriptLoader INSTANCE = new EchoScriptLoader();

    private final EchoScriptParser parser = new EchoScriptParser();

    private EchoScriptLoader() {
    }

    public EchoScriptLoadResult load(Path scriptsRoot) {
        return load(scriptsRoot, path -> true);
    }

    public EchoScriptLoadResult loadPack(Path scriptsRoot, String pack) {
        String normalizedPack = pack == null ? "" : pack.trim();
        return load(scriptsRoot, path -> belongsToPack(scriptsRoot, path, normalizedPack));
    }

    private EchoScriptLoadResult load(Path scriptsRoot, Predicate<Path> fileFilter) {
        long started = System.currentTimeMillis();
        List<EchoScriptDiagnostic> diagnostics = new ArrayList<>();
        List<Path> loadedFiles = new ArrayList<>();
        List<Path> failedFiles = new ArrayList<>();
        Map<Identifier, EchoScriptDefinitionView> definitions = new LinkedHashMap<>();
        Path root = scriptsRoot.toAbsolutePath().normalize();

        if (!Files.isDirectory(root)) {
            return result(started, definitions, diagnostics, loadedFiles, failedFiles);
        }

        List<Path> files = discover(root, diagnostics, fileFilter);
        int maxFiles = ScriptCoreConfig.integer(ScriptCoreConfig.MAX_FILES_PER_RELOAD, 5000);
        int maxBytes = ScriptCoreConfig.integer(ScriptCoreConfig.MAX_FILE_SIZE_KB, 512) * 1024;
        int accepted = 0;
        for (Path file : files) {
            if (accepted >= maxFiles) {
                diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, "SCRIPTCORE_FILE_LIMIT",
                        "ScriptCore reached max_files_per_reload before reading " + root.relativize(file) + ".",
                        file, null, "$", "Raise max_files_per_reload or split inactive scripts outside config/echo/scripts."));
                failedFiles.add(file);
                continue;
            }
            Path normalized = file.toAbsolutePath().normalize();
            if (!normalized.startsWith(root)) {
                diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, "SCRIPTCORE_UNSAFE_PATH",
                        "Rejected unsafe script path outside config/echo/scripts: " + normalized,
                        file, null, "$", "Move the file under config/echo/scripts."));
                failedFiles.add(file);
                continue;
            }
            try {
                if (Files.size(normalized) > maxBytes) {
                    diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, "SCRIPTCORE_FILE_TOO_LARGE",
                            "Script file is larger than max_file_size_kb: " + root.relativize(normalized),
                            normalized, null, "$", "Reduce the file size or raise max_file_size_kb."));
                    failedFiles.add(file);
                    continue;
                }
                EchoScriptDefinitionView definition = parseOne(root, normalized);
                EchoScriptDefinitionView previous = definitions.putIfAbsent(definition.id(), definition);
                if (previous != null) {
                    diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, "SCRIPTCORE_DUPLICATE_ID",
                            "Duplicate ScriptCore definition id " + definition.id() + " in " + root.relativize(normalized)
                                    + "; first definition kept from " + previous.sourceFile().map(Path::toString).orElse("unknown") + ".",
                            normalized, definition.id(), "$.id", "Give each definition a unique namespaced id."));
                    failedFiles.add(file);
                    continue;
                }
                accepted++;
                loadedFiles.add(file);
            } catch (RuntimeException | IOException exception) {
                String code = classify(exception);
                diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, code,
                        "Could not parse ScriptCore JSON " + root.relativize(normalized) + ": " + exception.getMessage(),
                        normalized, null, "$", suggestion(code)));
                failedFiles.add(file);
            }
        }
        return result(started, definitions, diagnostics, loadedFiles, failedFiles);
    }

    private List<Path> discover(Path root, List<EchoScriptDiagnostic> diagnostics, Predicate<Path> fileFilter) {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> path.toAbsolutePath().normalize())
                    .filter(path -> fileFilter == null || fileFilter.test(path))
                    .filter(path -> {
                        boolean safe = path.startsWith(root);
                        if (!safe) {
                            diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, "SCRIPTCORE_UNSAFE_PATH",
                                    "Rejected unsafe script path outside config/echo/scripts: " + path,
                                    path, null, "$", "Move the file under config/echo/scripts."));
                        }
                        return safe;
                    })
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            diagnostics.add(diagnostic(EchoScriptDiagnostic.Severity.ERROR, "SCRIPTCORE_JSON_PARSE_ERROR",
                    "Could not discover ScriptCore files: " + exception.getMessage(),
                    root, null, "$", "Check filesystem permissions for config/echo/scripts."));
            return List.of();
        }
    }

    private EchoScriptDefinitionView parseOne(Path root, Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new IllegalArgumentException("Root must be a JSON object.");
            }
            JsonObject json = rootElement.getAsJsonObject();
            if (!json.has("schema_version") && !json.has("schemaVersion")) {
                throw new IllegalArgumentException("Missing required field schema_version.");
            }
            if (!json.has("id")) {
                throw new IllegalArgumentException("Missing required field id.");
            }
            if (!json.has("type")) {
                throw new IllegalArgumentException("Missing required field type.");
            }
            Identifier id = Identifier.tryParse(json.get("id").getAsString());
            if (id == null) {
                throw new IllegalArgumentException("Invalid id field: " + json.get("id").getAsString());
            }
            return parser.parse(json, file, inferPack(root, file, json));
        }
    }

    private static boolean belongsToPack(Path scriptsRoot, Path file, String pack) {
        if (pack == null || pack.isBlank()) {
            return true;
        }
        Path root = scriptsRoot.toAbsolutePath().normalize();
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            return false;
        }
        Path relative = root.relativize(normalized);
        if (relative.getNameCount() == 1) {
            return "unknown".equals(pack);
        }
        String first = relative.getName(0).toString();
        if ("global".equals(first)) {
            return "global".equals(pack);
        }
        if ("examples".equals(first) && relative.getNameCount() > 1) {
            return relative.getName(1).toString().equals(pack);
        }
        return first.equals(pack);
    }

    private static String classify(Exception exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.startsWith("Missing required field")) {
            return "SCRIPTCORE_MISSING_REQUIRED_FIELD";
        }
        if (message.startsWith("Invalid id field") || message.startsWith("Invalid or missing id")) {
            return "SCRIPTCORE_INVALID_ID";
        }
        return "SCRIPTCORE_JSON_PARSE_ERROR";
    }

    private static String suggestion(String code) {
        return switch (code) {
            case "SCRIPTCORE_MISSING_REQUIRED_FIELD" -> "Add schema_version, id, and type to the JSON definition.";
            case "SCRIPTCORE_INVALID_ID" -> "Use a valid namespaced id such as \"my_pack:definition_id\".";
            default -> "Fix the JSON syntax and required fields.";
        };
    }

    private static String inferPack(Path root, Path file, JsonObject json) {
        String pack = json.has("pack") && json.get("pack").isJsonPrimitive() ? json.get("pack").getAsString() : "";
        if (!pack.isBlank()) {
            return pack;
        }
        Path relative = root.relativize(file);
        if (relative.getNameCount() == 1) {
            return "unknown";
        }
        String first = relative.getName(0).toString();
        if ("global".equals(first)) {
            return "global";
        }
        if ("examples".equals(first) && relative.getNameCount() > 1) {
            return relative.getName(1).toString();
        }
        return first;
    }

    private static EchoScriptLoadResult result(
            long started,
            Map<Identifier, EchoScriptDefinitionView> definitions,
            List<EchoScriptDiagnostic> diagnostics,
            List<Path> loadedFiles,
            List<Path> failedFiles) {
        int warnings = (int) diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.WARNING).count();
        int errors = (int) diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR).count();
        return new EchoScriptLoadResult(
                definitions.size(),
                failedFiles.size(),
                warnings,
                errors,
                List.copyOf(definitions.values()),
                diagnostics,
                loadedFiles,
                failedFiles,
                System.currentTimeMillis() - started);
    }

    private static EchoScriptDiagnostic diagnostic(
            EchoScriptDiagnostic.Severity severity,
            String code,
            String message,
            Path file,
            Identifier definitionId,
            String jsonPath,
            String suggestion) {
        return new EchoScriptDiagnostic(
                severity,
                code,
                message,
                java.util.Optional.ofNullable(file),
                java.util.Optional.ofNullable(definitionId),
                java.util.Optional.ofNullable(jsonPath),
                java.util.Optional.ofNullable(suggestion));
    }
}
