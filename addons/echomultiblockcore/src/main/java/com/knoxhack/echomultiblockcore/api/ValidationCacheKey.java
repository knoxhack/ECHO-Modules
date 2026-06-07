package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ValidationCacheKey(
        ResourceKey<Level> dimension,
        BlockPos controllerPos,
        Identifier definitionId,
        long structureVersion) {
    public ValidationCacheKey {
        controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
        structureVersion = Math.max(0L, structureVersion);
    }
}
