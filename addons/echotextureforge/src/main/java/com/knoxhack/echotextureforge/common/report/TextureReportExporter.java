package com.knoxhack.echotextureforge.common.report;

import com.knoxhack.echotextureforge.api.report.TextureAuditIssue;
import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureStyleFamily;
import com.knoxhack.echotextureforge.common.util.TextureForgeJson;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TextureReportExporter {
    private TextureReportExporter() {
    }

    public static List<Path> export(TextureAuditReport report, TextureForgePaths paths) throws IOException {
        List<Path> files = new ArrayList<>();
        Path reports = paths.reportsDir();
        boolean jsonOnly = Boolean.getBoolean("textureForge.jsonOnly");
        boolean markdownOnly = Boolean.getBoolean("textureForge.markdownOnly");

        Path auditMd = reports.resolve("texture_audit.md");
        Path auditJson = reports.resolve("texture_audit.json");
        Path summaryMd = reports.resolve("summary.md");
        Path summaryJson = reports.resolve("summary.json");
        Path compatSummaryMd = reports.resolve("textureforge_summary.md");

        if (!jsonOnly) {
            TextureForgeMarkdown.write(auditMd, auditMarkdown(report));
            TextureForgeMarkdown.write(summaryMd, summaryMarkdown(report));
            TextureForgeMarkdown.write(compatSummaryMd, summaryMarkdown(report));
            files.addAll(List.of(auditMd, summaryMd, compatSummaryMd));
        }
        if (!markdownOnly) {
            TextureForgeJson.write(auditJson, report);
            TextureForgeJson.write(summaryJson, summaryJson(report));
            files.addAll(List.of(auditJson, summaryJson));
        }

        if (!markdownOnly) {
            files.add(writeIssueJson(reports.resolve("missing_assets.json"), report, issue -> issue.code().startsWith("MISSING_")));
            files.add(writeIssueJson(reports.resolve("wrong_size_textures.json"), report,
                    issue -> "WRONG_TEXTURE_SIZE".equals(issue.code()) || "NOT_POWER_OF_TWO".equals(issue.code())));
            files.add(writeIssueJson(reports.resolve("model_reference_errors.json"), report,
                    issue -> issue.code().contains("MODEL") || "BROKEN_JSON".equals(issue.code())));
            files.add(writeIssueJson(reports.resolve("naming_errors.json"), report,
                    issue -> issue.code().contains("NAME") || issue.code().contains("PATH")
                            || issue.code().contains("PLACEHOLDER") || issue.code().contains("ILLEGAL")));

            files.add(writeIssueJson(paths.reportIssuesDir().resolve("critical.json"), report,
                    issue -> issue.severity() == TextureAuditSeverity.CRITICAL));
            files.add(writeIssueJson(paths.reportIssuesDir().resolve("warnings.json"), report,
                    issue -> issue.severity() == TextureAuditSeverity.WARNING));
            files.add(writeIssueJson(paths.reportIssuesDir().resolve("info.json"), report,
                    issue -> issue.severity() == TextureAuditSeverity.INFO));
        }

        for (Map.Entry<String, List<TextureAuditIssue>> entry : issuesByNamespace(report).entrySet()) {
            TextureAuditReport addon = filteredReport(report, entry.getKey(), entry.getValue());
            Path md = paths.reportAddonDir().resolve(entry.getKey() + "_audit.md");
            Path json = paths.reportAddonDir().resolve(entry.getKey() + "_audit.json");
            if (!jsonOnly) {
                TextureForgeMarkdown.write(md, auditMarkdown(addon));
                files.add(md);
            }
            if (!markdownOnly) {
                TextureForgeJson.write(json, addon);
                files.add(json);
            }
        }

        if (!markdownOnly) {
            for (Map.Entry<String, List<TextureSpec>> entry : specsByNamespace(report.specs()).entrySet()) {
                files.add(writeJson(paths.specsMergedDir().resolve(entry.getKey() + "_merged_specs.json"), entry.getValue()));
                files.add(writeJson(paths.specsGeneratedDir().resolve(entry.getKey() + "_generated_specs.json"),
                        entry.getValue().stream().filter(spec -> !spec.sourceRegistryId().startsWith("manual:")).toList()));
            }
        }

        if (!jsonOnly) {
            Path styleMd = reports.resolve("style_families.md");
            TextureForgeMarkdown.write(styleMd, styleFamiliesMarkdown(report));
            files.add(styleMd);
        }
        return List.copyOf(files);
    }

    private static Path writeIssueJson(Path path, TextureAuditReport report,
                                       Predicate<TextureAuditIssue> filter) throws IOException {
        return writeJson(path, report.issues().stream().filter(filter).toList());
    }

    private static Path writeJson(Path path, Object value) throws IOException {
        TextureForgeJson.write(path, value);
        return path;
    }

    private static String auditMarkdown(TextureAuditReport report) {
        StringBuilder out = new StringBuilder();
        out.append(TextureForgeMarkdown.heading("TextureForge Audit"));
        out.append(summaryBlock(report));
        out.append("## What To Fix First\n\n");
        out.append(actionSection(report));
        out.append("## Severity\n\n");
        out.append("| Severity | Count |\n| --- | ---: |\n");
        for (TextureAuditSeverity severity : TextureAuditSeverity.values()) {
            out.append("| ").append(severity).append(" | ")
                    .append(report.severitySummary().getOrDefault(severity, 0)).append(" |\n");
        }
        out.append("\n## Per Kind\n\n");
        out.append(kindSummary(report));
        out.append("\n## Issues\n\n");
        if (report.issues().isEmpty()) {
            out.append("No issues detected.\n");
        } else {
            out.append("| Severity | Code | Asset | Path | Message |\n| --- | --- | --- | --- | --- |\n");
            for (TextureAuditIssue issue : report.issues()) {
                out.append("| ").append(issue.severity())
                        .append(" | `").append(issue.code()).append("`")
                        .append(" | `").append(issue.namespace()).append(':').append(issue.assetId()).append("`")
                        .append(" | `").append(issue.path()).append("`")
                        .append(" | ").append(escape(issue.message())).append(" |\n");
            }
        }
        out.append("\n## Next 20 Textures To Generate\n\n");
        nextTextureSpecs(report, 20).forEach(spec -> out.append("- `")
                .append(spec.namespace()).append(':').append(spec.assetId()).append("` ")
                .append(spec.assetKind().id()).append('/').append(spec.textureType().id())
                .append(" -> `assets/").append(spec.namespace()).append('/').append(spec.outputPath()).append("`\n"));
        out.append("\n## Best Sheet Batches To Create\n\n");
        sheetBatchLines(report).forEach(line -> out.append("- ").append(line).append('\n'));
        return out.toString();
    }

    private static String actionSection(TextureAuditReport report) {
        StringBuilder out = new StringBuilder();
        out.append("- Critical blockers first: ").append(report.issues(TextureAuditSeverity.CRITICAL).size()).append('\n');
        out.append("- Items with no inventory texture: ").append(count(report, "MISSING_TEXTURE", TextureKind.ITEM)).append('\n');
        out.append("- Blocks broken in-game: ").append(count(report, "MISSING_BLOCKSTATE") + count(report, "MISSING_BLOCK_MODEL")).append('\n');
        out.append("- Models blocked by missing textures: ").append(count(report, "MODEL_TEXTURE_MISSING")).append('\n');
        out.append("- Machines missing active/inactive faces: ").append(count(report, "MISSING_MACHINE_ACTIVE_VARIANT")).append('\n');
        out.append("- UI icons missing: ").append(count(report, "MISSING_TEXTURE", TextureKind.UI)).append('\n');
        out.append("- Textures wrong size: ").append(report.wrongSizeTextures()).append('\n');
        out.append("- Files safe to apply: see `review/import_plan.json` and `import/import_report.md` after staging.\n");
        out.append("- Files with conflicts: see `import/apply_report.md` after dry-run or apply.\n\n");
        return out.toString();
    }

    private static String summaryMarkdown(TextureAuditReport report) {
        return TextureForgeMarkdown.heading("TextureForge Summary")
                + summaryBlock(report)
                + "## Output\n\n"
                + "- Reports: `" + report.outputRoot().resolve("reports") + "`\n"
                + "- Prompts: `" + report.outputRoot().resolve("prompts") + "`\n"
                + "- Import staging: `" + report.outputRoot().resolve("import") + "`\n"
                + "- Review state: `" + report.outputRoot().resolve("review/review_state.json") + "`\n";
    }

    private static String summaryBlock(TextureAuditReport report) {
        return """
                ## Totals

                | Metric | Count |
                | --- | ---: |
                | Scanned modules | %d |
                | Registered items | %d |
                | Registered blocks | %d |
                | Texture specs | %d |
                | Texture files | %d |
                | Item models | %d |
                | Block models | %d |
                | Blockstates | %d |
                | Missing textures | %d |
                | Missing models | %d |
                | Missing blockstates | %d |
                | Missing lang keys | %d |
                | Wrong-size textures | %d |
                | Unused textures | %d |

                """.formatted(
                report.totalScannedAddons(),
                report.totalRegisteredItems(),
                report.totalRegisteredBlocks(),
                report.totalSpecs(),
                report.totalTextures(),
                report.totalItemModels(),
                report.totalBlockModels(),
                report.totalBlockstates(),
                report.missingTextures(),
                report.missingModels(),
                report.missingBlockstates(),
                report.missingLangKeys(),
                report.wrongSizeTextures(),
                report.unusedTextures());
    }

    private static Map<String, Object> summaryJson(TextureAuditReport report) {
        Map<String, Object> out = new TreeMap<>();
        out.put("generatedAt", report.generatedAt().toString());
        out.put("workspaceRoot", report.workspaceRoot().toString());
        out.put("outputRoot", report.outputRoot().toString());
        out.put("totalScannedAddons", report.totalScannedAddons());
        out.put("totalSpecs", report.totalSpecs());
        out.put("missingTextures", report.missingTextures());
        out.put("missingModels", report.missingModels());
        out.put("missingBlockstates", report.missingBlockstates());
        out.put("missingLangKeys", report.missingLangKeys());
        out.put("wrongSizeTextures", report.wrongSizeTextures());
        out.put("unusedTextures", report.unusedTextures());
        out.put("severitySummary", report.severitySummary());
        return out;
    }

    private static String kindSummary(TextureAuditReport report) {
        Map<TextureKind, Long> counts = report.specs().stream()
                .collect(Collectors.groupingBy(TextureSpec::assetKind, () -> new EnumMap<>(TextureKind.class), Collectors.counting()));
        StringBuilder out = new StringBuilder("| Kind | Specs |\n| --- | ---: |\n");
        for (TextureKind kind : TextureKind.values()) {
            out.append("| ").append(kind.id()).append(" | ").append(counts.getOrDefault(kind, 0L)).append(" |\n");
        }
        return out.toString();
    }

    private static String styleFamiliesMarkdown(TextureAuditReport report) {
        StringBuilder out = new StringBuilder(TextureForgeMarkdown.heading("TextureForge Style Families"));
        Map<TextureStyleFamily, Long> usage = report.specs().stream()
                .collect(Collectors.groupingBy(TextureSpec::styleFamily,
                        () -> new EnumMap<>(TextureStyleFamily.class), Collectors.counting()));
        for (TextureStyleFamily family : TextureStyleFamily.values()) {
            out.append("## ").append(family.name()).append("\n\n");
            out.append("- Specs using family: ").append(usage.getOrDefault(family, 0L)).append('\n');
            out.append("- Visual direction: ").append(family.visualDirection()).append('\n');
            out.append("- Palette hints: ").append(String.join(", ", family.paletteHints())).append('\n');
            out.append("- Material hints: ").append(String.join(", ", family.materialHints())).append('\n');
            out.append("- Lighting rules: ").append(family.lightingRules()).append('\n');
            out.append("- Shape language: ").append(family.shapeLanguage()).append('\n');
            out.append("- Avoid: ").append(String.join(", ", family.avoidList())).append('\n');
            out.append("- Example direction: ").append(family.exampleDirection()).append("\n\n");
        }
        return out.toString();
    }

    private static List<TextureSpec> nextTextureSpecs(TextureAuditReport report, int limit) {
        var missing = report.issues().stream()
                .filter(issue -> "MISSING_TEXTURE".equals(issue.code()))
                .map(issue -> issue.namespace() + ":" + issue.assetId())
                .collect(Collectors.toSet());
        return report.specs().stream()
                .filter(spec -> missing.contains(spec.namespace() + ":" + spec.assetId()))
                .sorted(Comparator.comparingInt(TextureSpec::promptPriority).reversed()
                        .thenComparing(TextureSpec::namespace)
                        .thenComparing(spec -> spec.assetKind().id())
                        .thenComparing(TextureSpec::assetId))
                .limit(limit)
                .toList();
    }

    private static List<String> sheetBatchLines(TextureAuditReport report) {
        return nextTextureSpecs(report, 80).stream()
                .collect(Collectors.groupingBy(spec -> spec.namespace() + "/" + spec.sheetGroup(),
                        TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> "`" + entry.getKey() + "`: " + Math.min(16, entry.getValue()) + " texture(s) for next sheet")
                .toList();
    }

    private static Map<String, List<TextureAuditIssue>> issuesByNamespace(TextureAuditReport report) {
        return report.issues().stream().collect(Collectors.groupingBy(TextureAuditIssue::namespace, TreeMap::new, Collectors.toList()));
    }

    private static Map<String, List<TextureSpec>> specsByNamespace(List<TextureSpec> specs) {
        return specs.stream().collect(Collectors.groupingBy(TextureSpec::namespace, TreeMap::new, Collectors.toList()));
    }

    private static TextureAuditReport filteredReport(TextureAuditReport report, String namespace, List<TextureAuditIssue> issues) {
        List<TextureSpec> specs = report.specs().stream().filter(spec -> spec.namespace().equals(namespace)).toList();
        Map<TextureAuditSeverity, Integer> severities = new EnumMap<>(TextureAuditSeverity.class);
        for (TextureAuditSeverity severity : TextureAuditSeverity.values()) {
            severities.put(severity, (int) issues.stream().filter(issue -> issue.severity() == severity).count());
        }
        return new TextureAuditReport(report.generatedAt(), report.workspaceRoot(), report.outputRoot(),
                report.totalScannedAddons(), report.totalRegisteredItems(), report.totalRegisteredBlocks(), specs.size(),
                specs.size(), 0, 0, 0,
                count(issues, "MISSING_TEXTURE"),
                count(issues, "MISSING_ITEM_MODEL") + count(issues, "MISSING_BLOCK_MODEL"),
                count(issues, "MISSING_BLOCKSTATE"),
                count(issues, "MISSING_LANG_KEY"),
                count(issues, "WRONG_TEXTURE_SIZE"),
                count(issues, "UNUSED_TEXTURE"),
                severities, issues, specs, report.promptFiles(), report.reportFiles());
    }

    private static int count(TextureAuditReport report, String code) {
        return count(report.issues(), code);
    }

    private static int count(TextureAuditReport report, String code, TextureKind kind) {
        return (int) report.issues().stream()
                .filter(issue -> code.equals(issue.code()) && issue.assetKind() == kind)
                .count();
    }

    private static int count(List<TextureAuditIssue> issues, String code) {
        return (int) issues.stream().filter(issue -> code.equals(issue.code())).count();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }

    public static TextureAuditReport withFiles(TextureAuditReport report, List<Path> promptFiles, List<Path> reportFiles) {
        return new TextureAuditReport(report.generatedAt(), report.workspaceRoot(), report.outputRoot(),
                report.totalScannedAddons(), report.totalRegisteredItems(), report.totalRegisteredBlocks(),
                report.totalSpecs(), report.totalTextures(), report.totalItemModels(), report.totalBlockModels(),
                report.totalBlockstates(), report.missingTextures(), report.missingModels(), report.missingBlockstates(),
                report.missingLangKeys(), report.wrongSizeTextures(), report.unusedTextures(),
                report.severitySummary(), report.issues(), report.specs(), promptFiles, reportFiles);
    }
}
