package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record RobotAnimationState(
        Identifier robotId,
        BlockPos robotPos,
        BlockPos targetPos,
        String animationId,
        int durationTicks,
        int elapsedTicks,
        RobotPoseSnapshot pose,
        Identifier taskId) {
    public RobotAnimationState {
        robotId = robotId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown_robot") : robotId;
        robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        targetPos = targetPos == null ? robotPos : targetPos.immutable();
        animationId = animationId == null || animationId.isBlank() ? "idle" : animationId.strip();
        durationTicks = Math.max(1, durationTicks);
        elapsedTicks = Math.max(0, elapsedTicks);
        pose = pose == null ? RobotPoseSnapshot.idle() : pose;
        taskId = taskId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "idle") : taskId;
    }

    public double progress() {
        return Math.min(1.0D, elapsedTicks / (double) durationTicks);
    }
}
