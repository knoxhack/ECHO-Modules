package com.knoxhack.echoashfallprotocol.api.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record EchoDroneMarker(
        EchoDroneScanCategory category,
        String label,
        String detail,
        ResourceKey<Level> dimension,
        BlockPos pos,
        long expiresAt,
        boolean precise) {
    public EchoDroneMarker {
        category = category == null ? EchoDroneScanCategory.LOOT : category;
        label = label == null || label.isBlank() ? category.summaryName() : label.strip();
        detail = detail == null ? "" : detail.strip();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        pos = pos == null ? BlockPos.ZERO : pos.immutable();
    }
}
