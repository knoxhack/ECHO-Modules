package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record LensMultiblockScan(
        Identifier targetId,
        String structureName,
        MultiblockState state,
        double completion,
        BlockPos targetPos,
        List<String> missingBlocks,
        List<String> roboticStatus,
        List<String> taskQueue) {
    public LensMultiblockScan {
        structureName = structureName == null || structureName.isBlank() ? "Multiblock" : structureName.strip();
        state = state == null ? MultiblockState.UNBUILT : state;
        completion = Math.max(0.0D, Math.min(1.0D, completion));
        targetPos = targetPos == null ? BlockPos.ZERO : targetPos.immutable();
        missingBlocks = List.copyOf(missingBlocks == null ? List.of() : missingBlocks);
        roboticStatus = List.copyOf(roboticStatus == null ? List.of() : roboticStatus);
        taskQueue = List.copyOf(taskQueue == null ? List.of() : taskQueue);
    }
}
