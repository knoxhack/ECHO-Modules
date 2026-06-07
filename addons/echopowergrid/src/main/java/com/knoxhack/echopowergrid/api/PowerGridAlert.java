package com.knoxhack.echopowergrid.api;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PowerGridAlert(
        UUID networkId,
        ResourceKey<Level> dimension,
        BlockPos pos,
        PowerGridAlertLevel level,
        String code,
        String message) {
    public PowerGridAlert {
        networkId = networkId == null ? new UUID(0L, 0L) : networkId;
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        pos = pos == null ? BlockPos.ZERO : pos.immutable();
        level = level == null ? PowerGridAlertLevel.INFO : level;
        code = code == null ? "info" : code;
        message = message == null ? "" : message;
    }
}
