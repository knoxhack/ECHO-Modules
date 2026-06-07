package com.knoxhack.echo.scriptcore.loader;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptLoadResult;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import com.knoxhack.echo.scriptcore.examples.EchoScriptExampleGenerator;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echo.scriptcore.validation.EchoScriptValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class EchoScriptReloader {
    public static final EchoScriptReloader INSTANCE = new EchoScriptReloader();

    private volatile EchoScriptLoadResult lastResult = EchoScriptLoadResult.empty();

    private EchoScriptReloader() {
    }

    public EchoScriptLoadResult reloadAll() {
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.ENABLED, true)) {
            lastResult = EchoScriptLoadResult.empty();
            EchoScriptCore.LOGGER.info("ScriptCore reload skipped because the addon is disabled.");
            return lastResult;
        }
        Path root = scriptsRoot();
        EchoScriptExampleGenerator.generateIfEnabled(root);
        EchoScriptLoadResult parsed = EchoScriptLoader.INSTANCE.load(root);
        List<EchoScriptDiagnostic> diagnostics = new ArrayList<>(parsed.diagnostics());
        diagnostics.addAll(EchoScriptValidator.INSTANCE.validate(parsed.definitions()));
        boolean hasErrors = diagnostics.stream().anyMatch(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR);
        if (hasErrors && ScriptCoreConfig.bool(ScriptCoreConfig.FAIL_PACK_ON_ERROR, false)) {
            lastResult = withDiagnostics(parsed, diagnostics, parsed.definitions(), false);
            EchoScriptCore.LOGGER.warn("ScriptCore reload rejected with {} error(s); previous registry remains active.", lastResult.errorCount());
            return lastResult;
        }

        List<EchoScriptDefinitionView> accepted = hasErrors
                ? parsed.definitions().stream().filter(definition -> !hasFatalDiagnostic(definition, diagnostics)).toList()
                : parsed.definitions();
        EchoScriptRegistry.INSTANCE.replaceAll(accepted);
        EchoScriptAdapterRegistry.INSTANCE.registerDefinitions(EchoScriptRegistry.INSTANCE, diagnostics::add);
        lastResult = withDiagnostics(parsed, diagnostics, accepted, true);
        EchoScriptCore.LOGGER.info("ScriptCore reloaded {} definition(s) from {} file(s) in {}ms: errors={}, warnings={}.",
                accepted.size(), parsed.loadedFiles().size(), lastResult.durationMs(), lastResult.errorCount(), lastResult.warningCount());
        if (ScriptCoreConfig.bool(ScriptCoreConfig.LOG_LOADED_DEFINITIONS, true)) {
            accepted.stream().limit(64).forEach(definition ->
                    EchoScriptCore.LOGGER.info("ScriptCore loaded {} {} ({})", definition.type(), definition.id(), definition.pack()));
        }
        return lastResult;
    }

    public EchoScriptLoadResult reloadPack(String pack) {
        return reloadPack(scriptsRoot(), pack);
    }

    public EchoScriptLoadResult reloadType(String type) {
        return reloadType(scriptsRoot(), type);
    }

    public EchoScriptLoadResult reloadPack(Path root, String pack) {
        String normalizedPack = pack == null ? "" : pack.trim();
        if (normalizedPack.isBlank()) {
            return reloadAll();
        }
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.ENABLED, true)) {
            lastResult = EchoScriptLoadResult.empty();
            return lastResult;
        }
        EchoScriptExampleGenerator.generateIfEnabled(root);
        EchoScriptLoadResult parsed = EchoScriptLoader.INSTANCE.loadPack(root, normalizedPack);
        return applyScoped(parsed, definition -> normalizedPack.equals(definition.pack()), "pack " + normalizedPack);
    }

    public EchoScriptLoadResult reloadType(Path root, String type) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedType.isBlank()) {
            return reloadAll();
        }
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.ENABLED, true)) {
            lastResult = EchoScriptLoadResult.empty();
            return lastResult;
        }
        EchoScriptExampleGenerator.generateIfEnabled(root);
        EchoScriptLoadResult parsed = EchoScriptLoader.INSTANCE.load(root);
        return applyScoped(parsed, definition -> normalizedType.equals(definition.type()), "type " + normalizedType);
    }

    public EchoScriptLoadResult lastResult() {
        return lastResult;
    }

    public static Path scriptsRoot() {
        return Path.of("config").resolve("echo").resolve("scripts").toAbsolutePath().normalize();
    }

    private static boolean hasFatalDiagnostic(EchoScriptDefinitionView definition, List<EchoScriptDiagnostic> diagnostics) {
        Set<String> globalFatalCodes = Set.of("SCRIPTCORE_DUPLICATE_ID", "SCRIPTCORE_JSON_PARSE_ERROR", "SCRIPTCORE_FILE_TOO_LARGE", "SCRIPTCORE_UNSAFE_PATH", "SCRIPTCORE_INVALID_ID");
        Set<String> fatalCodes = new HashSet<>(globalFatalCodes);
        fatalCodes.add("SCRIPTCORE_MISSING_REQUIRED_FIELD");
        fatalCodes.add("SCRIPTCORE_MISSING_ID");
        fatalCodes.add("SCRIPTCORE_MISSING_TYPE");
        fatalCodes.add("SCRIPTCORE_UNKNOWN_TYPE");
        fatalCodes.add("SCRIPTCORE_INVALID_OBJECTIVE");
        fatalCodes.add("SCRIPTCORE_INVALID_REWARD");
        fatalCodes.add("SCRIPTCORE_INVALID_WEATHER_DURATION");
        fatalCodes.add("SCRIPTCORE_INVALID_FACTION_RANKS");
        fatalCodes.add("SCRIPTCORE_CIRCULAR_PREREQUISITE");
        fatalCodes.add("SCRIPTCORE_BROKEN_MISSION_REFERENCE");
        return diagnostics.stream()
                .filter(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR)
                .filter(d -> fatalCodes.contains(d.code()))
                .anyMatch(d -> d.definitionId().isPresent() && d.definitionId().get().equals(definition.id()));
    }

    private EchoScriptLoadResult applyScoped(EchoScriptLoadResult parsed, Predicate<EchoScriptDefinitionView> scope, String label) {
        List<EchoScriptDefinitionView> parsedScoped = parsed.definitions().stream()
                .filter(scope)
                .toList();
        List<EchoScriptDefinitionView> merged = new ArrayList<>();
        for (EchoScriptDefinitionView existing : EchoScriptRegistry.INSTANCE.all()) {
            if (!scope.test(existing)) {
                merged.add(existing);
            }
        }
        merged.addAll(parsedScoped);

        List<EchoScriptDiagnostic> diagnostics = new ArrayList<>(parsed.diagnostics());
        diagnostics.addAll(EchoScriptValidator.INSTANCE.validate(merged));
        boolean hasErrors = diagnostics.stream().anyMatch(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR);
        if (hasErrors && ScriptCoreConfig.bool(ScriptCoreConfig.FAIL_PACK_ON_ERROR, false)) {
            lastResult = scopedResult(parsed, diagnostics, EchoScriptRegistry.INSTANCE.all(), parsed.failedCount(), false);
            EchoScriptCore.LOGGER.warn("ScriptCore {} reload rejected with {} error(s); previous registry remains active.", label, lastResult.errorCount());
            return lastResult;
        }

        List<EchoScriptDefinitionView> accepted = hasErrors
                ? merged.stream().filter(definition -> !hasFatalDiagnostic(definition, diagnostics)).toList()
                : merged;
        int dropped = Math.max(0, merged.size() - accepted.size());
        EchoScriptRegistry.INSTANCE.replaceAll(accepted);
        EchoScriptAdapterRegistry.INSTANCE.registerDefinitions(EchoScriptRegistry.INSTANCE, diagnostics::add);
        lastResult = scopedResult(parsed, diagnostics, accepted, parsed.failedCount() + dropped, true);
        EchoScriptCore.LOGGER.info("ScriptCore {} reloaded. Registry now has {} definition(s): errors={}, warnings={}.",
                label, accepted.size(), lastResult.errorCount(), lastResult.warningCount());
        return lastResult;
    }

    private static EchoScriptLoadResult scopedResult(
            EchoScriptLoadResult parsed,
            List<EchoScriptDiagnostic> diagnostics,
            List<EchoScriptDefinitionView> definitions,
            int failedCount,
            boolean applied) {
        int warnings = (int) diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.WARNING).count();
        int errors = (int) diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR).count();
        int loaded = applied ? definitions.size() : 0;
        return new EchoScriptLoadResult(
                loaded,
                failedCount,
                warnings,
                errors,
                definitions,
                diagnostics,
                parsed.loadedFiles(),
                parsed.failedFiles(),
                parsed.durationMs());
    }

    private static EchoScriptLoadResult withDiagnostics(
            EchoScriptLoadResult parsed,
            List<EchoScriptDiagnostic> diagnostics,
            List<EchoScriptDefinitionView> definitions,
            boolean applied) {
        int warnings = (int) diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.WARNING).count();
        int errors = (int) diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR).count();
        int failed = parsed.failedCount() + (applied ? Math.max(0, parsed.definitions().size() - definitions.size()) : parsed.definitions().size());
        return new EchoScriptLoadResult(
                definitions.size(),
                failed,
                warnings,
                errors,
                definitions,
                diagnostics,
                parsed.loadedFiles(),
                parsed.failedFiles(),
                parsed.durationMs());
    }
}
