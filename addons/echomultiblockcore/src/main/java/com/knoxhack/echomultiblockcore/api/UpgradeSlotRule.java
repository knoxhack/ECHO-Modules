package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record UpgradeSlotRule(
        Identifier slotId,
        BlockPos localPosition,
        List<Identifier> allowedUpgrades,
        boolean required) {
    public UpgradeSlotRule {
        slotId = slotId == null ? Identifier.fromNamespaceAndPath("echomultiblockcore", "general") : slotId;
        localPosition = localPosition == null ? BlockPos.ZERO : localPosition.immutable();
        allowedUpgrades = List.copyOf(allowedUpgrades == null ? List.of() : allowedUpgrades);
    }
}
