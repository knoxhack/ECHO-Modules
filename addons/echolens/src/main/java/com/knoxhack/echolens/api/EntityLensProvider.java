package com.knoxhack.echolens.api;

import java.util.List;
import net.minecraft.world.entity.Entity;

public interface EntityLensProvider extends LensInfoProvider {
    @Override
    default boolean supports(LensContext context) {
        return context != null && context.hasEntity();
    }

    @Override
    default List<LensInfoSection> inspect(LensContext context) {
        return inspectEntity(context, context.entity());
    }

    List<LensInfoSection> inspectEntity(LensContext context, Entity entity);
}
