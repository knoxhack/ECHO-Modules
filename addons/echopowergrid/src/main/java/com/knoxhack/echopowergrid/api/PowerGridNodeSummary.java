package com.knoxhack.echopowergrid.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PowerGridNodeSummary(
        BlockPos pos,
        ResourceKey<Level> dimension,
        EchoPowerNodeType type,
        long localGeneration,
        long localDemand,
        long storedEnergy,
        long capacity,
        boolean online,
        long transferLimit,
        EchoPowerQuality quality,
        boolean blocked,
        boolean tripped) {
    public PowerGridNodeSummary {
        pos = pos == null ? BlockPos.ZERO : pos.immutable();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        type = type == null ? EchoPowerNodeType.CABLE : type;
        localGeneration = Math.max(0L, localGeneration);
        localDemand = Math.max(0L, localDemand);
        storedEnergy = Math.max(0L, storedEnergy);
        capacity = Math.max(0L, capacity);
        transferLimit = Math.max(0L, transferLimit);
        quality = quality == null ? EchoPowerQuality.STABLE : quality;
    }
}
