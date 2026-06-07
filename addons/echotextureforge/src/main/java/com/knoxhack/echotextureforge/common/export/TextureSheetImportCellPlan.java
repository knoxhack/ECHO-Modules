package com.knoxhack.echotextureforge.common.export;

public record TextureSheetImportCellPlan(
        String namespace,
        String assetId,
        String assetKind,
        String textureType,
        int row,
        int column,
        int x,
        int y,
        int width,
        int height,
        String outputPath,
        String stagedPath,
        String targetPath,
        boolean targetExists,
        boolean conflict,
        String status,
        String message) {
}
