package com.knoxhack.echopowergrid.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface EchoPowerNode {
    BlockPos getNodePos();
    ResourceKey<Level> getDimension();
    EchoPowerNodeType getNodeType();
    long getGenerationPerTick();
    long getDemandPerTick();
    long getStoredEnergy();
    long getCapacity();
    long getTransferLimit();
    boolean isOnline();
    boolean isOverloaded();
}
