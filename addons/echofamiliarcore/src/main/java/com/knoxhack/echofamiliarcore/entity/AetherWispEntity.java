package com.knoxhack.echofamiliarcore.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class AetherWispEntity extends ArcanaFamiliarEntity {
    public AetherWispEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, KIND_AETHER_WISP);
    }
}
