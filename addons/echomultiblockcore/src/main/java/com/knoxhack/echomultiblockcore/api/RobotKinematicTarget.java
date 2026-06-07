package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;

public record RobotKinematicTarget(BlockPos origin, BlockPos target, int durationTicks, String animationId) {
    public RobotKinematicTarget {
        origin = origin == null ? BlockPos.ZERO : origin.immutable();
        target = target == null ? origin : target.immutable();
        durationTicks = Math.max(1, durationTicks);
        animationId = animationId == null || animationId.isBlank() ? "move_to_target" : animationId.strip();
    }
}
