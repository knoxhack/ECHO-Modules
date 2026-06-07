package com.knoxhack.echo.creatorcore.client;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.adapter.ScriptCoreCreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorFormField;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import com.knoxhack.echo.creatorcore.codex.CodexBridgeStatus;
import com.knoxhack.echo.creatorcore.codex.CodexJobSnapshot;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardScreen;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class CreatorCoreScreenCoreClientIntegration {
    public static final Identifier DASHBOARD_PAGE = EchoCreatorCore.id("creator_dashboard");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final List<String> SCRIPTCORE_DRAFT_TYPES = List.of(
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
    private static final List<String> SCRIPTCORE_TEMPLATE_SECTIONS = List.of(
            "objectives",
            "rewards",
            "conditions",
            "unlock_conditions",
            "actions",
            "on_start",
            "on_complete",
            "choices",
            "ranks",
            "markers",
            "effects");

    private CreatorCoreScreenCoreClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoScreenRegistry.registerDataProvider("creatorcore", CreatorCoreScreenCoreClientIntegration::resolve);
        EchoScreenRegistry.registerDataProvider(EchoCreatorCore.id("creatorcore"), CreatorCoreScreenCoreClientIntegration::resolve);
        EchoScreenRegistry.registerStyleSheet(EchoCreatorCore.id("creator_dashboard"));
        EchoScreenRegistry.registerAction("creatorcore.refresh", context -> {
            EchoScreens.invalidatePage(DASHBOARD_PAGE);
            return true;
        });
        EchoScreenRegistry.registerAction("creatorcore.open_vanilla", context -> {
            Minecraft.getInstance().setScreen(new CreatorDashboardScreen());
            return true;
        });
        EchoScreenRegistry.registerAction("creatorcore.create_mission_draft", context -> {
            if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false)) {
                return false;
            }
            Identifier id = Identifier.fromNamespaceAndPath(EchoCreatorCore.MODID, "mission_studio_draft");
            CreatorCoreApi.get().drafts().createFromTemplate("mission", id, EchoCreatorCore.MODID);
            EchoScreens.invalidatePage(DASHBOARD_PAGE);
            return true;
        });
        EchoScreenRegistry.registerAction("creatorcore.export_first_draft", context -> {
            if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false)) {
                return false;
            }
            return CreatorCoreApi.get().drafts().listDrafts().stream()
                    .findFirst()
                    .map(draft -> CreatorCoreApi.get().exports().exportDraft(draft.id()).success())
                    .orElse(false);
        });
        registerScriptCoreActions();
        registerCodexActions();
        EchoCreatorCore.LOGGER.info("CreatorCore ScreenCore dashboard bridge registered.");
    }

    public static boolean openDashboard() {
        register();
        return EchoScreens.open(DASHBOARD_PAGE, context());
    }

    public static EchoDataContext context() {
        return EchoDataContext.empty()
                .missingPlaceholder("")
                .put("creatorcore.pageId", DASHBOARD_PAGE.toString())
                .provider("creatorcore", CreatorCoreScreenCoreClientIntegration::resolve);
    }

    private static void registerScriptCoreActions() {
        EchoScreenRegistry.registerAction("creatorcore.scriptcore.create_draft", context -> {
            if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false)) {
                return false;
            }
            String type = firstNonBlank(context.param("type"), context.actionValue(), "mission");
            String pack = firstNonBlank(context.param("pack"), "scriptcore_draft");
            Identifier id = parseId(firstNonBlank(context.param("id"), context.argument(), pack + ":draft"));
            if (id == null) {
                return false;
            }
            CreatorCoreApi.get().drafts().createFromTemplate(type, id, pack);
            scriptCoreAdapter().flatMap(adapter -> adapter.createDraft(type, id));
            try {
                CreatorCoreApi.get().drafts().saveDraft(id);
            } catch (java.io.IOException ignored) {
                return false;
            }
            EchoScreens.invalidatePage(DASHBOARD_PAGE);
            return true;
        });
        EchoScreenRegistry.registerAction("creatorcore.scriptcore.add_template_section", context -> {
            if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false)) {
                return false;
            }
            Identifier id = parseId(firstNonBlank(context.param("id"), context.actionValue(), context.argument()));
            String section = firstNonBlank(context.param("section"), context.param("template"), "objectives");
            if (id == null || CreatorCoreApi.get().drafts().addTemplateSection(id, section).isEmpty()) {
                return false;
            }
            try {
                CreatorCoreApi.get().drafts().saveDraft(id);
            } catch (java.io.IOException ignored) {
                return false;
            }
            EchoScreens.invalidatePage(DASHBOARD_PAGE);
            return true;
        });
        EchoScreenRegistry.registerAction("creatorcore.scriptcore.validate_draft", context -> {
            Identifier id = parseId(firstNonBlank(context.param("id"), context.actionValue(), context.argument()));
            if (id == null) {
                return false;
            }
            boolean creatorOk = CreatorCoreApi.get().drafts().validateDraft(id).stream()
                    .noneMatch(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR);
            boolean scriptOk = scriptCoreAdapter()
                    .map(adapter -> adapter.validateDraft(id).stream()
                            .noneMatch(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR))
                    .orElse(true);
            EchoScreens.invalidatePage(DASHBOARD_PAGE);
            return creatorOk && scriptOk;
        });
        EchoScreenRegistry.registerAction("creatorcore.scriptcore.export_draft", context -> {
            if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false)) {
                return false;
            }
            Identifier id = parseId(firstNonBlank(context.param("id"), context.actionValue(), context.argument()));
            return id != null && CreatorCoreApi.get().exports().exportDraft(id).success();
        });
        EchoScreenRegistry.registerAction("creatorcore.scriptcore.reload_pack", context ->
                scriptCoreAdapter().map(adapter -> adapter.reloadPack(firstNonBlank(context.param("pack"),
                        context.actionValue(), context.argument()))).orElse(false));
        EchoScreenRegistry.registerAction("creatorcore.scriptcore.reload_type", context ->
                scriptCoreAdapter().map(adapter -> adapter.reloadType(firstNonBlank(context.param("type"),
                        context.actionValue(), context.argument()))).orElse(false));
    }

    private static void registerCodexActions() {
        EchoScreenRegistry.registerAction("creatorcore.codex.run_profile", context -> false);
        EchoScreenRegistry.registerAction("creatorcore.codex.refresh", context -> {
            String job = firstNonBlank(context.param("job"), context.actionValue(), context.argument(),
                    CreatorCoreApi.get().codex().lastJob().id());
            if (job.isBlank()) {
                CreatorCoreApi.get().codex().refreshStatus();
            } else {
                CreatorCoreApi.get().codex().getJob(job);
            }
            EchoScreens.invalidatePage(DASHBOARD_PAGE);
            return true;
        });
        EchoScreenRegistry.registerAction("creatorcore.codex.validate", context -> false);
    }

    private static Object resolve(EchoDataContext context, List<String> path) {
        CreatorDashboardModel model = new CreatorDashboardModel();
        if (path == null || path.isEmpty()) {
            return dashboard(model);
        }
        return switch (path.get(0)) {
            case "status" -> status(model);
            case "adapters" -> model.adapters().stream().map(CreatorCoreScreenCoreClientIntegration::adapter).toList();
            case "definitions" -> model.definitions().stream().map(CreatorCoreScreenCoreClientIntegration::definition).toList();
            case "details" -> model.definitionDetails().stream().map(CreatorCoreScreenCoreClientIntegration::detail).toList();
            case "diagnostics" -> model.diagnostics().stream().map(CreatorCoreScreenCoreClientIntegration::diagnostic).toList();
            case "drafts" -> model.drafts().stream().map(CreatorCoreScreenCoreClientIntegration::draft).toList();
            case "previews" -> model.previews().stream().map(CreatorCoreScreenCoreClientIntegration::preview).toList();
            case "codex" -> codex(model);
            case "missionFields" -> missionFields(model);
            case "scriptcoreDraftTypes" -> SCRIPTCORE_DRAFT_TYPES;
            case "scriptcoreTemplates" -> SCRIPTCORE_TEMPLATE_SECTIONS;
            case "scriptcoreDraftDiagnostics" -> draftDiagnostics(model);
            case "lastExport" -> Map.of(
                    "success", CreatorCoreApi.get().exports().lastResult().success(),
                    "message", CreatorCoreApi.get().exports().lastResult().message(),
                    "targetPath", CreatorCoreApi.get().exports().lastResult().targetPath());
            default -> "";
        };
    }

    private static Map<String, Object> dashboard(CreatorDashboardModel model) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status(model));
        map.put("adapters", model.adapters().stream().map(CreatorCoreScreenCoreClientIntegration::adapter).toList());
        map.put("definitions", model.definitions().stream().map(CreatorCoreScreenCoreClientIntegration::definition).toList());
        map.put("diagnostics", model.diagnostics().stream().map(CreatorCoreScreenCoreClientIntegration::diagnostic).toList());
        map.put("drafts", model.drafts().stream().map(CreatorCoreScreenCoreClientIntegration::draft).toList());
        map.put("previews", model.previews().stream().map(CreatorCoreScreenCoreClientIntegration::preview).toList());
        map.put("codex", codex(model));
        map.put("missionFields", missionFields(model));
        map.put("scriptcoreDraftTypes", SCRIPTCORE_DRAFT_TYPES);
        map.put("scriptcoreTemplates", SCRIPTCORE_TEMPLATE_SECTIONS);
        map.put("scriptcoreDraftDiagnostics", draftDiagnostics(model));
        return Map.copyOf(map);
    }

    private static Map<String, Object> status(CreatorDashboardModel model) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", "ECHO: CreatorCore");
        map.put("subtitle", "CreatorCore by ECHO Labs");
        map.put("creatorMode", CreatorCoreApi.get().isCreatorModeEnabled() ? "Enabled" : "Disabled");
        map.put("writeMode", model.writeLocked() ? "Locked" : "Allowed");
        map.put("exportMode", model.exportsLocked() ? "Locked" : "Allowed");
        map.put("adapters", model.doctorReport().adaptersAvailable() + "/" + model.doctorReport().adaptersTotal());
        map.put("definitions", model.definitions().size());
        map.put("diagnostics", model.diagnostics().size());
        map.put("errors", model.doctorReport().errors());
        map.put("warnings", model.doctorReport().warnings());
        map.put("drafts", model.drafts().size());
        map.put("scriptcoreDraftTypes", SCRIPTCORE_DRAFT_TYPES.size());
        map.put("codexBridge", model.codexStatus().ok() ? "Online" : "Offline");
        return Map.copyOf(map);
    }

    private static Map<String, Object> codex(CreatorDashboardModel model) {
        CodexBridgeStatus status = model.codexStatus();
        CodexJobSnapshot job = model.codexJob();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ok", status.ok());
        map.put("bridge", status.bridge());
        map.put("message", status.message());
        map.put("workspace", status.workspace());
        map.put("codexPath", status.codexPath());
        map.put("codexAvailable", status.codexAvailable());
        map.put("codexError", status.codexError());
        map.put("dryRun", status.dryRun());
        map.put("model", status.defaultModel());
        map.put("authRequired", status.authRequired());
        map.put("sidecarRepoEdits", status.repoEditsAllowed());
        map.put("commandTemplateConfigured", status.commandTemplateConfigured());
        map.put("defaultValidationProfile", status.defaultValidationProfile());
        map.put("diagnostics", status.diagnostics());
        map.put("jobs", status.jobCount());
        map.put("running", status.runningJobCount());
        map.put("bridgeMode", model.codexBridgeLocked() ? "Locked" : "Allowed");
        map.put("repoEditMode", model.codexRepoEditsLocked() ? "Locked" : "Allowed");
        map.put("profiles", status.profiles());
        map.put("validationProfiles", status.validationProfiles());
        map.put("job", Map.of(
                "id", job.id(),
                "profile", job.profile(),
                "state", job.state(),
                "validationStatus", job.validationStatus(),
                "error", job.error(),
                "changedFiles", job.changedFiles(),
                "validationLines", job.validationLines(),
                "stdoutSummary", job.stdoutSummary()));
        return Map.copyOf(map);
    }

    private static Map<String, Object> adapter(CreatorAdapter adapter) {
        return Map.of(
                "id", adapter.id().toString(),
                "name", adapter.displayName(),
                "available", adapter.isAvailable(),
                "capabilities", String.join(", ", adapter.capabilities()),
                "status", adapter.status());
    }

    private static Map<String, Object> definition(CreatorDefinitionSummary definition) {
        return Map.of(
                "id", definition.id().toString(),
                "type", definition.type(),
                "title", definition.title(),
                "adapter", definition.sourceAdapter(),
                "pack", definition.pack(),
                "status", definition.status());
    }

    private static Map<String, Object> detail(CreatorDefinitionDetail detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", detail.id() == null ? "" : detail.id().toString());
        map.put("type", detail.type());
        map.put("title", detail.title());
        map.put("description", detail.description());
        map.put("adapter", detail.sourceAdapter());
        map.put("pack", detail.pack());
        map.put("status", detail.status());
        map.put("sourceFile", detail.sourceFile().orElse(""));
        map.put("tags", String.join(", ", detail.tags()));
        map.put("rawFields", detail.rawJson().entrySet().size());
        map.put("preview", detail.previewLines());
        return Map.copyOf(map);
    }

    private static Map<String, Object> diagnostic(CreatorDiagnostic diagnostic) {
        return Map.of(
                "severity", diagnostic.severity().name(),
                "code", diagnostic.code(),
                "message", diagnostic.message(),
                "source", diagnostic.source(),
                "definitionId", diagnostic.definitionId().map(Identifier::toString).orElse(""),
                "suggestion", diagnostic.suggestion().orElse(""));
    }

    private static Map<String, Object> draft(CreatorDraft draft) {
        return Map.of(
                "id", draft.id().toString(),
                "type", draft.type(),
                "title", draft.title(),
                "pack", draft.pack(),
                "status", draft.status().name(),
                "adapter", draft.sourceAdapter(),
                "rawFields", draft.content().entrySet().size());
    }

    private static Map<String, Object> preview(CreatorPreviewSummary preview) {
        return Map.of(
                "id", preview.id() == null ? "" : preview.id().toString(),
                "type", preview.type(),
                "title", preview.title(),
                "adapter", preview.sourceAdapter(),
                "scope", preview.scope(),
                "lines", preview.lines());
    }

    private static List<Map<String, Object>> missionFields(CreatorDashboardModel model) {
        return model.formSchemas().stream()
                .filter(schema -> "mission".equals(schema.type()))
                .findFirst()
                .map(CreatorFormSchema::fields)
                .orElse(List.of())
                .stream()
                .map(CreatorCoreScreenCoreClientIntegration::field)
                .toList();
    }

    private static List<Map<String, Object>> draftDiagnostics(CreatorDashboardModel model) {
        return model.drafts().stream()
                .flatMap(draft -> CreatorCoreApi.get().drafts().validateDraft(draft.id()).stream()
                        .map(diagnostic -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("draftId", draft.id().toString());
                            map.put("draftType", draft.type());
                            map.put("severity", diagnostic.severity().name());
                            map.put("code", diagnostic.code());
                            map.put("message", diagnostic.message());
                            map.put("suggestion", diagnostic.suggestion().orElse(""));
                            return map;
                        }))
                .toList();
    }

    private static Map<String, Object> field(CreatorFormField field) {
        return Map.of(
                "name", field.name(),
                "label", field.label(),
                "kind", field.kind().name(),
                "required", field.required(),
                "options", String.join(", ", field.options()),
                "placeholder", field.placeholder(),
                "readOnly", field.readOnly());
    }

    private static Optional<ScriptCoreCreatorAdapter> scriptCoreAdapter() {
        return CreatorCoreApi.get().adapters().adapters().stream()
                .filter(ScriptCoreCreatorAdapter.class::isInstance)
                .map(ScriptCoreCreatorAdapter.class::cast)
                .findFirst();
    }

    private static Identifier parseId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            return null;
        }
        if (!value.contains(":")) {
            value = "scriptcore_draft:" + value;
        }
        return Identifier.tryParse(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
