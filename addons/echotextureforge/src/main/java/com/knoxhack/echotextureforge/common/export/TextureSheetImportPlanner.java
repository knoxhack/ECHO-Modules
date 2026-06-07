package com.knoxhack.echotextureforge.common.export;

import com.knoxhack.echotextureforge.common.review.TextureReviewEntry;
import com.knoxhack.echotextureforge.common.review.TextureReviewService;
import com.knoxhack.echotextureforge.common.review.TextureReviewStatus;
import com.knoxhack.echotextureforge.common.util.TextureForgeJson;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public final class TextureSheetImportPlanner {
    private TextureSheetImportPlanner() {
    }

    public static TextureSheetImportPlan plan(TextureForgePaths paths, String sheetName, boolean stageCrops) throws IOException {
        String baseName = baseName(sheetName);
        Path sheetPath = paths.importIncomingDir().resolve(baseName + ".png");
        Path cutMapPath = paths.importIncomingDir().resolve(baseName + ".cut_map.json");
        List<String> messages = new ArrayList<>();
        boolean sheetExists = Files.isRegularFile(sheetPath);
        boolean cutMapExists = Files.isRegularFile(cutMapPath);
        if (!sheetExists) {
            messages.add("Incoming sheet image is missing: " + sheetPath);
        }
        if (!cutMapExists) {
            messages.add("Incoming cut map is missing: " + cutMapPath);
        }

        TextureSheetPlan cutMap = cutMapExists ? readCutMap(cutMapPath) : new TextureSheetPlan(baseName, 4, 1,
                32, 32, 0, 128, 32, "ASHFALL_SURVIVAL", "missing", List.of());
        BufferedImage sheet = null;
        int sheetWidth = 0;
        int sheetHeight = 0;
        if (sheetExists) {
            try {
                sheet = ImageIO.read(sheetPath.toFile());
                if (sheet == null) {
                    messages.add("Incoming sheet image could not be decoded by ImageIO.");
                } else {
                    sheetWidth = sheet.getWidth();
                    sheetHeight = sheet.getHeight();
                }
            } catch (IOException | RuntimeException exception) {
                messages.add("Incoming sheet image could not be read: " + exception.getMessage());
            }
        }

        boolean dimensionsValid = sheet != null && sheetWidth >= cutMap.sheetWidth() && sheetHeight >= cutMap.sheetHeight();
        if (sheet != null && !dimensionsValid) {
            messages.add("Sheet dimensions " + sheetWidth + "x" + sheetHeight + " are smaller than cut map "
                    + cutMap.sheetWidth() + "x" + cutMap.sheetHeight() + ".");
        }

        List<TextureSheetImportCellPlan> cells = new ArrayList<>();
        List<TextureReviewEntry> reviewEntries = new ArrayList<>();
        for (TextureSheetCell cell : cutMap.cells()) {
            CellTarget target = target(paths, cell);
            int width = cell.width() <= 0 ? cutMap.cellWidth() : cell.width();
            int height = cell.height() <= 0 ? cutMap.cellHeight() : cell.height();
            int x = cell.x() > 0 ? cell.x() : cell.column() * (cutMap.cellWidth() + cutMap.cellPadding());
            int y = cell.y() > 0 ? cell.y() : cell.row() * (cutMap.cellHeight() + cutMap.cellPadding());
            boolean inBounds = sheet != null && x >= 0 && y >= 0
                    && x + width <= sheetWidth && y + height <= sheetHeight;
            String status = inBounds ? "ready" : "invalid";
            String message = inBounds ? "Cell can be cropped." : "Cell crop rectangle is outside the sheet bounds.";
            if (target.targetExists()) {
                status = "conflict";
                message = "Target already exists; apply will skip unless overwrite-approved is used.";
            }
            Path stagedPath = paths.importStagedDir().resolve(target.namespace()).resolve(target.relativePath());
            if (stageCrops && inBounds && sheet != null) {
                Files.createDirectories(stagedPath.getParent());
                BufferedImage crop = sheet.getSubimage(x, y, width, height);
                ImageIO.write(crop, "png", stagedPath.toFile());
                status = target.targetExists() ? "staged_conflict" : "staged";
                message = target.targetExists()
                        ? "Cropped into staged output; source target still has a conflict."
                        : "Cropped into staged output and ready for review.";
                reviewEntries.add(new TextureReviewEntry(target.namespace() + ":" + cell.assetId(),
                        stagedPath.toString(), "assets/" + target.namespace() + "/" + target.relativePath(),
                        TextureReviewStatus.PENDING, "", Instant.now().toString(), baseName, ""));
            }
            cells.add(new TextureSheetImportCellPlan(target.namespace(), cell.assetId(), cell.assetKind(), cell.textureType(),
                    cell.row(), cell.column(), x, y, width, height,
                    "assets/" + target.namespace() + "/" + target.relativePath(), stagedPath.toString(),
                    target.targetPath().toString(), target.targetExists(), target.targetExists(), status, message));
        }
        if (!reviewEntries.isEmpty()) {
            TextureReviewService.upsertAll(paths, reviewEntries);
        }

        TextureSheetImportPlan plan = new TextureSheetImportPlan(baseName, sheetPath.toString(), cutMapPath.toString(),
                paths.importStagedDir().toString(), sheetWidth, sheetHeight, sheetExists, cutMapExists, dimensionsValid,
                stageCrops, messages, cells);
        writeOutputs(paths, baseName, plan);
        return plan;
    }

    private static TextureSheetPlan readCutMap(Path path) throws IOException {
        TextureSheetPlan plan = TextureForgeJson.read(path, TextureSheetPlan.class);
        if (plan == null) {
            return new TextureSheetPlan(path.getFileName().toString(), 4, 1, 32, 32, 0,
                    128, 32, "ASHFALL_SURVIVAL", "empty", List.of());
        }
        return plan;
    }

    private static void writeOutputs(TextureForgePaths paths, String baseName, TextureSheetImportPlan plan) throws IOException {
        TextureForgeJson.write(paths.importDir().resolve("import_plan.json"), plan);
        TextureForgeJson.write(paths.reviewDir().resolve("import_plan.json"), plan);
        TextureForgeJson.write(paths.importPreviewDir().resolve(baseName + "_preview.json"), plan);
        TextureForgeMarkdown.write(paths.importDir().resolve("import_report.md"), importMarkdown(plan));
    }

    private static String importMarkdown(TextureSheetImportPlan plan) {
        StringBuilder out = new StringBuilder(TextureForgeMarkdown.heading("TextureForge Import Plan"));
        out.append("- Sheet: `").append(plan.sheetPath()).append("`\n");
        out.append("- Cut map: `").append(plan.cutMapPath()).append("`\n");
        out.append("- Staged root: `").append(plan.stagedRoot()).append("`\n");
        out.append("- Sheet exists: ").append(plan.sheetExists()).append('\n');
        out.append("- Cut map exists: ").append(plan.cutMapExists()).append('\n');
        out.append("- Dimensions valid: ").append(plan.dimensionsValid()).append('\n');
        out.append("- Stage crops: ").append(plan.staged()).append("\n\n");
        if (!plan.messages().isEmpty()) {
            out.append("## Messages\n\n");
            plan.messages().forEach(message -> out.append("- ").append(message).append('\n'));
            out.append('\n');
        }
        out.append("## Cells\n\n");
        out.append("| Status | Asset | Crop | Staged | Target | Message |\n");
        out.append("| --- | --- | --- | --- | --- | --- |\n");
        for (TextureSheetImportCellPlan cell : plan.cells()) {
            out.append("| ").append(cell.status())
                    .append(" | `").append(cell.namespace()).append(':').append(cell.assetId()).append("`")
                    .append(" | `").append(cell.x()).append(',').append(cell.y()).append(' ')
                    .append(cell.width()).append('x').append(cell.height()).append("`")
                    .append(" | `").append(cell.stagedPath()).append("`")
                    .append(" | `").append(cell.targetPath()).append("`")
                    .append(" | ").append(cell.message().replace("|", "\\|")).append(" |\n");
        }
        return out.toString();
    }

    private static CellTarget target(TextureForgePaths paths, TextureSheetCell cell) {
        String output = cell.outputPath() == null ? "" : cell.outputPath().replace('\\', '/');
        String namespace = cell.namespace() == null || cell.namespace().isBlank() ? namespaceFromOutput(output) : cell.namespace();
        String relative = relativeFromOutput(namespace, output);
        Path target = paths.sourceAssetPath(namespace, relative);
        return new CellTarget(namespace, relative, target, Files.exists(target));
    }

    private static String namespaceFromOutput(String output) {
        String value = output.startsWith("assets/") ? output.substring("assets/".length()) : output;
        int slash = value.indexOf('/');
        return slash > 0 ? value.substring(0, slash) : "unknown";
    }

    private static String relativeFromOutput(String namespace, String output) {
        String prefix = "assets/" + namespace + "/";
        if (output.startsWith(prefix)) {
            return output.substring(prefix.length());
        }
        return output.startsWith("assets/") ? output.substring("assets/".length()) : output;
    }

    private static String baseName(String sheetName) {
        String clean = sheetName == null || sheetName.isBlank() ? "sheet" : sheetName.strip();
        clean = clean.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) {
            clean = clean.substring(slash + 1);
        }
        if (clean.endsWith(".png")) {
            clean = clean.substring(0, clean.length() - 4);
        }
        if (clean.endsWith(".cut_map.json")) {
            clean = clean.substring(0, clean.length() - ".cut_map.json".length());
        }
        return clean.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private record CellTarget(String namespace, String relativePath, Path targetPath, boolean targetExists) {
    }
}
