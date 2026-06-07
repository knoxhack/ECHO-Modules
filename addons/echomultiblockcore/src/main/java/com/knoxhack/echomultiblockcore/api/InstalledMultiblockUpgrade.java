package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record InstalledMultiblockUpgrade(
        Identifier upgradeId,
        Identifier slotId,
        BlockPos worldPosition,
        int tier) {
    public InstalledMultiblockUpgrade {
        upgradeId = upgradeId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "unknown_upgrade") : upgradeId;
        slotId = slotId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "general") : slotId;
        worldPosition = worldPosition == null ? BlockPos.ZERO : worldPosition.immutable();
        tier = Math.max(1, tier);
    }
}
