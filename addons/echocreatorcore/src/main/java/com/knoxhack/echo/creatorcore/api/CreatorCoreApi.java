package com.knoxhack.echo.creatorcore.api;

import com.knoxhack.echo.creatorcore.adapter.CreatorAdapterRegistry;
import com.knoxhack.echo.creatorcore.codex.CodexBridgeService;
import com.knoxhack.echo.creatorcore.codex.CodexPilotService;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.definition.CreatorDefinitionService;
import com.knoxhack.echo.creatorcore.draft.CreatorDraftService;
import com.knoxhack.echo.creatorcore.export.CreatorExportService;
import com.knoxhack.echo.creatorcore.session.CreatorProjectManager;
import com.knoxhack.echo.creatorcore.session.CreatorSessionManager;
import com.knoxhack.echo.creatorcore.ui.CreatorPanelRegistry;
import com.knoxhack.echo.creatorcore.validation.CreatorDoctorReport;
import com.knoxhack.echo.creatorcore.validation.CreatorValidationService;
import java.util.List;

public final class CreatorCoreApi {
    private static final CreatorCoreApi INSTANCE = new CreatorCoreApi();

    private final CreatorAdapterRegistry adapters = new CreatorAdapterRegistry();
    private final CreatorSessionManager sessions = new CreatorSessionManager();
    private final CreatorProjectManager projects = new CreatorProjectManager();
    private final CreatorPanelRegistry panels = new CreatorPanelRegistry();
    private final CreatorDraftService drafts = new CreatorDraftService();
    private final CreatorDefinitionService definitions = new CreatorDefinitionService(adapters);
    private final CreatorValidationService validation = new CreatorValidationService(adapters, drafts);
    private final CreatorExportService exports = new CreatorExportService(drafts, validation, adapters);
    private final CodexBridgeService codex = new CodexBridgeService();
    private final CodexPilotService pilot = new CodexPilotService();
    private boolean bootstrapped;

    private CreatorCoreApi() {
    }

    public static CreatorCoreApi get() {
        return INSTANCE;
    }

    public synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        adapters.registerDefaults();
        panels.registerDefaults();
        projects.refresh();
        bootstrapped = true;
    }

    public CreatorAdapterRegistry adapters() {
        bootstrap();
        return adapters;
    }

    public CreatorSessionManager sessions() {
        return sessions;
    }

    public CreatorDraftService drafts() {
        return drafts;
    }

    public CreatorDefinitionService definitions() {
        bootstrap();
        return definitions;
    }

    public CreatorValidationService validation() {
        bootstrap();
        return validation;
    }

    public CreatorExportService exports() {
        return exports;
    }

    public CodexBridgeService codex() {
        return codex;
    }

    public CodexPilotService pilot() {
        return pilot;
    }

    public CreatorPanelRegistry panels() {
        bootstrap();
        return panels;
    }

    public CreatorProjectManager projects() {
        return projects;
    }

    public boolean isCreatorModeEnabled() {
        return CreatorCoreConfig.bool(CreatorCoreConfig.ENABLED, true)
                && CreatorCoreConfig.bool(CreatorCoreConfig.CREATOR_MODE_ENABLED, true);
    }

    public CreatorDoctorReport runDoctor() {
        bootstrap();
        return validation.runDoctor();
    }

    public List<CreatorDefinitionSummary> listDefinitions() {
        bootstrap();
        return definitions.listDefinitions();
    }

    public List<CreatorDiagnostic> listDiagnostics() {
        bootstrap();
        return validation.listDiagnostics();
    }
}
