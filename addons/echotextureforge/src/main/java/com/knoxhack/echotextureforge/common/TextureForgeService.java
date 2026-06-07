package com.knoxhack.echotextureforge.common;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echotextureforge.EchoTextureForgeMod;
import com.knoxhack.echotextureforge.api.prompt.TexturePromptTemplate;
import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.common.config.TextureForgeConfig;
import com.knoxhack.echotextureforge.common.export.TextureApplyResult;
import com.knoxhack.echotextureforge.common.export.TextureApplyService;
import com.knoxhack.echotextureforge.common.export.TextureSheetImportPlan;
import com.knoxhack.echotextureforge.common.export.TextureSheetImportPlanner;
import com.knoxhack.echotextureforge.common.prompt.TexturePromptExporter;
import com.knoxhack.echotextureforge.common.report.TextureReportExporter;
import com.knoxhack.echotextureforge.common.review.TextureReviewService;
import com.knoxhack.echotextureforge.common.review.TextureReviewState;
import com.knoxhack.echotextureforge.common.review.TextureReviewStatus;
import com.knoxhack.echotextureforge.common.scan.TextureForgeScanner;
import com.knoxhack.echotextureforge.common.style.TextureStyleFamilies;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public enum TextureForgeService {
    INSTANCE;

    private TextureForgePaths paths = TextureForgePaths.discover();
    private TextureAuditReport lastReport;

    public void initialize() {
        this.paths = TextureForgePaths.discover();
    }

    public TextureForgePaths paths() {
        return paths;
    }

    public Optional<TextureAuditReport> lastReport() {
        return Optional.ofNullable(lastReport);
    }

    public void onServerStarted(Object event) {
        if (EchoBackendWorldEventBridge.serverStartedServer(event) == null) {
            return;
        }
        if (!TextureForgeConfig.enabled() || !TextureForgeConfig.scanOnStartup()) {
            return;
        }
        try {
            TextureAuditReport report = runAudit("", true, TextureForgeConfig.exportOnStartup(),
                    TextureForgeConfig.exportOnStartup());
            EchoTextureForgeMod.LOGGER.info("TextureForge startup scan complete: {} issue(s), output {}.",
                    report.issues().size(), report.outputRoot());
        } catch (IOException exception) {
            EchoTextureForgeMod.LOGGER.warn("TextureForge startup scan failed.", exception);
        }
    }

    public TextureAuditReport runAudit(String namespaceFilter, boolean includeRegistry,
                                       boolean exportReports, boolean exportPrompts) throws IOException {
        this.paths = TextureForgePaths.discover();
        TextureForgeScanner.ScanOutput output = TextureForgeScanner.scan(paths, clean(namespaceFilter), includeRegistry);
        TextureAuditReport report = output.report();
        List<Path> promptFiles = List.of();
        List<Path> reportFiles = List.of();
        if (exportPrompts) {
            promptFiles = TexturePromptExporter.export(report, paths);
        }
        if (exportReports) {
            reportFiles = TextureReportExporter.export(report, paths);
        }
        report = TextureReportExporter.withFiles(report, promptFiles, reportFiles);
        lastReport = report;
        return report;
    }

    public Path exportSinglePrompt(String namespace, String assetId, TextureKind kind) throws IOException {
        TextureSpec spec = lastReport == null
                ? fallbackSpec(namespace, assetId, kind)
                : lastReport.specs().stream()
                .filter(candidate -> candidate.namespace().equals(namespace)
                        && candidate.assetId().equals(assetId)
                        && candidate.assetKind() == kind)
                .findFirst()
                .orElseGet(() -> fallbackSpec(namespace, assetId, kind));
        Path path = paths.promptsDir().resolve("single_" + namespace + "_" + assetId.replace('/', '_') + ".md");
        TextureForgeMarkdown.write(path, TextureForgeMarkdown.heading(namespace + ":" + assetId)
                + TextureForgeMarkdown.codeFence("text", TexturePromptTemplate.singleTexturePrompt(spec)));
        return path;
    }

    public TextureSheetImportPlan planImport(String sheetName, boolean stageCrops) throws IOException {
        this.paths = TextureForgePaths.discover();
        return TextureSheetImportPlanner.plan(paths, sheetName, stageCrops);
    }

    public TextureApplyResult applyStaged(String modidFilter, boolean dryRun, boolean overwriteApproved) throws IOException {
        this.paths = TextureForgePaths.discover();
        return TextureApplyService.apply(paths, modidFilter, dryRun, overwriteApproved);
    }

    public TextureReviewState reviewState() {
        this.paths = TextureForgePaths.discover();
        return TextureReviewService.load(paths);
    }

    public TextureReviewState updateReview(String asset, TextureReviewStatus status, String notes) throws IOException {
        this.paths = TextureForgePaths.discover();
        return TextureReviewService.update(paths, asset, status, notes);
    }

    public Path exportReviewState() throws IOException {
        this.paths = TextureForgePaths.discover();
        return TextureReviewService.exportMarkdown(paths);
    }

    private static TextureSpec fallbackSpec(String namespace, String assetId, TextureKind kind) {
        return TextureSpec.builder(namespace, assetId, kind)
                .styleFamily(TextureStyleFamilies.defaultForNamespace(namespace))
                .build();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
