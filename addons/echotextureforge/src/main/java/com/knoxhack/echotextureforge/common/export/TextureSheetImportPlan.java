package com.knoxhack.echotextureforge.common.export;

import java.util.List;

public record TextureSheetImportPlan(
        String sheetName,
        String sheetPath,
        String cutMapPath,
        String stagedRoot,
        int sheetWidth,
        int sheetHeight,
        boolean sheetExists,
        boolean cutMapExists,
        boolean dimensionsValid,
        boolean staged,
        List<String> messages,
        List<TextureSheetImportCellPlan> cells) {
    public TextureSheetImportPlan {
        messages = messages == null ? List.of() : List.copyOf(messages);
        cells = cells == null ? List.of() : List.copyOf(cells);
    }
}
