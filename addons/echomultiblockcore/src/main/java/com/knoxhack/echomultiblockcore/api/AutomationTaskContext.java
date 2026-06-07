package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public record AutomationTaskContext(
        ServerLevel level,
        MultiblockControllerBlockEntity controller,
        BlockPos controllerPos,
        BlockPos robotPos,
        Identifier recipeId,
        AutomationExecutionPlan plan) {
    public AutomationTaskContext {
        if (controllerPos == null && controller != null) {
            controllerPos = controller.getBlockPos();
        }
        robotPos = robotPos == null ? BlockPos.ZERO : robotPos;
    }
}
