package com.knoxhack.echonexusprotocol.entity;

import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NexusGuardianEntity extends NexusMobEntity {
   private int lastSoundPhase;
   private int lastTelegraphPhase;
   private int phaseTelegraphCount;
   private int lastHazardPulseTick = -1;

   public NexusGuardianEntity(EntityType<? extends NexusGuardianEntity> type, Level level) { super(type, level); }
   public int phase() { float ratio = this.getHealth() / Math.max(1.0F, this.getMaxHealth()); if (ratio > 0.75F) return 1; if (ratio > 0.5F) return 2; return ratio > 0.25F ? 3 : 4; }
   @Override
   public void setHealth(float health) {
      super.setHealth(health);
      if (this.level() instanceof ServerLevel serverLevel && this.isAddedToLevel()) {
         telegraphPhaseIfChanged(serverLevel, this.phase());
      }
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel serverLevel) {
         int phase = this.phase();
         telegraphPhaseIfChanged(serverLevel, phase);
         if (this.tickCount % Math.max(35, 110 - phase * 18) == 0) {
            pulseHazard(serverLevel, phase);
         }
      }
   }

   public int lastTelegraphPhase() {
      return this.lastTelegraphPhase;
   }

   public int phaseTelegraphCount() {
      return this.phaseTelegraphCount;
   }

   public int lastHazardPulseTick() {
      return this.lastHazardPulseTick;
   }

   private void telegraphPhase(ServerLevel serverLevel, int phase) {
      this.lastTelegraphPhase = phase;
      this.phaseTelegraphCount++;
      serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.7D, this.getZ(), 8 + phase * 3, 0.25D + phase * 0.1D, 0.25D, 0.25D + phase * 0.1D, 0.02D);
      serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.1D, this.getZ(), 18 + phase * 8, 1.5D, 1.0D, 1.5D, 0.12D);
      serverLevel.playSound(null, this.blockPosition(), ModSounds.GUARDIAN_PHASE.get(), SoundSource.HOSTILE, 0.9F, 0.75F + phase * 0.08F);
   }

   private boolean telegraphPhaseIfChanged(ServerLevel serverLevel, int phase) {
      if (phase == this.lastSoundPhase) {
         return false;
      }
      this.lastSoundPhase = phase;
      this.telegraphPhase(serverLevel, phase);
      NexusWorldData.get(serverLevel).startAnomalyStorm(this.chunkPosition(), serverLevel.getGameTime());
      if (phase >= 3) {
         pulseHazard(serverLevel, phase);
      }
      return true;
   }

   private void pulseHazard(ServerLevel serverLevel, int phase) {
      NexusWorldData data = NexusWorldData.get(serverLevel);
      data.addFieldValue(this.chunkPosition(), -phase);
      data.addCorruptionPressure(this.chunkPosition(), phase * 2);
      if (phase >= 3) data.markRealityTearActive(this.chunkPosition());
      this.lastHazardPulseTick = Math.max(1, this.tickCount);
      serverLevel.sendParticles(phase >= 3 ? ParticleTypes.PORTAL : ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2D, this.getZ(), 10 + phase * 4, 0.8D + phase * 0.2D, 0.7D, 0.8D + phase * 0.2D, 0.08D);
      for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(7.0D + phase))) {
         player.hurtServer(serverLevel, this.damageSources().magic(), this.scaledBossDamage(2.5F + phase));
         if (phase >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 0));
         }
         if (phase >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
         }
         if (phase >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
         }
      }
   }
}
