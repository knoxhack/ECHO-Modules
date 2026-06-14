package com.knoxhack.echo.settlementcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Door-like block that prevents water flow and marks habitat boundary.
 */
public class AirlockBlock extends HabitatBlock {
    public AirlockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, state.getBlock(), 1);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
    }
}
