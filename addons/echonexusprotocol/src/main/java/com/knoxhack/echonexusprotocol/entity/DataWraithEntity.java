package com.knoxhack.echonexusprotocol.entity;

import com.knoxhack.echonexusprotocol.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DataWraithEntity extends NexusMobEntity {
   public DataWraithEntity(EntityType<? extends DataWraithEntity> type, Level level) { super(type, level); }
   public void tick() { super.tick(); this.setNoGravity(true); this.noPhysics = true; if (!this.level().isClientSide() && this.tickCount % 40 == 0) { Vec3 motion = this.getDeltaMovement(); this.setDeltaMovement(motion.x * 0.8D, 0.04D, motion.z * 0.8D); } if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 20 == 0) { serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 8, 0.28D, 0.45D, 0.28D, 0.02D); } if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 90 == 0) { boolean hit = false; for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.0D))) { player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45, 0)); player.hurtServer(serverLevel, this.damageSources().magic(), 2.0F); hit = true; } if (hit) { serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.8D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D); serverLevel.playSound(null, this.blockPosition(), ModSounds.REALITY_TEAR_PULSE.get(), SoundSource.HOSTILE, 0.45F, 1.55F); } } }
}
