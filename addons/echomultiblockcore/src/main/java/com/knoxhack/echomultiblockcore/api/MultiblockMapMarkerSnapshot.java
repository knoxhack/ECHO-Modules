package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record MultiblockMapMarkerSnapshot(
        Identifier markerId,
        Identifier definitionId,
        BlockPos position,
        ResourceKey<Level> dimension,
        MultiblockRole role,
        MultiblockState state,
        int color,
        String title,
        String summary) {
    public MultiblockMapMarkerSnapshot {
        position = position == null ? BlockPos.ZERO : position.immutable();
        role = role == null ? MultiblockRole.INFRASTRUCTURE : role;
        state = state == null ? MultiblockState.UNBUILT : state;
        color = color == 0 ? 0xFF00D8FF : color;
        title = title == null || title.isBlank() ? (definitionId == null ? "Multiblock" : definitionId.getPath()) : title.strip();
        summary = summary == null ? "" : summary.strip();
    }
}
