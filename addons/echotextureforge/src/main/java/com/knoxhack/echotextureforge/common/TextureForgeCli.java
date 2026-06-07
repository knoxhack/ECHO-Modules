package com.knoxhack.echotextureforge.common;

import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.common.export.TextureApplyResult;
import com.knoxhack.echotextureforge.common.export.TextureSheetImportPlan;
import java.util.ArrayList;
import java.util.List;

public final class TextureForgeCli {
    private TextureForgeCli() {
    }

    public static void main(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);
        if (!options.outputDir().isBlank()) {
            System.setProperty("textureForge.outputDir", options.outputDir());
        }
        if (options.strictMode()) {
            System.setProperty("textureForge.strict", "true");
        }
        if (options.jsonOnly()) {
            System.setProperty("textureForge.jsonOnly", "true");
        }
        if (options.markdownOnly()) {
            System.setProperty("textureForge.markdownOnly", "true");
        }

        switch (options.mode()) {
            case "prompts", "sheets" -> runAudit(options, false, true);
            case "report", "reports" -> runAudit(options, true, false);
            case "validate", "scan" -> runAudit(options, true, true);
            case "import-plan" -> runImport(options, false);
            case "import-stage" -> runImport(options, true);
            case "apply-dryrun" -> runApply(options, true, false);
            default -> runAudit(options, true, true);
        }
    }

    private static void runAudit(CliOptions options, boolean reports, boolean prompts) throws Exception {
        TextureAuditReport report = TextureForgeService.INSTANCE.runAudit(options.modid(), false, reports, prompts);
        System.out.println("TextureForge " + options.mode() + " complete: "
                + report.issues().size() + " issue(s), "
                + report.totalSpecs() + " spec(s), output " + report.outputRoot());
    }

    private static void runImport(CliOptions options, boolean stage) throws Exception {
        TextureSheetImportPlan plan = TextureForgeService.INSTANCE.planImport(options.sheetName(), stage);
        long conflicts = plan.cells().stream().filter(cell -> cell.conflict()).count();
        System.out.println("TextureForge import " + (stage ? "stage" : "plan") + " complete: "
                + plan.cells().size() + " cell(s), conflicts " + conflicts
                + ", output " + TextureForgeService.INSTANCE.paths().importDir());
    }

    private static void runApply(CliOptions options, boolean dryRun, boolean overwriteApproved) throws Exception {
        TextureApplyResult result = TextureForgeService.INSTANCE.applyStaged(options.modid(), dryRun, overwriteApproved);
        System.out.println("TextureForge apply " + (dryRun ? "dry-run" : "staged") + " complete: copied="
                + result.copied() + ", skipped=" + result.skipped() + ", conflicts=" + result.conflicts()
                + ", output " + TextureForgeService.INSTANCE.paths().importDir());
    }

    private record CliOptions(
            String mode,
            String modid,
            String outputDir,
            String sheetName,
            boolean strictMode,
            boolean jsonOnly,
            boolean markdownOnly) {
        static CliOptions parse(String[] args) {
            List<String> values = args == null ? List.of() : new ArrayList<>(List.of(args));
            String mode = values.isEmpty() ? "scan" : values.removeFirst().toLowerCase(java.util.Locale.ROOT);
            String modid = property("textureForgeMod", "");
            String outputDir = "";
            String sheetName = property("textureForgeSheet", "sheet");
            boolean strict = false;
            boolean jsonOnly = false;
            boolean markdownOnly = false;
            for (int i = 0; i < values.size(); i++) {
                String arg = values.get(i);
                if ("--modid".equals(arg) && i + 1 < values.size()) {
                    modid = values.get(++i);
                } else if ("--output-dir".equals(arg) && i + 1 < values.size()) {
                    outputDir = values.get(++i);
                } else if ("--sheet".equals(arg) && i + 1 < values.size()) {
                    sheetName = values.get(++i);
                } else if ("--strict".equals(arg) || "strict".equals(arg)) {
                    strict = true;
                } else if ("--json-only".equals(arg)) {
                    jsonOnly = true;
                } else if ("--markdown-only".equals(arg)) {
                    markdownOnly = true;
                } else if (!arg.startsWith("--") && ("import-plan".equals(mode) || "import-stage".equals(mode))) {
                    sheetName = arg;
                } else if (!arg.startsWith("--") && modid.isBlank()) {
                    modid = arg;
                }
            }
            return new CliOptions(mode, modid == null ? "" : modid, outputDir == null ? "" : outputDir,
                    sheetName == null || sheetName.isBlank() ? "sheet" : sheetName, strict, jsonOnly, markdownOnly);
        }

        private static String property(String key, String fallback) {
            String value = System.getProperty(key, "");
            return value.isBlank() ? fallback : value;
        }
    }
}
