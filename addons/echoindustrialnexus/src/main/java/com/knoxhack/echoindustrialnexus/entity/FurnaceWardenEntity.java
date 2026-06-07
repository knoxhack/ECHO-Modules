package com.knoxhack.echoindustrialnexus.entity;

import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import com.knoxhack.echoindustrialnexus.registry.ModBlocks;
import com.knoxhack.echoindustrialnexus.registry.ModEntities;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import com.knoxhack.echoindustrialnexus.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class FurnaceWardenEntity extends Zombie {
   private final ServerBossEvent bossEvent = new ServerBossEvent(
      this.getUUID(), Component.literal("The Furnace Warden"), BossBarColor.RED, BossBarOverlay.PROGRESS
   );
   private final Set<UUID> participants = new HashSet<>();
   private int phase = 1;
   private boolean coolingNodesSpawned;
   private boolean rewardsDropped;
   private int lastNodeWarning = -1;

   public FurnaceWardenEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.xpReward = 120;
   }

   public static Builder createAttributes() {
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, 180.0)
         .add(Attributes.ATTACK_DAMAGE, 10.0)
         .add(Attributes.ARMOR, 12.0)
         .add(Attributes.MOVEMENT_SPEED, 0.24)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
   }

   protected void customServerAiStep(ServerLevel level) {
      super.customServerAiStep(level);
      this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / Math.max(1.0F, this.getMaxHealth())));
      this.trackNearbyParticipants(level);
      this.updatePhase(level);
      if (this.tickCount % 80 == 0 && this.getTarget() instanceof Player player) {
         player.sendSystemMessage(Component.literal(this.phaseLine()));
         player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, this.phase >= 3 ? 1 : 0));
         if (this.phase >= 2) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 60));
         }
      }
      if (this.phase >= 2 && this.tickCount % 120 == 0) {
         this.spawnFactoryDrone(level);
      }
      if (this.phase >= 2 && this.tickCount % 45 == 0 && this.getTarget() instanceof Player player) {
         BlockPos firePos = player.blockPosition();
         if (level.getBlockState(firePos).isAir()) {
            level.setBlockAndUpdate(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
         }
      }
      if (this.phase == 4) {
         if (!this.coolingNodesSpawned) {
            this.spawnCoolingNodes(level);
            this.coolingNodesSpawned = true;
         }
         int nodes = this.countCoolingNodes(level);
         if (nodes > 0) {
            this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 1));
            if (nodes != this.lastNodeWarning || this.tickCount % 100 == 0) {
               this.lastNodeWarning = nodes;
               for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0D))) {
                  player.sendSystemMessage(Component.literal("ECHO-7 // Cooling nodes remaining: " + nodes + ". Break them to expose the Warden core."));
               }
            }
         } else {
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0));
            this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
         }
      }
   }

   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      this.bossEvent.addPlayer(player);
      this.participants.add(player.getUUID());
   }

   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.bossEvent.removePlayer(player);
   }

   public void die(DamageSource damageSource) {
      if (this.level() instanceof ServerLevel serverLevel) {
         if (!this.rewardsDropped) {
            this.rewardsDropped = true;
            this.dropCodeReward(serverLevel, new ItemStack((ItemLike)ModItems.WARDEN_THERMAL_CORE.get()));
            this.dropCodeReward(serverLevel, new ItemStack((ItemLike)ModItems.OVERCLOCK_CORE.get()));
            this.dropCodeReward(serverLevel, new ItemStack((ItemLike)ModItems.FURNACE_WARDEN_TROPHY.get()));
            this.rewardParticipants(serverLevel);
         }
      }

      if (damageSource.getEntity() instanceof Player player) {
         player.sendSystemMessage(Component.literal("ECHO-7 // Industrial guardian offline. Production survived its operator."));
      }

      this.bossEvent.removeAllPlayers();
      super.die(damageSource);
   }

   private void dropCodeReward(ServerLevel level, ItemStack stack) {
      if (stack.isEmpty()) {
         return;
      }
      ItemEntity item = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), stack);
      item.setDefaultPickUpDelay();
      level.addFreshEntity(item);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("Phase", this.phase);
      output.putBoolean("CoolingNodesSpawned", this.coolingNodesSpawned);
      output.putBoolean("RewardsDropped", this.rewardsDropped);
      output.putInt("LastNodeWarning", this.lastNodeWarning);
      output.putString("Participants", this.participantString());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.phase = Math.max(1, Math.min(4, input.getIntOr("Phase", 1)));
      this.coolingNodesSpawned = input.getBooleanOr("CoolingNodesSpawned", false);
      this.rewardsDropped = input.getBooleanOr("RewardsDropped", false);
      this.lastNodeWarning = input.getIntOr("LastNodeWarning", -1);
      this.participants.clear();
      input.getString("Participants").ifPresent(value -> {
         for (String part : value.split(",")) {
            if (part.isBlank()) {
               continue;
            }
            try {
               this.participants.add(UUID.fromString(part));
            } catch (IllegalArgumentException ignored) {
            }
         }
      });
   }

   private void trackNearbyParticipants(ServerLevel level) {
      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(36.0D))) {
         this.participants.add(player.getUUID());
      }
   }

   private void rewardParticipants(ServerLevel level) {
      this.trackNearbyParticipants(level);
      AABB rewardArea = this.getBoundingBox().inflate(48.0D);
      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, rewardArea)) {
         this.participants.add(player.getUUID());
      }
      for (UUID uuid : this.participants) {
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
         if (player == null || player.level() != level) {
            continue;
         }
         IndustrialProgress.markWardenDefeated(player);
         player.sendSystemMessage(Component.literal("ECHO-7 // Furnace Warden defeat recorded. Thermal Array Mk II cache eligibility unlocked."));
      }
   }

   private void updatePhase(ServerLevel level) {
      float health = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
      int next = health <= 0.25F ? 4 : (health <= 0.5F ? 3 : (health <= 0.75F ? 2 : 1));
      if (next != this.phase) {
         this.phase = next;
         if (this.getTarget() instanceof Player player) {
            player.sendSystemMessage(Component.literal(this.phaseLine()));
         }
         level.playSound(null, this.blockPosition(), ModSounds.WARDEN_PHASE.get(), SoundSource.HOSTILE, 1.4F, 0.85F + this.phase * 0.12F);
         level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 36, 1.8D, 0.5D, 1.8D, 0.04D);
         level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.2D, this.getZ(), 24, 1.5D, 0.3D, 1.5D, 0.02D);

         if (this.phase >= 3) {
            level.explode(this, this.getX(), this.getY(), this.getZ(), 1.8F, false, ExplosionInteraction.MOB);
         }
      }
   }

   private void spawnFactoryDrone(ServerLevel level) {
      if (this.getTarget() == null || level.getEntitiesOfClass(Zombie.class, this.getBoundingBox().inflate(10.0D), zombie -> zombie != this).size() >= 4) {
         return;
      }
      FurnaceDroneEntity drone = ModEntities.FURNACE_DRONE.get().create(level, EntitySpawnReason.EVENT);
      if (drone == null) {
         return;
      }
      if (drone instanceof Mob mob) {
         mob.setTarget(this.getTarget());
      }
      if (drone.getAttribute(Attributes.MAX_HEALTH) != null) {
         drone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0D);
         drone.setHealth(24.0F);
      }
      if (drone.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
         drone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0D);
      }
      drone.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60, 0));
      drone.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0));
      double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
      drone.setPos(this.getX() + Math.cos(angle) * 4.0D, this.getY(), this.getZ() + Math.sin(angle) * 4.0D);
      drone.setCustomName(Component.literal("Furnace Drone"));
      drone.setPersistenceRequired();
      level.playSound(null, drone.blockPosition(), ModSounds.MACHINE_HUM.get(), SoundSource.HOSTILE, 0.8F, 1.4F);
      level.addFreshEntity(drone);
   }

   private void spawnCoolingNodes(ServerLevel level) {
      BlockPos center = this.blockPosition();
      BlockPos[] nodes = new BlockPos[]{
         center.offset(4, 0, 0),
         center.offset(-4, 0, 0),
         center.offset(0, 0, 4),
         center.offset(0, 0, -4)
      };
      for (BlockPos pos : nodes) {
         if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlockAndUpdate(pos, ModBlocks.COOLING_FAN_BLOCK.get().defaultBlockState());
            level.setBlockAndUpdate(pos.below(), ModBlocks.WARNING_STRIPE_BLOCK.get().defaultBlockState());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 12, 0.35D, 0.25D, 0.35D, 0.02D);
         }
      }
   }

   private int countCoolingNodes(ServerLevel level) {
      int nodes = 0;
      BlockPos center = this.blockPosition();
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -1, -5), center.offset(5, 2, 5))) {
         if (level.getBlockState(pos).is(ModBlocks.COOLING_FAN_BLOCK.get())) {
            nodes++;
         }
      }
      return nodes;
   }

   private String phaseLine() {
      return switch (this.phase) {
         case 2 -> "ECHO-7 // Heat overload. Furnace Warden vents are cycling.";
         case 3 -> "ECHO-7 // Meltdown phase. Exposed thermal core detected.";
         case 4 -> "ECHO-7 // Emergency shutdown window open. Break production, then survive it.";
         default -> "ECHO-7 // Industrial guardian active. It was not built to protect people. It was built to protect production.";
      };
   }

   private String participantString() {
      StringBuilder builder = new StringBuilder();
      for (UUID uuid : this.participants) {
         if (!builder.isEmpty()) {
            builder.append(',');
         }
         builder.append(uuid);
      }
      return builder.toString();
   }
}
