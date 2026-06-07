package com.knoxhack.echolens.api;

import java.util.List;
import net.minecraft.world.level.material.FluidState;

public interface FluidLensProvider extends LensInfoProvider {
    @Override
    default boolean supports(LensContext context) {
        return context != null && context.hasFluid();
    }

    @Override
    default List<LensInfoSection> inspect(LensContext context) {
        return inspectFluid(context, context.fluidState());
    }

    List<LensInfoSection> inspectFluid(LensContext context, FluidState state);
}
