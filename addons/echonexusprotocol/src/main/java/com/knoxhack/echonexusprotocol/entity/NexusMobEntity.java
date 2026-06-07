package com.knoxhack.echonexusprotocol.entity;

import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusMissionHooks;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class NexusMobEntity extends Zombie {
   private boolean nexusBalanceApplied;

   public NexusMobEntity(EntityType<? extends Zombie> type, Level level) {
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
      this.applyNexusBalance();
      if (!this.level().isClientSide() && this.tickCount % 100 == 0 && this.level() instanceof ServerLevel serverLevel) {
         NexusWorldData.get(serverLevel).addCorruptionPressure(this.chunkPosition(), 1);
      }
      if (!this.level().isClientSide() && this.tickCount % 60 == 0 && this.level() instanceof ServerLevel serverLevel && "merge".equals(NexusWorldData.get(serverLevel).endingState()) && this.getTarget() instanceof ServerPlayer && this.random.nextBoolean()) {
         this.setTarget(null);
      }
   }

   public void die(DamageSource damageSource) {
      super.die(damageSource);
      if (this.level() instanceof ServerLevel serverLevel && damageSource.getEntity() instanceof ServerPlayer player) {
         String id = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
         if (id.contains("corruption_warden")) {
            boolean firstWorldDefeat = NexusWorldData.get(serverLevel).markWardenDefeated();
            NexusPlayerData data = NexusPlayerData.get(player);
            data.markWardenDefeated();
            data.refreshFieldTelemetry(player);
            NexusPlayerData.saveAndSync(player, data);
            NexusMissionHooks.recordWarden(player);
            if (firstWorldDefeat) {
               this.spawnAtLocation(serverLevel, new ItemStack((ItemLike)ModItems.REACTOR_CORE.get()));
               this.spawnAtLocation(serverLevel, new ItemStack((ItemLike)ModItems.BLACKBOX_FRAGMENT.get()));
            }
            serverLevel.playSound(null, this.blockPosition(), ModSounds.WARDEN_PULSE.get(), net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 0.55F);
            player.sendSystemMessage(
               net.minecraft.network.chat.Component.literal(
                  firstWorldDefeat
                     ? "ECHO-7 // Corruption Warden offline. The containment lab failed, but its Reactor Core is recoverable."
                     : "ECHO-7 // Corruption Warden signal already indexed. No duplicate reactor reward authorized."
               )
            );
         } else if (id.contains("nexus_guardian")) {
            boolean firstWorldDefeat = NexusWorldData.get(serverLevel).markGuardianDefeated();
            NexusPlayerData data = NexusPlayerData.get(player);
            data.markGuardianDefeated();
            data.refreshFieldTelemetry(player);
            NexusPlayerData.saveAndSync(player, data);
            NexusMissionHooks.recordCoreEntered(player);
            if (firstWorldDefeat) {
               this.spawnAtLocation(serverLevel, new ItemStack((ItemLike)ModItems.CORE_KEY_ASSEMBLY.get()));
            }
            serverLevel.playSound(null, this.blockPosition(), ModSounds.ENDING_CHOICE.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.65F);
            player.sendSystemMessage(
               net.minecraft.network.chat.Component.literal(
                  firstWorldDefeat
                     ? "ECHO-7 // Nexus Guardian defeated. The Core is no longer waiting. Choose what rebuilds the world."
                     : "ECHO-7 // Guardian defeat already recorded. Final-path reward remains one-time."
               )
            );
         }
      }
   }

   protected float scaledBossDamage(float baseDamage) {
      return baseDamage * Math.max(1, (Integer)Config.BOSS_DAMAGE_PERCENT.get()) / 100.0F;
   }

   private void applyNexusBalance() {
      if (this.nexusBalanceApplied || this.level().isClientSide()) {
         return;
      }

      String id = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
      if (id.contains("corruption_warden") || id.contains("nexus_guardian")) {
         float ratio = Math.max(1, (Integer)Config.BOSS_HEALTH_PERCENT.get()) / 100.0F;
         float healthRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * ratio);
         this.setHealth(Math.max(1.0F, this.getMaxHealth() * healthRatio));
      }

      this.nexusBalanceApplied = true;
   }
}
