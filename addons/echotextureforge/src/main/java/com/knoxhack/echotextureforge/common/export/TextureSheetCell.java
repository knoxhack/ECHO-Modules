package com.knoxhack.echotextureforge.common.export;

public record TextureSheetCell(
        String assetId,
        String namespace,
        String assetKind,
        String textureType,
        String styleFamily,
        int row,
        int column,
        int x,
        int y,
        int width,
        int height,
        String outputPath,
        String expectedSize) {
}
