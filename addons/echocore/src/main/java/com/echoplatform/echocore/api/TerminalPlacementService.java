package com.echoplatform.echocore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface TerminalPlacementService {
    default boolean placeTerminal(Level level, BlockPos pos, Player owner) {
        return false;
    }

    default BlockState structureBlockState() {
        return null;
    }

    default boolean isTerminalBlock(BlockState state) {
        return false;
    }
}
