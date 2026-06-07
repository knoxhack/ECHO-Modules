package com.knoxhack.echo.creatorcore.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.knoxhack.echo.creatorcore.adapter.CreatorAdapterRegistry;
import com.knoxhack.echo.creatorcore.api.CreatorDiagnostic;
import com.knoxhack.echo.creatorcore.api.CreatorDraft;
import com.knoxhack.echo.creatorcore.api.CreatorExportResult;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.draft.CreatorDraftService;
import com.knoxhack.echo.creatorcore.validation.CreatorValidationService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class CreatorExportService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final CreatorDraftService drafts;
    private final CreatorValidationService validation;
    private final CreatorAdapterRegistry adapters;
    private CreatorExportResult lastResult = CreatorExportResult.failed("No export has run yet.", "");

    public CreatorExportService(CreatorDraftService drafts, CreatorValidationService validation, CreatorAdapterRegistry adapters) {
        this.drafts = drafts;
        this.validation = validation;
        this.adapters = adapters;
    }

    public CreatorExportResult exportDraft(Identifier id) {
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_EXPORTS, false)) {
            lastResult = CreatorExportResult.failed("Exports are locked by config (allow_exports=false).", exportRoot().toString());
            return lastResult;
        }
        CreatorDraft draft = drafts.getDraft(id).orElse(null);
        if (draft == null) {
            lastResult = CreatorExportResult.failed("Draft not found: " + id, "");
            return lastResult;
        }
        List<CreatorDiagnostic> diagnostics = drafts.validate(draft);
        boolean hasErrors = diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == CreatorDiagnostic.Severity.ERROR);
        if (hasErrors) {
            lastResult = new CreatorExportResult(false, "", diagnostics, "Draft has validation errors.", 0);
            return lastResult;
        }
        try {
            Path target = exportPath(draft);
            Optional<CreatorExportResult> scriptCoreResult = tryScriptCoreExport(draft, target);
            if (scriptCoreResult.isPresent()) {
                lastResult = scriptCoreResult.get();
                return lastResult;
            }
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                Path backup = target.resolveSibling(target.getFileName() + ".bak." + Instant.now().toEpochMilli());
                Files.copy(target, backup);
            }
            Files.writeString(target, GSON.toJson(draft.content()), StandardCharsets.UTF_8);
            lastResult = new CreatorExportResult(true, target.toString(), diagnostics,
                    "Exported draft " + id + " to " + target, 1);
            return lastResult;
        } catch (IOException exception) {
            lastResult = CreatorExportResult.failed("Export failed: " + exception.getMessage(), exportRoot().toString());
            return lastResult;
        }
    }

    public CreatorExportResult lastResult() {
        return lastResult;
    }

    public Path exportRoot() {
        String configured = CreatorCoreConfig.string(CreatorCoreConfig.EXPORT_ROOT, "config/echo/scripts");
        Path path = Path.of(configured);
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    public Path previewExportPath(CreatorDraft draft) throws IOException {
        return exportPath(draft);
    }

    private Optional<CreatorExportResult> tryScriptCoreExport(CreatorDraft draft, Path target) {
        if (adapters == null) {
            return Optional.empty();
        }
        return adapters.adapters().stream()
                .filter(adapter -> adapter.id().getPath().equals("scriptcore"))
                .filter(adapter -> adapter.capabilities().contains("export"))
                .filter(adapter -> adapter.isAvailable())
                .findFirst()
                .map(adapter -> adapter.exportDraft(draft, target));
    }

    private Path exportPath(CreatorDraft draft) throws IOException {
        Path base = exportRoot();
        Path target = base.resolve(segment(draft.pack(), "default"))
                .resolve(plural(segment(draft.type(), "definition")))
                .resolve(segment(draft.id().getNamespace(), "minecraft"))
                .resolve(draft.id().getPath() + ".json")
                .normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Refusing unsafe export path: " + target);
        }
        return target;
    }

    private static String segment(String value, String fallback) {
        String safe = value == null || value.isBlank() ? fallback : value;
        return safe.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static String plural(String type) {
        return type.endsWith("s") ? type : type + "s";
    }
}
