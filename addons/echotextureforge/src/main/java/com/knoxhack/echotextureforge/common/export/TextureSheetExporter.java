package com.knoxhack.echotextureforge.common.export;

import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.common.util.TextureForgeJson;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TextureSheetExporter {
    private static final int DEFAULT_COLUMNS = 4;
    private static final int DEFAULT_CELL_SIZE = 32;
    private static final int DEFAULT_PADDING = 0;
    private static final int MAX_CELLS = 16;

    private TextureSheetExporter() {
    }

    public static TextureSheetPlan plan(String name, String sheetType, List<TextureSpec> specs) {
        List<TextureSpec> limited = specs.stream().limit(MAX_CELLS).toList();
        int columns = DEFAULT_COLUMNS;
        int rows = Math.max(1, (int) Math.ceil(limited.size() / (double) columns));
        int cellStep = DEFAULT_CELL_SIZE + DEFAULT_PADDING;
        List<TextureSheetCell> cells = new ArrayList<>();
        for (int i = 0; i < limited.size(); i++) {
            TextureSpec spec = limited.get(i);
            int row = i / columns;
            int column = i % columns;
            cells.add(new TextureSheetCell(
                    spec.assetId(),
                    spec.namespace(),
                    spec.assetKind().id(),
                    spec.textureType().id(),
                    spec.styleFamily().name(),
                    row,
                    column,
                    column * cellStep,
                    row * cellStep,
                    spec.expectedResolution().width(),
                    spec.expectedResolution().height(),
                    "assets/" + spec.namespace() + "/" + spec.outputPath(),
                    spec.expectedResolution().id()));
        }
        String style = limited.isEmpty() ? "ASHFALL_SURVIVAL" : limited.getFirst().styleFamily().name();
        return new TextureSheetPlan(name, columns, rows, DEFAULT_CELL_SIZE, DEFAULT_CELL_SIZE, DEFAULT_PADDING,
                columns * DEFAULT_CELL_SIZE, rows * DEFAULT_CELL_SIZE, style, sheetType, cells);
    }

    public static TextureSheetPlan plan(String name, List<TextureSpec> specs) {
        return plan(name, "mixed", specs);
    }

    public static String prompt(TextureSheetPlan plan) {
        StringBuilder out = new StringBuilder();
        out.append("Generate one transparent texture sheet for Minecraft mod textures.\n\n");
        out.append("Sheet:\n");
        out.append("- Name: ").append(plan.name()).append('\n');
        out.append("- Sheet Type: ").append(plan.sheetType()).append('\n');
        out.append("- Grid Size: ").append(plan.columns()).append(" columns x ").append(plan.rows()).append(" rows\n");
        out.append("- Cell Size: ").append(plan.cellWidth()).append("x").append(plan.cellHeight()).append(" pixels\n");
        out.append("- Cell Padding: ").append(plan.cellPadding()).append(" pixels\n");
        out.append("- Expected Sheet Size: ").append(plan.sheetWidth()).append("x").append(plan.sheetHeight()).append(" pixels\n");
        out.append("- Style Family: ").append(plan.styleFamily()).append("\n\n");
        out.append("Numbered Cell List:\n");
        for (TextureSheetCell cell : plan.cells()) {
            out.append(cell.row() * plan.columns() + cell.column() + 1)
                    .append(". row ").append(cell.row())
                    .append(", column ").append(cell.column())
                    .append(" -> ").append(cell.namespace()).append(':').append(cell.assetId())
                    .append(" (").append(cell.assetKind()).append('/').append(cell.textureType()).append(")")
                    .append(" -> ").append(cell.outputPath()).append('\n');
        }
        out.append("""

                Hard Requirements:
                - transparent sheet background
                - each cell contains exactly one isolated texture
                - 32x32 pixels per cell unless the cut map says otherwise
                - no labels inside cells
                - no shared lighting background
                - no UI frame unless the cell is a UI texture
                - no background scene
                - no fake 3D render
                - Minecraft-style pixel art
                - clean silhouettes readable at inventory size
                - keep cell order exactly as listed

                Return:
                - one transparent PNG sheet only
                - no mockup
                - no labels inside cells
                - use the matching cut map JSON for cropping
                """);
        out.append("\nMatching Cut Map JSON:\n");
        out.append(TextureForgeMarkdown.codeFence("json", TextureForgeJson.GSON.toJson(plan)));
        return out.toString();
    }

    public static Path writePlan(Path markdownPath, Path jsonPath, TextureSheetPlan plan) throws IOException {
        TextureForgeMarkdown.write(markdownPath, TextureForgeMarkdown.heading(plan.name()) + prompt(plan));
        TextureForgeJson.write(jsonPath, plan);
        return markdownPath;
    }
}
