package com.knoxhack.echonexusprotocol.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class StaticCrawlerEntity extends NexusMobEntity {
   public StaticCrawlerEntity(EntityType<? extends StaticCrawlerEntity> type, Level level) { super(type, level); }
   public void tick() { super.tick(); LivingEntity target = this.getTarget(); if (!this.level().isClientSide() && target != null && this.onGround() && this.tickCount % 55 == 0) { Vec3 toward = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ()).normalize(); this.setDeltaMovement(toward.x * 0.7D, 0.34D, toward.z * 0.7D); } }
}