package com.knoxhack.echo.scriptcore.command;

import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnosticsSummary;
import com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi;
import com.knoxhack.echo.scriptcore.api.EchoScriptLoadResult;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeMigrationReport;
import com.knoxhack.echo.scriptcore.api.EchoScriptRuntimeSnapshot;
import com.knoxhack.echo.scriptcore.authoring.EchoScriptAuthoringService;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import com.knoxhack.echo.scriptcore.examples.EchoScriptExampleGenerator;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloader;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreUiExecutionService;
import com.knoxhack.echo.scriptcore.validation.EchoScriptValidator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class EchoScriptCommands {
    private EchoScriptCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal("scriptcore")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("reload").requires(EchoScriptCommands::admin)
                        .executes(ctx -> reload(ctx.getSource()))
                        .then(Commands.literal("pack")
                                .then(Commands.argument("pack", StringArgumentType.word())
                                        .executes(ctx -> reloadPack(ctx.getSource(), StringArgumentType.getString(ctx, "pack")))))
                        .then(Commands.literal("type")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> reloadType(ctx.getSource(), StringArgumentType.getString(ctx, "type"))))))
                .then(Commands.literal("validate").requires(EchoScriptCommands::admin)
                        .executes(ctx -> validate(ctx.getSource(), false))
                        .then(Commands.literal("verbose").executes(ctx -> validate(ctx.getSource(), true))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource()))
                        .then(Commands.literal("type")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> listType(ctx.getSource(), StringArgumentType.getString(ctx, "type")))))
                        .then(Commands.literal("pack")
                                .then(Commands.argument("pack", StringArgumentType.word())
                                        .executes(ctx -> listPack(ctx.getSource(), StringArgumentType.getString(ctx, "pack"))))))
                .then(Commands.literal("show")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> show(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("doctor").requires(EchoScriptCommands::admin).executes(ctx -> doctor(ctx.getSource())))
                .then(Commands.literal("export").requires(EchoScriptCommands::admin)
                        .then(Commands.literal("examples").executes(ctx -> exportExamples(ctx.getSource()))))
                .then(Commands.literal("draft").requires(EchoScriptCommands::admin)
                        .then(Commands.literal("create")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .executes(ctx -> draftCreate(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("validate")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(ctx -> draftValidate(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("save")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(ctx -> draftSave(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                .then(runtimeRoot());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> runtimeRoot() {
        return Commands.literal("runtime").requires(EchoScriptCommands::admin)
                .then(Commands.literal("status").executes(ctx -> runtimeStatus(ctx.getSource())))
                .then(Commands.literal("inspect")
                        .then(Commands.literal("player")
                                .executes(ctx -> runtimeInspectPlayer(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                        .then(Commands.literal("world")
                                .executes(ctx -> runtimeInspectWorld(ctx.getSource()))))
                .then(Commands.literal("migrate")
                        .then(Commands.literal("preview")
                                .then(Commands.literal("player")
                                        .then(Commands.argument("from", StringArgumentType.string())
                                                .then(Commands.argument("to", StringArgumentType.string())
                                                        .executes(ctx -> runtimeMigratePlayer(ctx.getSource(),
                                                                ctx.getSource().getPlayerOrException(),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to"),
                                                                false)))))
                                .then(Commands.literal("world")
                                        .then(Commands.argument("from", StringArgumentType.string())
                                                .then(Commands.argument("to", StringArgumentType.string())
                                                        .executes(ctx -> runtimeMigrateWorld(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to"),
                                                                false))))))
                        .then(Commands.literal("apply")
                                .then(Commands.literal("player")
                                        .then(Commands.argument("from", StringArgumentType.string())
                                                .then(Commands.argument("to", StringArgumentType.string())
                                                        .executes(ctx -> runtimeMigratePlayer(ctx.getSource(),
                                                                ctx.getSource().getPlayerOrException(),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to"),
                                                                true)))))
                                .then(Commands.literal("world")
                                        .then(Commands.argument("from", StringArgumentType.string())
                                                .then(Commands.argument("to", StringArgumentType.string())
                                                        .executes(ctx -> runtimeMigrateWorld(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to"),
                                                                true)))))))
                .then(Commands.literal("export")
                        .then(Commands.literal("snapshot")
                                .then(Commands.literal("player")
                                        .executes(ctx -> runtimeExportPlayer(ctx.getSource(), ctx.getSource().getPlayerOrException())))
                                .then(Commands.literal("world")
                                        .executes(ctx -> runtimeExportWorld(ctx.getSource())))));
    }

    private static boolean admin(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int status(CommandSourceStack source) {
        EchoScriptLoadResult last = EchoScriptReloader.INSTANCE.lastResult();
        tell(source, "ScriptCore by ECHO Labs");
        tell(source, "Enabled: " + ScriptCoreConfig.bool(ScriptCoreConfig.ENABLED, true)
                + ", definitions: " + EchoScriptRegistry.INSTANCE.all().size()
                + ", last reload: " + last.durationMs() + "ms"
                + ", errors: " + last.errorCount()
                + ", warnings: " + last.warningCount());
        tell(source, "Types: " + EchoScriptRegistry.INSTANCE.countByType());
        tell(source, "Packs: " + EchoScriptRegistry.INSTANCE.countByPack().keySet());
        tell(source, "Adapters: " + adapterSummary());
        EchoScriptDiagnosticsSummary summary = EchoScriptCoreApi.get().diagnosticsSummary();
        tell(source, "Runtime storage: " + (summary.runtimeStorageAvailable() ? "available" : "unavailable")
                + " (" + summary.runtimeStorageBackend() + ")");
        tell(source, "ScreenCore UI action bridge: " + ScriptCoreUiExecutionService.INSTANCE.bridgeStatus());
        tell(source, "ScreenCore UI action bridge last rejection: "
                + ScriptCoreUiExecutionService.INSTANCE.lastServerRejectionStatus());
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandSourceStack source) {
        if (!ScriptCoreConfig.bool(ScriptCoreConfig.ALLOW_RUNTIME_RELOAD, true)) {
            tell(source, "ScriptCore runtime reload is disabled in config.");
            return 0;
        }
        EchoScriptLoadResult result = EchoScriptReloader.INSTANCE.reloadAll();
        tell(source, "ScriptCore reload complete: " + result.loadedCount() + " loaded, "
                + result.failedCount() + " failed, " + result.errorCount() + " errors, "
                + result.warningCount() + " warnings.");
        return result.errorCount() == 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reloadPack(CommandSourceStack source, String pack) {
        EchoScriptLoadResult result = EchoScriptReloader.INSTANCE.reloadPack(pack);
        tell(source, "ScriptCore pack reload requested for " + pack + ". Registry now has "
                + EchoScriptRegistry.INSTANCE.getByPack(pack).size() + " matching definition(s).");
        return result.errorCount() == 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reloadType(CommandSourceStack source, String type) {
        EchoScriptLoadResult result = EchoScriptReloader.INSTANCE.reloadType(type);
        tell(source, "ScriptCore type reload requested for " + type + ". Registry now has "
                + EchoScriptRegistry.INSTANCE.getByType(type).size() + " matching definition(s).");
        return result.errorCount() == 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int validate(CommandSourceStack source, boolean verbose) {
        List<EchoScriptDiagnostic> diagnostics = EchoScriptValidator.INSTANCE.validate(EchoScriptRegistry.INSTANCE.all());
        long errors = diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(d -> d.severity() == EchoScriptDiagnostic.Severity.WARNING).count();
        tell(source, "ScriptCore validation: errors=" + errors + ", warnings=" + warnings + ".");
        if (verbose || errors > 0 || warnings > 0) {
            diagnostics.stream().limit(verbose ? 40 : 8).forEach(diagnostic -> tell(source, format(diagnostic)));
        }
        return errors == 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int list(CommandSourceStack source) {
        tell(source, "ScriptCore packs: " + EchoScriptRegistry.INSTANCE.countByPack());
        tell(source, "ScriptCore types: " + EchoScriptRegistry.INSTANCE.countByType());
        return Command.SINGLE_SUCCESS;
    }

    private static int listType(CommandSourceStack source, String type) {
        List<String> ids = EchoScriptRegistry.INSTANCE.getByType(type).stream()
                .map(definition -> definition.id().toString())
                .sorted()
                .toList();
        tell(source, "ScriptCore type " + type + " (" + ids.size() + "): " + ids);
        return Command.SINGLE_SUCCESS;
    }

    private static int listPack(CommandSourceStack source, String pack) {
        List<String> ids = EchoScriptRegistry.INSTANCE.getByPack(pack).stream()
                .map(definition -> definition.type() + ":" + definition.id())
                .sorted()
                .toList();
        tell(source, "ScriptCore pack " + pack + " (" + ids.size() + "): " + ids);
        return Command.SINGLE_SUCCESS;
    }

    private static int show(CommandSourceStack source, String rawId) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            tell(source, "Invalid ScriptCore id: " + rawId);
            return 0;
        }
        EchoScriptDefinitionView definition = EchoScriptRegistry.INSTANCE.get(id).orElse(null);
        if (definition == null) {
            tell(source, "No ScriptCore definition found for " + id + ".");
            return 0;
        }
        tell(source, definition.type() + " " + definition.id() + " [" + definition.pack() + "]");
        tell(source, "Title: " + definition.title().orElse("(untitled)"));
        definition.description().ifPresent(description -> tell(source, "Description: " + description));
        tell(source, "Tags: " + definition.tags() + ", actions=" + definition.actions().size()
                + ", conditions=" + definition.conditions().size());
        tell(source, "Runtime: " + integrationStatus(definition));
        return Command.SINGLE_SUCCESS;
    }

    private static int doctor(CommandSourceStack source) {
        EchoScriptLoadResult last = EchoScriptReloader.INSTANCE.lastResult();
        List<EchoScriptDiagnostic> diagnostics = last.diagnostics().isEmpty()
                ? EchoScriptValidator.INSTANCE.validate(EchoScriptRegistry.INSTANCE.all())
                : last.diagnostics();
        EchoScriptDiagnosticsSummary summary = EchoScriptCoreApi.get().diagnosticsSummary();
        tell(source, "ScriptCore Doctor:");
        tell(source, "- Loaded packs: " + summary.loadedPacks());
        tell(source, "- Definitions: " + summary.definitionCount() + " total");
        tell(source, "- Errors: " + summary.errors());
        tell(source, "- Warnings: " + summary.warnings());
        tell(source, "- Missing adapters: " + summary.missingAdapters());
        tell(source, "- Broken references: " + summary.brokenReferences());
        tell(source, "- Circular mission chains: " + summary.circularMissionChains());
        tell(source, "- Invalid objectives: " + summary.invalidObjectives());
        tell(source, "- Unknown actions: " + summary.unknownActions());
        tell(source, "- Unknown conditions: " + summary.unknownConditions());
        tell(source, "- HoloMap marker issues: " + summary.holomapMarkerIssues());
        tell(source, "- Unreachable archive entries: " + summary.unreachableArchiveEntries());
        tell(source, "- Endings that may never trigger: " + summary.impossibleEndings());
        tell(source, "- Runtime storage: " + (summary.runtimeStorageAvailable() ? "available" : "unavailable")
                + " (" + summary.runtimeStorageBackend() + ")");
        tell(source, "- ScreenCore UI action bridge: " + ScriptCoreUiExecutionService.INSTANCE.bridgeStatus());
        tell(source, "- ScreenCore UI action bridge last rejection: "
                + ScriptCoreUiExecutionService.INSTANCE.lastServerRejectionStatus());
        diagnostics.stream()
                .filter(d -> d.severity() != EchoScriptDiagnostic.Severity.INFO)
                .sorted(Comparator.comparing(EchoScriptDiagnostic::code))
                .limit(12)
                .forEach(diagnostic -> tell(source, "  " + format(diagnostic)));
        return summary.errors() == 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int exportExamples(CommandSourceStack source) {
        EchoScriptExampleGenerator.generateOfficialExamples(EchoScriptReloader.scriptsRoot(), true);
        tell(source, "ScriptCore examples regenerated under " + EchoScriptReloader.scriptsRoot() + ".");
        return Command.SINGLE_SUCCESS;
    }

    private static int draftCreate(CommandSourceStack source, String type, String rawId) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            tell(source, "Invalid draft id: " + rawId);
            return 0;
        }
        boolean ok = EchoScriptAuthoringService.INSTANCE.createDraftDefinition(type, id);
        tell(source, ok ? "Created ScriptCore draft " + id + "." : "Draft creation denied or failed. Check dev_mode and allow_draft_writes.");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int draftValidate(CommandSourceStack source, String rawId) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            tell(source, "Invalid draft id: " + rawId);
            return 0;
        }
        List<EchoScriptDiagnostic> diagnostics = EchoScriptAuthoringService.INSTANCE.validateDraft(id);
        tell(source, "Draft " + id + " diagnostics: " + diagnostics.size());
        diagnostics.stream().limit(8).forEach(diagnostic -> tell(source, format(diagnostic)));
        return diagnostics.stream().noneMatch(d -> d.severity() == EchoScriptDiagnostic.Severity.ERROR) ? Command.SINGLE_SUCCESS : 0;
    }

    private static int draftSave(CommandSourceStack source, String rawId) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            tell(source, "Invalid draft id: " + rawId);
            return 0;
        }
        List<EchoScriptDiagnostic> diagnostics = EchoScriptAuthoringService.INSTANCE.validateDraft(id);
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == EchoScriptDiagnostic.Severity.ERROR)
                .limit(8)
                .forEach(diagnostic -> tell(source, format(diagnostic)));
        boolean ok = diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == EchoScriptDiagnostic.Severity.ERROR)
                && EchoScriptAuthoringService.INSTANCE.saveDraftToScripts(id);
        tell(source, ok ? "Exported ScriptCore draft " + id + " into config/echo/scripts." : "Draft save denied or failed. Check dev_mode, allow_draft_writes, read_only_mode, and draft diagnostics.");
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int runtimeStatus(CommandSourceStack source) {
        var migrations = EchoScriptCoreApi.get().runtimeMigrations();
        tell(source, "ScriptCore runtime storage: " + (migrations.available() ? "available" : "unavailable")
                + " (" + migrations.backendName() + ")");
        tell(source, "Runtime migration apply/export: "
                + (ScriptCoreConfig.runtimeMigrationsAllowed() ? "allowed" : "locked")
                + " (dev_mode or allow_runtime_migrations required; read_only_mode must be false).");
        return migrations.available() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int runtimeInspectPlayer(CommandSourceStack source, ServerPlayer player) {
        EchoScriptRuntimeSnapshot snapshot = EchoScriptCoreApi.get().runtimeMigrations().snapshotPlayer(player);
        tell(source, "ScriptCore player runtime values for " + snapshot.owner() + ": " + snapshot.values().size());
        snapshot.values().stream().limit(16)
                .forEach(value -> tell(source, value.scope() + " " + value.key() + " = " + value.value()));
        return snapshot.available() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int runtimeInspectWorld(CommandSourceStack source) {
        EchoScriptRuntimeSnapshot snapshot = EchoScriptCoreApi.get().runtimeMigrations().snapshotWorld(source.getLevel());
        tell(source, "ScriptCore world runtime values for " + snapshot.owner() + ": " + snapshot.values().size());
        snapshot.values().stream().limit(16)
                .forEach(value -> tell(source, value.scope() + " " + value.key() + " = " + value.value()));
        return snapshot.available() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int runtimeMigratePlayer(
            CommandSourceStack source, ServerPlayer player, String from, String to, boolean apply) {
        if (apply && !ScriptCoreConfig.runtimeMigrationsAllowed()) {
            tell(source, "ScriptCore runtime migration apply is locked. Enable dev_mode or allow_runtime_migrations and keep read_only_mode=false.");
            return 0;
        }
        EchoScriptRuntimeMigrationReport report = apply
                ? EchoScriptCoreApi.get().runtimeMigrations().applyPlayer(player, from, to)
                : EchoScriptCoreApi.get().runtimeMigrations().previewPlayer(player, from, to);
        return reportMigration(source, report, apply);
    }

    private static int runtimeMigrateWorld(CommandSourceStack source, String from, String to, boolean apply) {
        if (apply && !ScriptCoreConfig.runtimeMigrationsAllowed()) {
            tell(source, "ScriptCore runtime migration apply is locked. Enable dev_mode or allow_runtime_migrations and keep read_only_mode=false.");
            return 0;
        }
        EchoScriptRuntimeMigrationReport report = apply
                ? EchoScriptCoreApi.get().runtimeMigrations().applyWorld(source.getLevel(), from, to)
                : EchoScriptCoreApi.get().runtimeMigrations().previewWorld(source.getLevel(), from, to);
        return reportMigration(source, report, apply);
    }

    private static int runtimeExportPlayer(CommandSourceStack source, ServerPlayer player) {
        if (!ScriptCoreConfig.runtimeMigrationsAllowed()) {
            tell(source, "ScriptCore runtime snapshot export is locked. Enable dev_mode or allow_runtime_migrations and keep read_only_mode=false.");
            return 0;
        }
        EchoScriptRuntimeSnapshot snapshot = EchoScriptCoreApi.get().runtimeMigrations().snapshotPlayer(player);
        var path = EchoScriptCoreApi.get().runtimeMigrations().exportSnapshot(snapshot);
        tell(source, path == null ? "ScriptCore runtime snapshot export failed." : "ScriptCore runtime snapshot exported to " + path + ".");
        return path == null ? 0 : Command.SINGLE_SUCCESS;
    }

    private static int runtimeExportWorld(CommandSourceStack source) {
        if (!ScriptCoreConfig.runtimeMigrationsAllowed()) {
            tell(source, "ScriptCore runtime snapshot export is locked. Enable dev_mode or allow_runtime_migrations and keep read_only_mode=false.");
            return 0;
        }
        EchoScriptRuntimeSnapshot snapshot = EchoScriptCoreApi.get().runtimeMigrations().snapshotWorld(source.getLevel());
        var path = EchoScriptCoreApi.get().runtimeMigrations().exportSnapshot(snapshot);
        tell(source, path == null ? "ScriptCore runtime snapshot export failed." : "ScriptCore runtime snapshot exported to " + path + ".");
        return path == null ? 0 : Command.SINGLE_SUCCESS;
    }

    private static int reportMigration(CommandSourceStack source, EchoScriptRuntimeMigrationReport report, boolean apply) {
        tell(source, "ScriptCore runtime migration " + (apply ? "apply" : "preview")
                + ": supported=" + report.supported()
                + ", candidates=" + report.candidates()
                + ", copied=" + report.copied()
                + ", skipped=" + report.skipped() + ".");
        report.entries().stream().limit(16)
                .forEach(entry -> tell(source, entry.scope() + " " + entry.fromKey()
                        + " -> " + entry.toKey() + " [" + entry.note() + "]"));
        report.diagnostics().stream().limit(8).forEach(diagnostic -> tell(source, format(diagnostic)));
        return report.supported() && (!apply || report.copied() == report.candidates()) ? Command.SINGLE_SUCCESS : 0;
    }

    private static String adapterSummary() {
        return EchoScriptAdapterRegistry.INSTANCE.adapters().stream()
                .map(adapter -> adapter.id().getPath() + "=" + (adapter.isAvailable() ? "available" : "stub"))
                .toList()
                .toString();
    }

    private static String missingAdapters() {
        return EchoScriptAdapterRegistry.INSTANCE.adapters().stream()
                .filter(adapter -> !adapter.isAvailable())
                .map(adapter -> adapter.id().getPath())
                .toList()
                .toString();
    }

    private static String integrationStatus(EchoScriptDefinitionView definition) {
        List<String> available = EchoScriptAdapterRegistry.INSTANCE.adapters().stream()
                .filter(adapter -> adapter.supportedDefinitionTypes().contains(definition.type()))
                .map(adapter -> adapter.id().getPath() + (adapter.isAvailable() ? ":available" : ":registered in ScriptCore only"))
                .toList();
        return available.isEmpty() ? "registered in ScriptCore only" : available.toString();
    }

    private static String format(EchoScriptDiagnostic diagnostic) {
        return "[" + diagnostic.severity() + " " + diagnostic.code() + "] "
                + diagnostic.definitionId().map(id -> id + ": ").orElse("")
                + diagnostic.message()
                + diagnostic.suggestion().map(s -> " Suggestion: " + s).orElse("");
    }

    private static void tell(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
