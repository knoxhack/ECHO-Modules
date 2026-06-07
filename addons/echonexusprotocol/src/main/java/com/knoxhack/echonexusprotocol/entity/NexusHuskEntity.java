package com.knoxhack.echonexusprotocol.entity;

import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NexusHuskEntity extends NexusMobEntity {
   public NexusHuskEntity(EntityType<? extends NexusHuskEntity> type, Level level) { super(type, level); }
   public void tick() { super.tick(); if (!this.level().isClientSide() && this.tickCount % 80 == 0 && this.level() instanceof ServerLevel serverLevel) { NexusWorldData.get(serverLevel).addCorruptionPressure(this.chunkPosition(), 2); } }
}