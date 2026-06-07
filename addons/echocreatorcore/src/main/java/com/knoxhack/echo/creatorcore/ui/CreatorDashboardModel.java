package com.knoxhack.echo.creatorcore.ui;

import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionDetail;
import com.knoxhack.echo.creatorcore.api.CreatorDefinitionSummary;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorFormSchema;
import com.knoxhack.echo.creatorcore.api.CreatorPreviewSummary;
import com.knoxhack.echo.creatorcore.codex.CodexBridgeStatus;
import com.knoxhack.echo.creatorcore.codex.CodexJobSnapshot;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.validation.CreatorDoctorReport;
import java.util.List;
import java.util.Optional;

public final class CreatorDashboardModel {
    private CreatorDoctorReport doctorReport;
    private List<CreatorAdapter> adapters = List.of();
    private List<CreatorDefinitionSummary> definitions = List.of();
    private List<CreatorDefinitionDetail> definitionDetails = List.of();
    private List<CreatorPreviewSummary> previews = List.of();
    private List<CreatorFormSchema> formSchemas = List.of();
    private List<CreatorDiagnostic> diagnostics = List.of();
    private List<CreatorDraft> drafts = List.of();
    private CodexBridgeStatus codexStatus = CodexBridgeStatus.unavailable("Codex bridge has not been checked yet.");
    private CodexJobSnapshot codexJob = CodexJobSnapshot.empty("No Codex job has run yet.");

    public CreatorDashboardModel() {
        refresh();
    }

    public void refresh() {
        CreatorCoreApi api = CreatorCoreApi.get();
        doctorReport = api.runDoctor();
        adapters = api.adapters().adapters();
        definitions = api.listDefinitions();
        definitionDetails = api.definitions().details(12);
        previews = api.definitions().previewSummaries();
        formSchemas = api.definitions().formSchemas();
        diagnostics = api.listDiagnostics();
        drafts = api.drafts().listDrafts();
        codexStatus = api.codex().status();
        codexJob = api.codex().lastJob();
    }

    public CreatorDoctorReport doctorReport() {
        return doctorReport;
    }

    public List<CreatorAdapter> adapters() {
        return adapters;
    }

    public List<CreatorDefinitionSummary> definitions() {
        return definitions;
    }

    public List<CreatorDefinitionDetail> definitionDetails() {
        return definitionDetails;
    }

    public Optional<CreatorDefinitionDetail> firstDefinitionDetail() {
        return definitionDetails.stream().findFirst();
    }

    public List<CreatorPreviewSummary> previews() {
        return previews;
    }

    public List<CreatorFormSchema> formSchemas() {
        return formSchemas;
    }

    public List<CreatorDiagnostic> diagnostics() {
        return diagnostics;
    }

    public List<CreatorDraft> drafts() {
        return drafts;
    }

    public boolean writeLocked() {
        return !CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false);
    }

    public boolean exportsLocked() {
        return !CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false);
    }

    public CodexBridgeStatus codexStatus() {
        return codexStatus;
    }

    public CodexJobSnapshot codexJob() {
        return codexJob;
    }

    public boolean codexBridgeLocked() {
        return !CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_BRIDGE, false);
    }

    public boolean codexRepoEditsLocked() {
        return !CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_REPO_EDITS, false);
    }
}
