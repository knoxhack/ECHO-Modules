package com.knoxhack.echoashfallprotocol.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for blocks/entities that provide machine state feedback.
 */
public interface MachineStateProvider {
    /**
     * Get the current operational state of this machine.
     */
    MachineState getMachineState(Level level, BlockPos pos, BlockState state);

    /**
     * Check if this machine should show state in tooltip.
     */
    default boolean showStateInTooltip() {
        return true;
    }
}
