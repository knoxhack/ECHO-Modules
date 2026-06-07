package com.knoxhack.echotextureforge.common.export;

import java.util.List;

public record TextureSheetPlan(
        String name,
        int columns,
        int rows,
        int cellWidth,
        int cellHeight,
        int cellPadding,
        int sheetWidth,
        int sheetHeight,
        String styleFamily,
        String sheetType,
        List<TextureSheetCell> cells) {
    public TextureSheetPlan {
        name = name == null || name.isBlank() ? "texture_sheet" : name;
        columns = Math.max(1, columns);
        rows = Math.max(1, rows);
        cellWidth = Math.max(1, cellWidth);
        cellHeight = Math.max(1, cellHeight);
        cellPadding = Math.max(0, cellPadding);
        sheetWidth = sheetWidth <= 0 ? columns * cellWidth : sheetWidth;
        sheetHeight = sheetHeight <= 0 ? rows * cellHeight : sheetHeight;
        styleFamily = styleFamily == null || styleFamily.isBlank() ? "ASHFALL_SURVIVAL" : styleFamily;
        sheetType = sheetType == null || sheetType.isBlank() ? "mixed" : sheetType;
        cells = cells == null ? List.of() : List.copyOf(cells);
    }
}
