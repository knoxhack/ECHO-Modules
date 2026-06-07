package com.knoxhack.echonexusprotocol.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CoreSoldierEntity extends NexusMobEntity {
   public CoreSoldierEntity(EntityType<? extends CoreSoldierEntity> type, Level level) { super(type, level); }
   public void tick() { super.tick(); LivingEntity target = this.getTarget(); if (!this.level().isClientSide() && target != null && this.tickCount % 70 == 0 && this.distanceTo(target) < 7.0F && this.level() instanceof ServerLevel serverLevel) { target.hurtServer(serverLevel, this.damageSources().magic(), 3.0F); } }
}