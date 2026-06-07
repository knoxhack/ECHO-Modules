package com.knoxhack.echoashfallprotocol.api.drone;

import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record EchoDroneOwnerData(
        UUID ownerId,
        UUID droneId,
        String customName,
        EchoDroneMode mode,
        String taskLabel,
        int batteryPercent,
        int health,
        int signalQuality,
        long lastScanTime,
        long lastWarningTime,
        ResourceKey<Level> targetDimension,
        BlockPos targetPos,
        boolean deployed,
        boolean returningToOwner,
        boolean pathingStuck,
        Set<EchoDroneUpgrade> upgrades) {
}
