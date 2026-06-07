package com.knoxhack.echorecovery.api;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface GravePlacementProvider {
    Optional<BlockPos> findPlacement(ServerPlayer player, ServerLevel level, BlockPos origin, String deathCause);
}
