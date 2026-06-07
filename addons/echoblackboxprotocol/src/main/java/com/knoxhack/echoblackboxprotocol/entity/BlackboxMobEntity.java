package com.knoxhack.echoblackboxprotocol.entity;

import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.world.BlackboxWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class BlackboxMobEntity extends Zombie {
   public BlackboxMobEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, 32.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.MOVEMENT_SPEED, 0.27);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.getTarget() instanceof Player player && this.mergeNeutral(player)) {
         this.setTarget(null);
         this.setAggressive(false);
         if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 40 == 0) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 6, 0.25, 0.25, 0.25, 0.01);
         }
         if (this.tickCount % 200 == 0) {
            player.sendSystemMessage(Component.literal("ECHO-7 // merged memory signature recognized. Hostile packet ignored."));
         }
         return;
      }

      if (!this.level().isClientSide() && this.getTarget() instanceof Player && this.worldEnding() == BlackboxEnding.DESTROY) {
         this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
         if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 60 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.7, this.getZ(), 5, 0.25, 0.15, 0.25, 0.01);
         }
      }

      if (!this.level().isClientSide() && this.tickCount % 100 == 0 && this.getTarget() instanceof Player player && this.distanceToSqr(player) < 36.0) {
         if (this.getType() == ModEntities.SECURITY_ECHO.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
         } else if (this.getType() == ModEntities.MEMORY_PARASITE.get()) {
            BlackboxProgress progress = BlackboxProgress.get(player);
            progress.falseSignals(progress.falseSignalCount() + 1);
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0));
         } else if (this.getType() == ModEntities.BLACKBOX_SENTINEL.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
         } else if (this.getType() == ModEntities.FALSE_ECHO_MINION.get()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // scanner warning: trusted voice mismatch detected."));
         } else {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
         }
      }
   }

   private boolean mergeNeutral(Player player) {
      return this.worldEnding() == BlackboxEnding.MERGE
         && (this.getType() == ModEntities.MEMORY_PARASITE.get()
            || this.getType() == ModEntities.FALSE_ECHO_MINION.get()
            || this.getType() == ModEntities.SECURITY_ECHO.get());
   }

   private BlackboxEnding worldEnding() {
      if (this.level() instanceof ServerLevel serverLevel) {
         return BlackboxWorldData.get(serverLevel.getServer().overworld()).ending();
      } else {
         return BlackboxEnding.NONE;
      }
   }
}
