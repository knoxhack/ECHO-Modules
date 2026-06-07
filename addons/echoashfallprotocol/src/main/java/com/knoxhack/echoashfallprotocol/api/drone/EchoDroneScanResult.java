package com.knoxhack.echoashfallprotocol.api.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record EchoDroneScanResult(
        EchoDroneScanCategory category,
        BlockPos pos,
        String label,
        String detail,
        Identifier targetId,
        double distanceSqr,
        boolean precise) {
    public EchoDroneScanResult {
        category = category == null ? EchoDroneScanCategory.LOOT : category;
        pos = pos == null ? BlockPos.ZERO : pos.immutable();
        label = label == null || label.isBlank() ? category.summaryName() : label.strip();
        detail = detail == null ? "" : detail.strip();
    }
}
