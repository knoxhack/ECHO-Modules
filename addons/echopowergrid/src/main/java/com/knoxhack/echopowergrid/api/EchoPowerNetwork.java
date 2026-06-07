package com.knoxhack.echopowergrid.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class EchoPowerNetwork {
    public final UUID networkId;
    public final ResourceKey<Level> dimension;
    final Set<BlockPos> nodes = new HashSet<>();
    public long totalGeneration;
    public long totalDemand;
    public long totalStored;
    public long totalCapacity;
    public EchoGridState state = EchoGridState.OFFLINE;
    public EchoPowerQuality quality = EchoPowerQuality.STABLE;
    public long transferLimit;
    public boolean dirty = true;
    public int ticksSinceUpdate;
    public int overloadGraceTicks = 0;
    public boolean overloaded = false;

    public EchoPowerNetwork(UUID networkId, ResourceKey<Level> dimension) {
        this.networkId = networkId;
        this.dimension = dimension;
    }

    public Set<BlockPos> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public void addNode(BlockPos pos) {
        nodes.add(pos);
    }

    public void removeNode(BlockPos pos) {
        nodes.remove(pos);
    }

    public void clearNodes() {
        nodes.clear();
    }

    public void addAllNodes(java.util.Collection<BlockPos> positions) {
        nodes.addAll(positions);
    }

    public boolean contains(BlockPos pos) {
        return nodes.contains(pos);
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public PowerGridSnapshot toSnapshot() {
        return new PowerGridSnapshot(networkId, totalGeneration, totalDemand, totalStored, totalCapacity, state, quality, nodes.size());
    }
}
