package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public record AutomationEffectInvocation(
        ServerLevel level,
        MultiblockControllerBlockEntity controller,
        BlockPos controllerPos,
        Player actor,
        Identifier effectId,
        MultiblockAutomationRecipe recipe,
        AutomationExecutionPlan plan,
        TaskExecutionSnapshot taskSnapshot,
        String phase) {
    public AutomationEffectInvocation {
        if (controllerPos == null && controller != null) {
            controllerPos = controller.getBlockPos();
        }
        controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        phase = phase == null || phase.isBlank() ? "unknown" : phase.strip();
    }
}
