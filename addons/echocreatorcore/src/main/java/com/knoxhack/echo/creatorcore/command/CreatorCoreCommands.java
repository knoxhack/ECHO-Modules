package com.knoxhack.echo.creatorcore.command;

import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorExportResult;
import com.knoxhack.echo.creatorcore.api.CreatorPermission;
import com.knoxhack.echo.creatorcore.codex.CodexJobProfile;
import com.knoxhack.echo.creatorcore.codex.CodexJobSnapshot;
import com.knoxhack.echo.creatorcore.codex.CodexPilotSnapshot;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.session.CreatorPermissionService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;

public final class CreatorCoreCommands {
    private static final int MAX_LINES = 16;
    private static final String CLIENT_ENTRYPOINT = "com.knoxhack.echo.creatorcore.EchoCreatorCoreClient";

    private CreatorCoreCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> creatorCoreRoot() {
        return Commands.literal("creatorcore")
                .requires(CreatorPermissionService::canView)
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("open").executes(context -> open(context.getSource())))
                .then(Commands.literal("doctor").executes(context -> requireOperator(context.getSource(),
                        () -> doctor(context.getSource()))))
                .then(Commands.literal("adapters").executes(context -> adapters(context.getSource())))
                .then(Commands.literal("definitions").executes(context -> definitions(context.getSource())))
                .then(Commands.literal("diagnostics").executes(context -> diagnostics(context.getSource())))
                .then(draftsRoot())
                .then(Commands.literal("reload").executes(context -> requireOperator(context.getSource(),
                        () -> reload(context.getSource()))))
                .then(Commands.literal("report").executes(context -> report(context.getSource())))
                .then(Commands.literal("help").executes(context -> help(context.getSource())))
                .then(Commands.literal("panels").executes(context -> panels(context.getSource())))
                .then(codexRoot())
                .then(missionRoot());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> creatorAliasRoot() {
        return Commands.literal("creator")
                .requires(CreatorPermissionService::canView)
                .executes(context -> status(context.getSource()))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("open").executes(context -> open(context.getSource())))
                .then(Commands.literal("doctor").executes(context -> requireOperator(context.getSource(),
                        () -> doctor(context.getSource()))))
                .then(Commands.literal("drafts").executes(context -> listDrafts(context.getSource())))
                .then(Commands.literal("help").executes(context -> help(context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> draftsRoot() {
        return Commands.literal("drafts")
                .executes(context -> listDrafts(context.getSource()))
                .then(Commands.literal("list").executes(context -> listDrafts(context.getSource())))
                .then(Commands.literal("create")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("pack", StringArgumentType.word())
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .executes(context -> createDraft(context.getSource(),
                                                        StringArgumentType.getString(context, "type"),
                                                        StringArgumentType.getString(context, "pack"),
                                                        StringArgumentType.getString(context, "id")))))))
                .then(Commands.literal("validate")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> requireOperator(context.getSource(),
                                        () -> validateDraft(context.getSource(),
                                                StringArgumentType.getString(context, "id"))))))
                .then(Commands.literal("export")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> exportDraft(context.getSource(),
                                        StringArgumentType.getString(context, "id")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> missionRoot() {
        return Commands.literal("mission")
                .then(Commands.literal("draft")
                        .then(Commands.argument("pack", StringArgumentType.word())
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(context -> createMissionDraft(context.getSource(),
                                                StringArgumentType.getString(context, "pack"),
                                                StringArgumentType.getString(context, "id"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> codexRoot() {
        return Commands.literal("codex")
                .executes(context -> codexStatus(context.getSource()))
                .then(Commands.literal("status").executes(context -> codexStatus(context.getSource())))
                .then(Commands.literal("run")
                        .then(Commands.argument("profile", StringArgumentType.word())
                                .executes(context -> codexRun(context.getSource(),
                                        StringArgumentType.getString(context, "profile"), "", false))
                                .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                        .executes(context -> codexRun(context.getSource(),
                                                StringArgumentType.getString(context, "profile"),
                                                StringArgumentType.getString(context, "prompt"), false)))))
                .then(Commands.literal("vision")
                        .then(Commands.literal("run")
                                .then(Commands.argument("profile", StringArgumentType.word())
                                        .executes(context -> codexRun(context.getSource(),
                                                StringArgumentType.getString(context, "profile"), "", true))
                                        .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                                .executes(context -> codexRun(context.getSource(),
                                                        StringArgumentType.getString(context, "profile"),
                                                        StringArgumentType.getString(context, "prompt"), true))))))
                .then(pilotRoot())
                .then(Commands.literal("refresh")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .executes(context -> codexRefresh(context.getSource(),
                                        StringArgumentType.getString(context, "job")))))
                .then(Commands.literal("validate")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .executes(context -> codexValidate(context.getSource(),
                                        StringArgumentType.getString(context, "job")))))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .executes(context -> codexCancel(context.getSource(),
                                        StringArgumentType.getString(context, "job")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> pilotRoot() {
        return Commands.literal("pilot")
                .executes(context -> pilotStatus(context.getSource()))
                .then(Commands.literal("status").executes(context -> pilotStatus(context.getSource())))
                .then(Commands.literal("spawn")
                        .executes(context -> pilotSpawn(context.getSource(), "echonpcore:test_survivor", "codex-pilot"))
                        .then(Commands.argument("profileId", StringArgumentType.string())
                                .executes(context -> pilotSpawn(context.getSource(),
                                        StringArgumentType.getString(context, "profileId"), "codex-pilot"))
                                .then(Commands.argument("label", StringArgumentType.greedyString())
                                        .executes(context -> pilotSpawn(context.getSource(),
                                                StringArgumentType.getString(context, "profileId"),
                                                StringArgumentType.getString(context, "label"))))))
                .then(Commands.literal("stop").executes(context -> pilotStop(context.getSource())))
                .then(Commands.literal("pause").executes(context -> pilotPause(context.getSource())))
                .then(Commands.literal("resume").executes(context -> pilotResume(context.getSource())))
                .then(Commands.literal("despawn").executes(context -> pilotDespawn(context.getSource())))
                .then(Commands.literal("task")
                        .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                .executes(context -> pilotTask(context.getSource(),
                                        StringArgumentType.getString(context, "prompt")))))
                .then(Commands.literal("inspect").executes(context -> pilotInspect(context.getSource())))
                .then(Commands.literal("capture").executes(context -> pilotCapture(context.getSource())))
                .then(Commands.literal("interact").executes(context -> pilotInteract(context.getSource())))
                .then(Commands.literal("use").executes(context -> pilotInteract(context.getSource())))
                .then(Commands.literal("follow").executes(context -> pilotFollow(context.getSource())))
                .then(Commands.literal("goto")
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> pilotGoto(context.getSource(),
                                                        DoubleArgumentType.getDouble(context, "x"),
                                                        DoubleArgumentType.getDouble(context, "y"),
                                                        DoubleArgumentType.getDouble(context, "z")))))))
                .then(Commands.literal("look")
                        .then(Commands.argument("yaw", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("pitch", DoubleArgumentType.doubleArg(-90.0D, 90.0D))
                                        .executes(context -> pilotLook(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "yaw"),
                                                DoubleArgumentType.getDouble(context, "pitch"))))))
                .then(Commands.literal("say")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> pilotSay(context.getSource(),
                                        StringArgumentType.getString(context, "message")))))
                .then(Commands.literal("place")
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("block", StringArgumentType.string())
                                                        .executes(context -> pilotPlace(context.getSource(),
                                                                DoubleArgumentType.getDouble(context, "x"),
                                                                DoubleArgumentType.getDouble(context, "y"),
                                                                DoubleArgumentType.getDouble(context, "z"),
                                                                StringArgumentType.getString(context, "block"))))))))
                .then(Commands.literal("break")
                        .executes(context -> pilotBreak(context.getSource(), null))
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(context -> pilotBreak(context.getSource(), blockArgs(
                                                        DoubleArgumentType.getDouble(context, "x"),
                                                        DoubleArgumentType.getDouble(context, "y"),
                                                        DoubleArgumentType.getDouble(context, "z"))))))));
    }

    private static int status(CommandSourceStack source) {
        CreatorCoreApi api = CreatorCoreApi.get();
        long available = api.adapters().availableCount();
        int total = api.adapters().adapters().size();
        var diagnostics = api.listDiagnostics();
        long errors = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.WARNING).count();
        tell(source, "CreatorCore is " + (api.isCreatorModeEnabled() ? "enabled" : "disabled")
                + "; adapters " + available + "/" + total
                + ", drafts " + api.drafts().listDrafts().size()
                + ", definitions " + api.listDefinitions().size()
                + ", diagnostics " + errors + " errors/" + warnings + " warnings.", ChatFormatting.AQUA);
        tell(source, "Writes: " + lockState(CreatorCoreConfig.ALLOW_DRAFT_WRITES)
                + ", exports: " + lockState(CreatorCoreConfig.ALLOW_EXPORTS)
                + ", permission: " + CreatorPermissionService.permissionFor(source) + ".", ChatFormatting.GRAY);
        tell(source, "Use /echo creatorcore help for commands or /echo creatorcore report for a support summary.", ChatFormatting.DARK_GRAY);
        return Command.SINGLE_SUCCESS;
    }

    private static int open(CommandSourceStack source) {
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CLIENT_DASHBOARD, true)) {
            fail(source, "CreatorCore dashboard is disabled by config (allow_client_dashboard=false).");
            return 0;
        }
        try {
            Class.forName(CLIENT_ENTRYPOINT, false, Thread.currentThread().getContextClassLoader())
                    .getMethod("openDashboard")
                    .invoke(null);
            tell(source, "Opening CreatorCore dashboard.", ChatFormatting.AQUA);
            return Command.SINGLE_SUCCESS;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | LinkageError exception) {
            fail(source, "CreatorCore client dashboard entrypoint is unavailable: " + exception.getMessage());
            return 0;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            fail(source, "CreatorCore dashboard failed to open: " + cause.getMessage());
            return 0;
        }
    }

    private static int doctor(CommandSourceStack source) {
        CreatorCoreApi.get().runDoctor().compactLines()
                .forEach(line -> tell(source, line, ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int report(CommandSourceStack source) {
        CreatorCoreApi.get().runDoctor().compactLines()
                .forEach(line -> tell(source, line, ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int adapters(CommandSourceStack source) {
        var adapters = CreatorCoreApi.get().adapters().adapters();
        long available = adapters.stream().filter(CreatorAdapter::isAvailable).count();
        tell(source, "Creator adapters: " + available + "/" + adapters.size() + " available.", ChatFormatting.AQUA);
        adapters.stream()
                .sorted(Comparator.comparing(adapter -> adapter.id().toString()))
                .forEach(adapter -> tell(source,
                        adapter.displayName() + " [" + adapter.id().getPath() + "] "
                                + (adapter.isAvailable() ? "available" : "stub") + " - " + adapter.status()
                                + capabilitySuffix(adapter),
                        adapter.isAvailable() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    private static int definitions(CommandSourceStack source) {
        var definitions = CreatorCoreApi.get().listDefinitions().stream()
                .sorted(Comparator.comparing(CreatorDefinitionSummary::type).thenComparing(def -> def.id().toString()))
                .toList();
        tell(source, "Creator definitions: " + definitions.size(), ChatFormatting.AQUA);
        definitions.stream().limit(MAX_LINES).forEach(definition ->
                tell(source, definition.type() + " " + definition.id()
                        + " [" + definition.sourceAdapter() + "/" + definition.pack() + "] "
                        + definition.status() + " - " + definition.title(), ChatFormatting.GRAY));
        if (definitions.size() > MAX_LINES) {
            tell(source, "... " + (definitions.size() - MAX_LINES) + " more definition(s).", ChatFormatting.DARK_GRAY);
        }
        if (definitions.isEmpty()) {
            tell(source, "No definitions are available yet. Install/reload ScriptCore or compatible adapters.", ChatFormatting.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int diagnostics(CommandSourceStack source) {
        var diagnostics = CreatorCoreApi.get().listDiagnostics();
        tell(source, "Creator diagnostics: " + diagnostics.size(), ChatFormatting.AQUA);
        diagnostics.stream().limit(MAX_LINES).forEach(diagnostic ->
                tell(source, diagnostic.severity() + " " + diagnostic.code() + ": " + diagnostic.message(),
                        color(diagnostic)));
        if (diagnostics.size() > MAX_LINES) {
            tell(source, "... " + (diagnostics.size() - MAX_LINES) + " more diagnostic(s).", ChatFormatting.DARK_GRAY);
        }
        if (diagnostics.isEmpty()) {
            tell(source, "No diagnostics reported.", ChatFormatting.GREEN);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listDrafts(CommandSourceStack source) {
        var drafts = CreatorCoreApi.get().drafts().listDrafts();
        tell(source, "Creator drafts: " + drafts.size(), ChatFormatting.AQUA);
        drafts.stream().limit(MAX_LINES).forEach(draft ->
                tell(source, draft.type() + " " + draft.id() + " [" + draft.status() + "] "
                        + draft.pack() + " - " + draft.title(), ChatFormatting.GRAY));
        if (drafts.size() > MAX_LINES) {
            tell(source, "... " + (drafts.size() - MAX_LINES) + " more draft(s).", ChatFormatting.DARK_GRAY);
        }
        if (drafts.isEmpty()) {
            tell(source, "No drafts yet. Use /echo creatorcore drafts create <type> <pack> <id> after unlocking draft writes.", ChatFormatting.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int createDraft(CommandSourceStack source, String type, String pack, String rawId) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.CREATOR)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.CREATOR));
            return 0;
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false)) {
            fail(source, "Draft writes are locked by config (allow_draft_writes=false).");
            return 0;
        }
        Identifier id = parseId(source, rawId);
        if (id == null) {
            return 0;
        }
        try {
            CreatorDraft draft = CreatorCoreApi.get().drafts().createFromTemplate(type, id, pack);
            var path = CreatorCoreApi.get().drafts().saveDraft(id);
            tell(source, "Created " + draft.type() + " draft " + id + " at " + path + ".", ChatFormatting.GREEN);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException | java.io.IOException exception) {
            fail(source, "Draft creation failed: " + exception.getMessage());
            return 0;
        }
    }

    private static int createMissionDraft(CommandSourceStack source, String pack, String rawId) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.CREATOR)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.CREATOR));
            return 0;
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false)) {
            fail(source, "Draft writes are locked by config (allow_draft_writes=false).");
            return 0;
        }
        Identifier id = parseId(source, rawId);
        if (id == null) {
            return 0;
        }
        try {
            CreatorDraft draft = CreatorCoreApi.get().drafts().createMissionStudioDraft(id, pack, "mission_studio");
            var path = CreatorCoreApi.get().drafts().saveDraft(id);
            tell(source, "Created Mission Studio draft " + id + " at " + path
                    + " with chapter, phase, briefing, prerequisites, objectives, and rewards fields.",
                    ChatFormatting.GREEN);
            return draft == null ? 0 : Command.SINGLE_SUCCESS;
        } catch (RuntimeException | java.io.IOException exception) {
            fail(source, "Mission Studio draft creation failed: " + exception.getMessage());
            return 0;
        }
    }

    private static int validateDraft(CommandSourceStack source, String rawId) {
        Identifier id = parseId(source, rawId);
        if (id == null) {
            return 0;
        }
        var diagnostics = CreatorCoreApi.get().drafts().validateDraft(id);
        long errors = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.WARNING).count();
        tell(source, "Draft " + id + " validation: " + errors + " error(s), " + warnings + " warning(s).",
                errors == 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
        diagnostics.stream().limit(MAX_LINES).forEach(diagnostic ->
                tell(source, diagnostic.severity() + " " + diagnostic.code() + ": " + diagnostic.message(), color(diagnostic)));
        return errors == 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int exportDraft(CommandSourceStack source, String rawId) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false)) {
            fail(source, "Exports are locked by config (allow_exports=false).");
            return 0;
        }
        Identifier id = parseId(source, rawId);
        if (id == null) {
            return 0;
        }
        CreatorExportResult result = CreatorCoreApi.get().exports().exportDraft(id);
        tell(source, result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        result.diagnostics().stream().limit(MAX_LINES)
                .forEach(diagnostic -> tell(source, diagnostic.severity() + " " + diagnostic.code() + ": "
                        + diagnostic.message(), color(diagnostic)));
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reload(CommandSourceStack source) {
        int reloaded = 0;
        for (CreatorAdapter adapter : CreatorCoreApi.get().adapters().adapters()) {
            try {
                adapter.reload();
                reloaded++;
            } catch (RuntimeException | LinkageError exception) {
                tell(source, "Adapter reload failed for " + adapter.displayName() + ": " + exception.getMessage(), ChatFormatting.YELLOW);
            }
        }
        tell(source, "Reload requested for " + reloaded + " CreatorCore adapter(s).", ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int codexStatus(CommandSourceStack source) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        var status = CreatorCoreApi.get().codex().refreshStatus();
        tell(source, "Codex bridge: " + (status.ok() ? "online" : "offline")
                + ", codex=" + (status.codexAvailable() ? "available" : "unavailable")
                + ", jobs=" + status.jobCount() + ", running=" + status.runningJobCount(), status.ok() ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        tell(source, "Bridge URL: " + CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_URL, "http://127.0.0.1:47321")
                + ", workspace=" + status.workspace(), ChatFormatting.GRAY);
        tell(source, "Bridge guards: auth=" + (status.authRequired() ? "required" : "off")
                + ", sidecar repo edits=" + (status.repoEditsAllowed() ? "allowed" : "locked")
                + ", command template=" + (status.commandTemplateConfigured() ? "configured" : "default"), ChatFormatting.GRAY);
        if (!status.codexError().isBlank()) {
            tell(source, "Codex detail: " + status.codexError(), ChatFormatting.YELLOW);
        }
        CodexJobSnapshot latest = status.latestJob();
        if (latest.hasJob()) {
            printJob(source, latest);
        }
        return status.ok() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int codexRun(CommandSourceStack source, String profile, String prompt, boolean useLatestCapture) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_BRIDGE, false)) {
            fail(source, "Codex bridge is locked by config (allow_codex_bridge=false).");
            return 0;
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS, false)) {
            fail(source, "Codex repo edits are locked by config (allow_codex_repo_edits=false).");
            return 0;
        }
        if (CodexJobProfile.byId(profile).isEmpty()) {
            fail(source, "Unknown Codex profile: " + profile + ". Valid profiles: " + CodexJobProfile.ids());
            return 0;
        }
        CodexJobSnapshot job = useLatestCapture
                ? CreatorCoreApi.get().codex().startJobWithLatestCapture(profile, prompt)
                : CreatorCoreApi.get().codex().startJob(profile, prompt);
        printJob(source, job);
        return job.hasJob() && !"unavailable".equals(job.state()) ? Command.SINGLE_SUCCESS : 0;
    }

    private static int codexRefresh(CommandSourceStack source, String jobId) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        CodexJobSnapshot job = CreatorCoreApi.get().codex().getJob(jobId);
        printJob(source, job);
        return job.hasJob() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int codexValidate(CommandSourceStack source, String jobId) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        CodexJobSnapshot job = CreatorCoreApi.get().codex().validateJob(jobId);
        printJob(source, job);
        return job.hasJob() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int codexCancel(CommandSourceStack source, String jobId) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        CodexJobSnapshot job = CreatorCoreApi.get().codex().cancelJob(jobId);
        printJob(source, job);
        return job.hasJob() ? Command.SINGLE_SUCCESS : 0;
    }

    private static int pilotStatus(CommandSourceStack source) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        CodexPilotSnapshot status = CreatorCoreApi.get().pilot().snapshot();
        tell(source, "Codex Pilot: " + (status.enabled() ? "enabled" : "locked")
                + ", spawned=" + status.spawned()
                + ", paused=" + status.paused()
                + ", autopilot=" + (status.autopilotAllowed() ? "allowed" : "locked")
                + ", worldActions=" + (status.worldActionsAllowed() ? "allowed" : "locked"), ChatFormatting.AQUA);
        tell(source, "Pilot profile=" + status.profile()
                + ", label=" + status.label()
                + ", dimension=" + status.dimension()
                + ", position=" + status.position(), ChatFormatting.GRAY);
        tell(source, status.lastMessage(), status.enabled() ? ChatFormatting.GRAY : ChatFormatting.YELLOW);
        status.recentEvents().stream().skip(Math.max(0, status.recentEvents().size() - 4))
                .forEach(line -> tell(source, "pilot " + line, ChatFormatting.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int pilotSpawn(CommandSourceStack source, String profileId, String label) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().spawn(source, profileId, label);
        printPilotResult(source);
        return result;
    }

    private static int pilotStop(CommandSourceStack source) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().stop();
        printPilotResult(source);
        return result;
    }

    private static int pilotPause(CommandSourceStack source) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().pause();
        printPilotResult(source);
        return result;
    }

    private static int pilotResume(CommandSourceStack source) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().resume();
        printPilotResult(source);
        return result;
    }

    private static int pilotDespawn(CommandSourceStack source) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().despawn();
        printPilotResult(source);
        return result;
    }

    private static int pilotTask(CommandSourceStack source, String prompt) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().submitTask(prompt);
        printPilotResult(source);
        return result;
    }

    private static int pilotInspect(CommandSourceStack source) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().inspect(source);
        printPilotResult(source);
        return result;
    }

    private static int pilotCapture(CommandSourceStack source) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().capture();
        printPilotResult(source);
        return result;
    }

    private static int pilotInteract(CommandSourceStack source) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().interact(source);
        printPilotResult(source);
        return result;
    }

    private static int pilotFollow(CommandSourceStack source) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().follow(source);
        printPilotResult(source);
        return result;
    }

    private static int pilotGoto(CommandSourceStack source, double x, double y, double z) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().goTo(source, x, y, z);
        printPilotResult(source);
        return result;
    }

    private static int pilotLook(CommandSourceStack source, double yaw, double pitch) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().look((float) yaw, (float) pitch);
        printPilotResult(source);
        return result;
    }

    private static int pilotSay(CommandSourceStack source, String message) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().say(source, message);
        printPilotResult(source);
        return result;
    }

    private static int pilotPlace(CommandSourceStack source, double x, double y, double z, String block) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        JsonObject args = blockArgs(x, y, z);
        args.addProperty("block", block);
        int result = CreatorCoreApi.get().pilot().placeBlock(source, args);
        printPilotResult(source);
        return result;
    }

    private static int pilotBreak(CommandSourceStack source, JsonObject args) {
        if (!CreatorPermissionService.permissionFor(source).atLeast(CreatorPermission.DEVELOPER)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.DEVELOPER));
            return 0;
        }
        int result = CreatorCoreApi.get().pilot().breakTarget(source, args == null ? new JsonObject() : args);
        printPilotResult(source);
        return result;
    }

    private static JsonObject blockArgs(double x, double y, double z) {
        JsonObject args = new JsonObject();
        args.addProperty("x", x);
        args.addProperty("y", y);
        args.addProperty("z", z);
        return args;
    }

    private static void printPilotResult(CommandSourceStack source) {
        CodexPilotSnapshot status = CreatorCoreApi.get().pilot().snapshot();
        tell(source, status.lastMessage(), status.lastMessage().contains("refused") || status.lastMessage().contains("failed")
                ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
    }

    private static int help(CommandSourceStack source) {
        tell(source, "CreatorCore commands:", ChatFormatting.AQUA);
        tell(source, "/echo creatorcore status | open | report | doctor | adapters | reload", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore definitions | diagnostics | drafts list", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore drafts create <type> <pack> <id>", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore mission draft <pack> <id>", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore drafts validate <id> | drafts export <id>", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore codex status | codex run <profile> [prompt] | codex validate <job>", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore codex vision capture <label> | codex vision run <profile> [prompt]", ChatFormatting.GRAY);
        tell(source, "/echo creatorcore codex pilot spawn|status|task|inspect|capture|interact|follow|goto|look|say|place|break|stop", ChatFormatting.GRAY);
        tell(source, "Docs: addons/echocreatorcore/docs/CREATORCORE_USAGE.md", ChatFormatting.DARK_GRAY);
        return Command.SINGLE_SUCCESS;
    }

    private static int panels(CommandSourceStack source) {
        var panels = CreatorCoreApi.get().panels().panels();
        tell(source, "Creator panels: " + panels.size(), ChatFormatting.AQUA);
        panels.forEach(panel -> tell(source, panel.id() + " - " + panel.title()
                + (panel.summary().isBlank() ? "" : ": " + panel.summary()), ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int requireOperator(CommandSourceStack source, IntSupplier action) {
        if (!CreatorPermissionService.canOperate(source)) {
            fail(source, CreatorPermissionService.denial(source, CreatorPermission.OPERATOR));
            return 0;
        }
        return action.getAsInt();
    }

    private static Identifier parseId(CommandSourceStack source, String rawId) {
        String value = rawId == null ? "" : rawId.trim();
        if (!value.contains(":")) {
            value = EchoCreatorCore.MODID + ":" + value;
        }
        Identifier id = Identifier.tryParse(value);
        if (id == null) {
            fail(source, "Invalid CreatorCore id: " + rawId);
        }
        return id;
    }

    private static String lockState(EchoNativeConfigSpec.BooleanValue value) {
        return CreatorCoreConfig.bool(value, false) ? "allowed" : "locked";
    }

    private static String capabilitySuffix(CreatorAdapter adapter) {
        return adapter.capabilities().isEmpty() ? "" : " capabilities=" + adapter.capabilities();
    }

    private static ChatFormatting color(CreatorDiagnostic diagnostic) {
        return switch (diagnostic.severity()) {
            case ERROR -> ChatFormatting.RED;
            case WARNING -> ChatFormatting.YELLOW;
            case INFO -> ChatFormatting.GRAY;
        };
    }

    private static void printJob(CommandSourceStack source, CodexJobSnapshot job) {
        if (!job.hasJob()) {
            fail(source, job.error().isBlank() ? "Codex job unavailable." : job.error());
            return;
        }
        tell(source, "Codex job " + job.id() + " [" + job.profile() + "] state=" + job.state()
                + ", validation=" + job.validationStatus(), ChatFormatting.AQUA);
        if (!job.error().isBlank()) {
            tell(source, "Codex error: " + job.error(), ChatFormatting.RED);
        }
        job.changedFiles().stream().limit(MAX_LINES)
                .forEach(path -> tell(source, "changed " + path, ChatFormatting.GRAY));
        if (job.changedFiles().size() > MAX_LINES) {
            tell(source, "... " + (job.changedFiles().size() - MAX_LINES) + " more changed file(s).", ChatFormatting.DARK_GRAY);
        }
        job.validationLines().stream().skip(Math.max(0, job.validationLines().size() - 6))
                .forEach(line -> tell(source, "validation " + line, ChatFormatting.DARK_GRAY));
    }

    private static void tell(CommandSourceStack source, String message, ChatFormatting color) {
        source.sendSuccess(() -> Component.literal("[ECHO CREATOR] " + message).withStyle(color), false);
    }

    private static void fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("[ECHO CREATOR] " + message).withStyle(ChatFormatting.RED));
    }
}
