package com.knoxhack.echopowergrid.api;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PowerGridNetworkSummary(
        UUID networkId,
        ResourceKey<Level> dimension,
        BlockPos anchorPos,
        EchoGridState state,
        EchoPowerQuality quality,
        long totalGeneration,
        long totalDemand,
        long availablePower,
        long totalStored,
        long totalCapacity,
        int nodeCount,
        long transferLimit) {
    public PowerGridNetworkSummary {
        networkId = networkId == null ? new UUID(0L, 0L) : networkId;
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        anchorPos = anchorPos == null ? BlockPos.ZERO : anchorPos.immutable();
        state = state == null ? EchoGridState.OFFLINE : state;
        quality = quality == null ? EchoPowerQuality.STABLE : quality;
        totalGeneration = Math.max(0L, totalGeneration);
        totalDemand = Math.max(0L, totalDemand);
        availablePower = Math.max(0L, availablePower);
        totalStored = Math.max(0L, totalStored);
        totalCapacity = Math.max(0L, totalCapacity);
        nodeCount = Math.max(0, nodeCount);
        transferLimit = Math.max(0L, transferLimit);
    }
}
