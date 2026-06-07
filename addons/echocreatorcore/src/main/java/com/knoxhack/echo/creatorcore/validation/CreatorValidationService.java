package com.knoxhack.echo.creatorcore.validation;

import com.knoxhack.echo.creatorcore.adapter.CreatorAdapterRegistry;
import com.knoxhack.echo.creatorcore.api.CreatorAdapter;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.draft.CreatorDraftService;
import java.time.Instant;
import java.util.List;

public final class CreatorValidationService {
    private final CreatorAdapterRegistry adapters;
    private final CreatorDraftService drafts;

    public CreatorValidationService(CreatorAdapterRegistry adapters, CreatorDraftService drafts) {
        this.adapters = adapters;
        this.drafts = drafts;
    }

    public List<CreatorDiagnostic> listDiagnostics() {
        CreatorDiagnosticIndex index = new CreatorDiagnosticIndex();
        index.addAll(internalDiagnostics());
        index.addAll(adapters.diagnostics());
        drafts.listDrafts().forEach(draft -> index.addAll(drafts.validate(draft)));
        return index.all();
    }

    public CreatorDoctorReport runDoctor() {
        List<CreatorDiagnostic> diagnostics = listDiagnostics();
        long errors = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.WARNING).count();
        long info = diagnostics.stream().filter(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.INFO).count();
        boolean scriptCoreAvailable = adapters.adapters().stream()
                .filter(adapter -> adapter.id().getPath().equals("scriptcore"))
                .findFirst()
                .map(CreatorAdapter::isAvailable)
                .orElse(false);
        return new CreatorDoctorReport(
                Instant.now(),
                (int) adapters.availableCount(),
                adapters.adapters().size(),
                adapters.listDefinitions().size(),
                drafts.listDrafts().size(),
                errors,
                warnings,
                info,
                scriptCoreAvailable,
                !CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false),
                !CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false),
                drafts.store().root().toString(),
                CreatorCoreConfig.string(CreatorCoreConfig.EXPORT_ROOT, "config/echo/scripts"),
                true,
                diagnostics);
    }

    private static List<CreatorDiagnostic> internalDiagnostics() {
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.CREATOR_MODE_ENABLED, true)) {
            return List.of(CreatorDiagnostic.warning("creatorcore.mode.disabled",
                    "Creator mode is disabled in config.", "CreatorCore",
                    "Set creator_mode_enabled=true to enable dashboard features."));
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_DRAFT_WRITES, false)) {
            return List.of(CreatorDiagnostic.info("creatorcore.writes_locked",
                    "Draft writes are locked by default. Dashboard and validation remain read-only.", "CreatorCore"));
        }
        return List.of();
    }
}
