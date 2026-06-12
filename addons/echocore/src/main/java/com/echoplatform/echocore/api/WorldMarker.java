package com.echoplatform.echocore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record WorldMarker(
        Identifier id,
        Identifier regionId,
        WorldMarkerType type,
        String displayName,
        String summary,
        ResourceKey<Level> dimension,
        BlockPos pos,
        int radius,
        boolean discovered,
        long updatedGameTime) {
    public WorldMarker {
        type = type == null ? WorldMarkerType.STRUCTURE : type;
        displayName = displayName == null ? "" : displayName;
        summary = summary == null ? "" : summary;
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        pos = pos == null ? BlockPos.ZERO : pos;
        radius = Math.max(1, radius);
        updatedGameTime = Math.max(0L, updatedGameTime);
    }

    public WorldMarker discovered(boolean value) {
        return new WorldMarker(id, regionId, type, displayName, summary, dimension, pos, radius, value, updatedGameTime);
    }

    public String title() {
        return displayName;
    }
}
