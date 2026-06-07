package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record AutoBuilderStep(
        BlockPos worldPosition,
        Identifier expectedBlockId,
        String expectedText,
        boolean optional) {
    public AutoBuilderStep {
        worldPosition = worldPosition == null ? BlockPos.ZERO : worldPosition.immutable();
        expectedBlockId = expectedBlockId == null ? Identifier.fromNamespaceAndPath("minecraft", "air") : expectedBlockId;
        expectedText = expectedText == null || expectedText.isBlank() ? expectedBlockId.toString() : expectedText.strip();
    }
}
