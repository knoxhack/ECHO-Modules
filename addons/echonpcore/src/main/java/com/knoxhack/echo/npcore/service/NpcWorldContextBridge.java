package com.knoxhack.echo.npcore.service;

import com.knoxhack.echocore.api.EchoCoreServices;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public interface NpcWorldContextBridge {
    default Identifier settlementAt(ServerLevel level, BlockPos pos) {
        if (level != null && pos != null) {
            return EchoCoreServices.worldRegions().nearbyMarkers(level, pos, 64).stream()
                    .findFirst()
                    .map(marker -> marker.id())
                    .orElse(null);
        }
        return null;
    }

    default Identifier regionAt(ServerLevel level, BlockPos pos) {
        if (level != null) {
            return EchoCoreServices.worldRegions().nearbyRegions(level, pos, 1).stream()
                    .findFirst()
                    .map(region -> region.id())
                    .orElse(null);
        }
        return null;
    }
}
