package com.echoplatform.echocore.api;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface StructureDiscoveryService {
    default boolean recordStructureScan(Player player, Identifier structureId, BlockPos pos, String title, String summary) {
        return false;
    }

    default Set<Identifier> discoveredRegions(Player player) {
        return Set.of();
    }
}
