package com.echoplatform.echocore.api;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Backward-compatible discovery service name kept for modules that predate
 * {@link StructureDiscoveryService}.
 */
@Deprecated(forRemoval = false)
public interface IStructureDiscoveryService extends StructureDiscoveryService {
    default boolean recordStructureScan(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary) {
        return false;
    }

    @Override
    default boolean recordStructureScan(Player player, Identifier structureId, BlockPos pos, String title, String summary) {
        return player instanceof ServerPlayer serverPlayer
                && recordStructureScan(serverPlayer, structureId, pos, title, summary);
    }

    default boolean recordStructureEntry(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary) {
        return false;
    }

    default boolean hasDiscoveredRegion(Player player, Identifier regionId) {
        return false;
    }

    @Override
    default Set<Identifier> discoveredRegions(Player player) {
        return Set.of();
    }
}
