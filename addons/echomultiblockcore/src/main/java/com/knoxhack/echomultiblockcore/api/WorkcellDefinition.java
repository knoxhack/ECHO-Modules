package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record WorkcellDefinition(
        Identifier id,
        WorkcellType type,
        BlockPos localPosition,
        BlockPos size,
        List<Identifier> requiredBlocks,
        List<Identifier> allowedTaskTypes,
        List<RobotToolType> requiredRobotTools,
        String status) {
    public WorkcellDefinition {
        id = id == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown_workcell") : id;
        type = type == null ? WorkcellType.ASSEMBLY : type;
        localPosition = localPosition == null ? BlockPos.ZERO : localPosition.immutable();
        size = size == null ? new BlockPos(1, 1, 1) : size.immutable();
        requiredBlocks = List.copyOf(requiredBlocks == null ? List.of() : requiredBlocks);
        allowedTaskTypes = List.copyOf(allowedTaskTypes == null ? List.of() : allowedTaskTypes);
        requiredRobotTools = List.copyOf(requiredRobotTools == null ? List.of() : requiredRobotTools);
        status = status == null || status.isBlank() ? "Idle" : status.strip();
    }
}
