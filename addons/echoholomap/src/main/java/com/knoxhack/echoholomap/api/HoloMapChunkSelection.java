package com.knoxhack.echoholomap.api;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapChunkSelection(
        ResourceKey<Level> dimension,
        int chunkX,
        int chunkZ) {
    public HoloMapChunkSelection {
        dimension = dimension == null ? Level.OVERWORLD : dimension;
    }

    public Identifier dimensionId() {
        return dimension.identifier();
    }

    public double centerX() {
        return chunkX * 16.0D + 8.0D;
    }

    public double centerZ() {
        return chunkZ * 16.0D + 8.0D;
    }
}
