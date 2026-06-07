package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record MultiblockBuildAssistSnapshot(
        Identifier definitionId,
        String displayName,
        int width,
        int height,
        int depth,
        BlockPos controllerLocalPos,
        int previewColor,
        boolean rotationsAllowed,
        boolean mirrorable,
        boolean complete,
        String warning,
        List<MultiblockBuildAssistCell> cells,
        MultiblockMaterialSummary materials) {
    public MultiblockBuildAssistSnapshot {
        if (definitionId == null) {
            throw new IllegalArgumentException("Build assist definition id is required.");
        }
        displayName = displayName == null || displayName.isBlank() ? definitionId.toString() : displayName.strip();
        width = Math.max(1, width);
        height = Math.max(1, height);
        depth = Math.max(1, depth);
        controllerLocalPos = controllerLocalPos == null ? new BlockPos(width / 2, 0, depth / 2) : controllerLocalPos;
        warning = warning == null ? "" : warning.strip();
        cells = List.copyOf(cells == null ? List.of() : cells);
        materials = materials == null ? MultiblockMaterialSummary.empty(definitionId) : materials;
    }

    public static MultiblockBuildAssistSnapshot from(MultiblockDefinition definition, int maxVolume) {
        boolean oversized = definition.volume() > maxVolume;
        List<MultiblockBuildAssistCell> cells = oversized ? List.of() : cells(definition);
        return new MultiblockBuildAssistSnapshot(
                definition.id(),
                definition.displayName(),
                definition.width(),
                definition.height(),
                definition.depth(),
                definition.controllerLocalPosition().orElseGet(() -> new BlockPos(definition.width() / 2, 0, definition.depth() / 2)),
                definition.previewColor(),
                definition.allowedRotations(),
                definition.mirrorable(),
                !oversized,
                oversized ? "Definition volume " + definition.volume() + " exceeds build-assist sync limit " + maxVolume + "." : "",
                cells,
                MultiblockMaterialSummary.from(definition));
    }

    private static List<MultiblockBuildAssistCell> cells(MultiblockDefinition definition) {
        java.util.ArrayList<MultiblockBuildAssistCell> cells = new java.util.ArrayList<>();
        for (int y = 0; y < definition.height(); y++) {
            for (int z = 0; z < definition.depth(); z++) {
                for (int x = 0; x < definition.width(); x++) {
                    StructureBlockRequirement requirement = definition.requirementAt(x, y, z);
                    cells.add(MultiblockBuildAssistCell.from(new BlockPos(x, y, z), requirement));
                }
            }
        }
        return cells;
    }
}
