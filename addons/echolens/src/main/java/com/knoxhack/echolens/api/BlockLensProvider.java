package com.knoxhack.echolens.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockLensProvider extends LensInfoProvider {
    @Override
    default boolean supports(LensContext context) {
        return context != null && context.hasBlock();
    }

    @Override
    default List<LensInfoSection> inspect(LensContext context) {
        return inspectBlock(context, context.blockState(), context.blockPos());
    }

    List<LensInfoSection> inspectBlock(LensContext context, BlockState state, BlockPos pos);
}
