package com.knoxhack.echo.scriptcore.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeMigrationEntry;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeMigrationReport;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeMigrationService;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeSnapshot;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeValue;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloader;
import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import com.echoplatform.echocore.api.IDataView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ScriptCoreRuntimeMigrationService implements EchoScriptRuntimeMigrationService {
    public static final ScriptCoreRuntimeMigrationService INSTANCE = new ScriptCoreRuntimeMigrationService();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ScriptCoreRuntimeMigrationService() {
    }

    @Override
    public boolean available() {
        return ScriptCoreRuntimeStateService.INSTANCE.available();
    }

    @Override
    public String backendName() {
        return ScriptCoreRuntimeStateService.INSTANCE.backendName();
    }

    @Override
    public EchoScriptRuntimeSnapshot snapshotPlayer(ServerPlayer player) {
        return snapshot(player == null ? null : EchoCoreServices.playerData(player),
                player == null ? "unknown-player" : player.getScoreboardName(), DataScope.PLAYER);
    }

    @Override
    public EchoScriptRuntimeSnapshot snapshotWorld(Level level) {
        String owner = level == null || level.dimension() == null ? "unknown-world" : level.dimension().identifier().toString();
        return snapshot(level == null ? null : EchoCoreServices.worldData(level), owner, DataScope.WORLD);
    }

    @Override
    public EchoScriptRuntimeMigrationReport previewPlayer(ServerPlayer player, String from, String to) {
        return migrate(player == null ? null : EchoCoreServices.playerData(player), DataScope.PLAYER, from, to, false);
    }

    @Override
    public EchoScriptRuntimeMigrationReport applyPlayer(ServerPlayer player, String from, String to) {
        return migrate(player == null ? null : EchoCoreServices.playerData(player), DataScope.PLAYER, from, to, true);
    }

    @Override
    public EchoScriptRuntimeMigrationReport previewWorld(Level level, String from, String to) {
        return migrate(level == null ? null : EchoCoreServices.worldData(level), DataScope.WORLD, from, to, false);
    }

    @Override
    public EchoScriptRuntimeMigrationReport applyWorld(Level level, String from, String to) {
        return migrate(level == null ? null : EchoCoreServices.worldData(level), DataScope.WORLD, from, to, true);
    }

    @Override
    public Path exportSnapshot(EchoScriptRuntimeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        Path root = EchoScriptReloader.scriptsRoot()
                .resolve(".runtime_snapshots")
                .toAbsolutePath()
                .normalize();
        String owner = safePath(snapshot.owner()).replace('/', '_');
        Path target = root.resolve("scriptcore_runtime_" + owner + "_" + Instant.now().toEpochMilli() + ".json")
                .toAbsolutePath()
                .normalize();
        if (!target.startsWith(root)) {
            return null;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(toJson(snapshot)));
            return target;
        } catch (IOException exception) {
            EchoScriptCore.LOGGER.warn("ScriptCore runtime snapshot export failed.", exception);
            return null;
        }
    }

    private EchoScriptRuntimeSnapshot snapshot(IDataView view, String owner, DataScope scope) {
        if (!available() || view == null) {
            return new EchoScriptRuntimeSnapshot(false, backendName(), owner, List.of());
        }
        List<EchoScriptRuntimeValue> values = runtimeEntries(view, scope).entrySet().stream()
                .filter(entry -> runtimeKind(entry.getKey(), scope).isPresent())
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .map(entry -> new EchoScriptRuntimeValue(entry.getKey(), scope.name(), entry.getValue()))
                .toList();
        return new EchoScriptRuntimeSnapshot(true, backendName(), owner, values);
    }

    private EchoScriptRuntimeMigrationReport migrate(
            IDataView view, DataScope scope, String from, String to, boolean apply) {
        if (!available()) {
            return unsupported("DataCore runtime storage is unavailable.");
        }
        if (view == null) {
            return unsupported("No " + scope + " runtime data view is available.");
        }
        String fromToken = safePath(from);
        String toToken = safePath(to);
        if (fromToken.isBlank() || toToken.isBlank() || "unknown".equals(fromToken) || "unknown".equals(toToken)) {
            return unsupported("Runtime migration requires non-empty from/to keys.");
        }
        List<EchoScriptRuntimeMigrationEntry> entries = runtimeEntries(view, scope).entrySet().stream()
                .filter(entry -> runtimeKind(entry.getKey(), scope).isPresent())
                .map(entry -> migrationEntry(view, scope, entry.getKey(), entry.getValue(), fromToken, toToken, apply))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        int copied = (int) entries.stream().filter(EchoScriptRuntimeMigrationEntry::copied).count();
        int skipped = Math.max(0, entries.size() - copied);
        List<EchoScriptDiagnostic> diagnostics = entries.isEmpty()
                ? List.of(EchoScriptDiagnostic.info("SCRIPTCORE_RUNTIME_MIGRATION_EMPTY",
                        "No ScriptCore runtime keys matched '" + from + "'."))
                : List.of(EchoScriptDiagnostic.info("SCRIPTCORE_RUNTIME_MIGRATION_READY",
                        (apply ? "Applied" : "Previewed") + " ScriptCore runtime migration from '"
                                + from + "' to '" + to + "'."));
        return new EchoScriptRuntimeMigrationReport(true, apply, entries.size(), copied, skipped, entries, diagnostics);
    }

    private static Map<Identifier, String> runtimeEntries(IDataView view, DataScope scope) {
        Map<Identifier, String> entries = new LinkedHashMap<>();
        if (view == null || scope == null) {
            return entries;
        }
        entries.putAll(view.debugSnapshot());
        for (IDataKey<?> key : EchoCoreServices.dataService().registeredKeys()) {
            if (key == null || key.scope() != scope
                    || runtimeKind(key.id(), scope).isEmpty()
                    || entries.containsKey(key.id())) {
                continue;
            }
            if (view.has(key)) {
                entries.put(key.id(), debugValue(view, key));
            }
        }
        return entries;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String debugValue(IDataView view, IDataKey key) {
        Object value = view.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Optional<EchoScriptRuntimeMigrationEntry> migrationEntry(
            IDataView view, DataScope scope, Identifier fromKey, String debugValue,
            String fromToken, String toToken, boolean apply) {
        String path = fromKey.getPath();
        if (!path.contains(fromToken)) {
            return Optional.empty();
        }
        Identifier toKey = EchoScriptCore.id(path.replace(fromToken, toToken));
        if (fromKey.equals(toKey)) {
            return Optional.empty();
        }
        RuntimeKind kind = runtimeKind(fromKey, scope).orElse(null);
        if (kind == null) {
            return Optional.empty();
        }
        boolean copied = apply && copy(view, kind, scope, fromKey, toKey);
        String note = apply
                ? copied ? "copied; source preserved" : "copy failed; source preserved"
                : "preview; source will be preserved";
        return Optional.of(new EchoScriptRuntimeMigrationEntry(fromKey, toKey, scope.name(), debugValue, copied, note));
    }

    private static boolean copy(IDataView view, RuntimeKind kind, DataScope scope, Identifier fromKey, Identifier toKey) {
        return switch (kind) {
            case FLAG -> {
                boolean value = view.get(IDataKey.flag(fromKey, scope, false, true));
                yield view.set(IDataKey.flag(toKey, scope, false, true), value) || view.has(IDataKey.flag(toKey, scope, false, true));
            }
            case COUNTER -> {
                long value = view.get(IDataKey.counter(fromKey, scope, 0L, true));
                yield view.set(IDataKey.counter(toKey, scope, 0L, true), value) || view.has(IDataKey.counter(toKey, scope, 0L, true));
            }
        };
    }

    private static Optional<RuntimeKind> runtimeKind(Identifier key, DataScope scope) {
        if (key == null || !EchoScriptCore.MODID.equals(key.getNamespace()) || scope == null) {
            return Optional.empty();
        }
        String path = key.getPath();
        if (scope == DataScope.WORLD && path.startsWith("world_state/")) {
            return Optional.of(RuntimeKind.FLAG);
        }
        if (scope == DataScope.PLAYER && path.startsWith("faction_reputation/")) {
            return Optional.of(RuntimeKind.COUNTER);
        }
        if (scope == DataScope.PLAYER && path.startsWith("custom_metric/")) {
            return Optional.of(RuntimeKind.COUNTER);
        }
        if (scope == DataScope.PLAYER && path.startsWith("branch/")) {
            return Optional.of(RuntimeKind.FLAG);
        }
        return Optional.empty();
    }

    private static EchoScriptRuntimeMigrationReport unsupported(String message) {
        return new EchoScriptRuntimeMigrationReport(false, false, 0, 0, 0, List.of(), List.of(new EchoScriptDiagnostic(
                EchoScriptDiagnostic.Severity.WARNING,
                "SCRIPTCORE_RUNTIME_MIGRATION_UNAVAILABLE",
                message,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Install/enable DataCore and run the command from a server context."))));
    }

    private static JsonObject toJson(EchoScriptRuntimeSnapshot snapshot) {
        JsonObject object = new JsonObject();
        object.addProperty("available", snapshot.available());
        object.addProperty("backend", snapshot.backend());
        object.addProperty("owner", snapshot.owner());
        JsonArray values = new JsonArray();
        for (EchoScriptRuntimeValue value : snapshot.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("key", value.key() == null ? "" : value.key().toString());
            entry.addProperty("scope", value.scope());
            entry.addProperty("value", value.value());
            values.add(entry);
        }
        object.add("values", values);
        return object;
    }

    private static String safePath(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace(':', '/')
                .replaceAll("[^a-z0-9_./-]", "_")
                .replaceAll("/+", "/");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    private enum RuntimeKind {
        FLAG,
        COUNTER
    }
}
