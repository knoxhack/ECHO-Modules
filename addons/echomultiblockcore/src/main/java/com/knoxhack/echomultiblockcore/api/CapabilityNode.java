package com.knoxhack.echomultiblockcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record CapabilityNode(
        Identifier nodeId,
        Identifier capabilityId,
        BlockPos position,
        int capacity,
        int throughput,
        boolean online) {
    public CapabilityNode {
        nodeId = nodeId == null ? capabilityId : nodeId;
        capabilityId = capabilityId == null ? MultiblockCapability.WORKCELL.id() : capabilityId;
        position = position == null ? BlockPos.ZERO : position.immutable();
        capacity = Math.max(0, capacity);
        throughput = Math.max(0, throughput);
    }
}
