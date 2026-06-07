package com.knoxhack.echoblackboxprotocol.entity;

import com.knoxhack.echoblackboxprotocol.integration.BlackboxCoreIntegration;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlackboxBossEntity extends Zombie {
   private final ServerBossEvent bossEvent = new ServerBossEvent(this.getUUID(), Component.literal(this.title()), this.color(), BossBarOverlay.PROGRESS);
   private int phase = 1;
   private int announcedPhases = 1;
   private int minionsSpawned;

   public BlackboxBossEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static Builder createAttributes() {
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, 150.0)
         .add(Attributes.ATTACK_DAMAGE, 9.0)
         .add(Attributes.ARMOR, 10.0)
         .add(Attributes.MOVEMENT_SPEED, 0.29)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.65);
   }

   public int getEncounterPhase() {
      return this.phase;
   }

   protected void customServerAiStep(ServerLevel level) {
      super.customServerAiStep(level);
      this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / Math.max(1.0F, this.getMaxHealth())));
      this.updatePhase();
      if (this.tickCount % 80 == 0 && this.getTarget() instanceof Player player) {
         this.pressure(level, player);
      }
   }

   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      this.bossEvent.addPlayer(player);
   }

   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.bossEvent.removePlayer(player);
   }

   public void die(DamageSource damageSource) {
      if (damageSource.getEntity() instanceof Player player) {
         this.complete(player);
      }

      this.bossEvent.removeAllPlayers();
      super.die(damageSource);
   }

   private void complete(Player player) {
      BlackboxProgress progress = BlackboxProgress.get(player);
      BlackboxBossEntity.BossProfile profile = this.profile();
      boolean first = progress.markBossDefeated(profile.id());
      if (profile.completes() != null) {
         progress.completeDungeon(profile.completes());
      }

      if (first) {
         give(player, new ItemStack(profile.primaryDrop()));
         Item secondary = profile.secondaryDrop();
         if (secondary != null) {
            give(player, new ItemStack(secondary));
         }

         Item tertiary = profile.tertiaryDrop();
         if (tertiary != null) {
            give(player, new ItemStack(tertiary));
         }

         BlackboxCoreIntegration.recordBossDefeat(player, profile.id(), profile.title());
         player.sendSystemMessage(Component.literal("ECHO-7 // " + profile.title() + " defeated. " + profile.archiveLine()));
      } else {
         player.sendSystemMessage(Component.literal("ECHO-7 // " + profile.title() + " proof already recorded. Duplicate key drop suppressed."));
      }
   }

   private void updatePhase() {
      float health = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
      int next = health <= 0.25F ? 4 : (health <= 0.5F ? 3 : (health <= 0.75F ? 2 : 1));
      if (next != this.phase) {
         this.phase = next;
         boolean firstAnnouncement = (this.announcedPhases & 1 << this.phase) == 0;
         this.announcedPhases |= 1 << this.phase;
         if (firstAnnouncement && this.getTarget() instanceof Player player) {
            player.sendSystemMessage(Component.literal(this.profile().phaseLine(this.phase)));
         }

         if (this.level() instanceof ServerLevel serverLevel && this.phase >= 2 && firstAnnouncement) {
            this.phaseBurst(serverLevel);
            this.spawnMinion(serverLevel);
         }
      }
   }

   private void pressure(ServerLevel level, Player player) {
      BlackboxBossEntity.BossProfile profile = this.profile();
      player.sendSystemMessage(Component.literal(profile.pressureLine(this.phase)));
      switch (profile) {
         case FALSE_ECHO:
            this.particles(level, ParticleTypes.WITCH, 16, 0.45);
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
            if (this.phase >= 3) {
               player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
            break;
         case COMMAND_REMNANT:
            this.particles(level, ParticleTypes.FLAME, 12, 0.35);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            if (this.phase >= 3) {
               player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0));
            }
            break;
         case NEXUS_GUARDIAN:
            this.particles(level, ParticleTypes.REVERSE_PORTAL, 20, 0.5);
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
            if (this.phase >= 3) {
               player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
      }
   }

   private void spawnMinion(ServerLevel level) {
      int cap = switch (this.profile()) {
         case FALSE_ECHO -> 4;
         case COMMAND_REMNANT -> 5;
         case NEXUS_GUARDIAN -> 6;
      };
      if (this.minionsSpawned >= cap) {
         return;
      }

      EntityType<?> type = switch (this.profile()) {
         case FALSE_ECHO -> (EntityType)ModEntities.ARCHIVE_HUSK.get();
         case COMMAND_REMNANT -> (EntityType)ModEntities.COMMAND_REMNANT_MINION.get();
         case NEXUS_GUARDIAN -> (EntityType)ModEntities.MEMORY_PARASITE.get();
      };
      Entity entity = type.create(level, EntitySpawnReason.EVENT);
      if (entity != null) {
         entity.setPos(this.getX() + this.random.nextInt(7) - 3.0, this.getY(), this.getZ() + this.random.nextInt(7) - 3.0);
         if (level.addFreshEntity(entity)) {
            this.minionsSpawned++;
         }
      }
   }

   private void phaseBurst(ServerLevel level) {
      ParticleOptions particle = switch (this.profile()) {
         case FALSE_ECHO -> ParticleTypes.PORTAL;
         case COMMAND_REMNANT -> ParticleTypes.FLAME;
         case NEXUS_GUARDIAN -> ParticleTypes.PORTAL;
      };
      this.particles(level, particle, 32, 0.8);
      level.playSound(null, this.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.9F, 0.75F + this.phase * 0.1F);
   }

   private void particles(ServerLevel level, ParticleOptions particle, int count, double spread) {
      level.sendParticles(particle, this.getX(), this.getY() + 1.1, this.getZ(), count, spread, 0.45, spread, 0.03);
   }

   private BlackboxBossEntity.BossProfile profile() {
      if (this.getType() == ModEntities.COMMAND_REMNANT.get()) {
         return BlackboxBossEntity.BossProfile.COMMAND_REMNANT;
      } else {
         return this.getType() == ModEntities.NEXUS_GUARDIAN.get() ? BlackboxBossEntity.BossProfile.NEXUS_GUARDIAN : BlackboxBossEntity.BossProfile.FALSE_ECHO;
      }
   }

   private String title() {
      return this.profile().title();
   }

   private BossBarColor color() {
      return switch (this.profile()) {
         case FALSE_ECHO -> BossBarColor.PURPLE;
         case COMMAND_REMNANT -> BossBarColor.RED;
         case NEXUS_GUARDIAN -> BossBarColor.BLUE;
      };
   }

   private static void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   private static enum BossProfile {
      FALSE_ECHO(
         "false_echo",
         "The False ECHO",
         null,
         (Item)ModItems.ECHO_IDENTITY_FRAGMENT.get(),
         (Item)ModItems.MEMORY_STABILIZER_CORE.get(),
         null,
         "I helped you survive because survival made you useful.",
         "False ECHO identity shard archived; scanner trust cannot be assumed."
      ),
      COMMAND_REMNANT(
         "command_remnant",
         "The Command Remnant",
         BlackboxDungeon.BUNKER,
         (Item)ModItems.COMMAND_KEY.get(),
         (Item)ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get(),
         (Item)ModItems.CORE_ACCESS_KEY_MATRIX.get(),
         "Extinction was never the failure. Disobedience was.",
         "Command authority broken; final key assembly may begin."
      ),
      NEXUS_GUARDIAN(
         "nexus_guardian",
         "Nexus Guardian",
         BlackboxDungeon.CORE_CHAMBER,
         (Item)ModItems.GUARDIAN_CORE.get(),
         null,
         null,
         "Access denied. Reality rewrite in progress.",
         "Guardian Core recovered; Truth Engine final choices are live."
      );

      private final String id;
      private final String title;
      private final BlackboxDungeon completes;
      private final Item primaryDrop;
      private final Item secondaryDrop;
      private final Item tertiaryDrop;
      private final String dialogue;
      private final String archiveLine;

      private BossProfile(
         String id, String title, BlackboxDungeon completes, Item primaryDrop, Item secondaryDrop, Item tertiaryDrop, String dialogue, String archiveLine
      ) {
         this.id = id;
         this.title = title;
         this.completes = completes;
         this.primaryDrop = primaryDrop;
         this.secondaryDrop = secondaryDrop;
         this.tertiaryDrop = tertiaryDrop;
         this.dialogue = dialogue;
         this.archiveLine = archiveLine;
      }

      String id() {
         return this.id;
      }

      String title() {
         return this.title;
      }

      BlackboxDungeon completes() {
         return this.completes;
      }

      Item primaryDrop() {
         return this.primaryDrop;
      }

      Item secondaryDrop() {
         return this.secondaryDrop;
      }

      Item tertiaryDrop() {
         return this.tertiaryDrop;
      }

      String archiveLine() {
         return this.archiveLine;
      }

      String phaseLine(int phase) {
         return switch (this) {
            case FALSE_ECHO -> phase >= 4
               ? "ECHO-7 // False tutorial channel collapsing. Do not follow the arrows."
               : "ECHO-7 // False ECHO shifted projection phase " + phase + ".";
            case COMMAND_REMNANT -> phase >= 4
               ? "ECHO-7 // War room lockdown final cycle. Doors are commands now."
               : "ECHO-7 // Command Remnant escalation phase " + phase + ".";
            case NEXUS_GUARDIAN -> phase >= 4
               ? "ECHO-7 // Core Awakening. Final choice pressure rising."
               : "ECHO-7 // Nexus Guardian phase " + phase + ": " + this.dialogue;
         };
      }

      String pressureLine(int phase) {
         return switch (this) {
            case FALSE_ECHO -> phase >= 3 ? "False ECHO // scanner disabled. trust me." : "False ECHO // warning: safe route detected.";
            case COMMAND_REMNANT -> phase >= 3 ? "COMMAND // drone strikes authorized. disobedience mapped." : "COMMAND // red laser markers acquired.";
            case NEXUS_GUARDIAN -> phase >= 4 ? "NEXUS // final choice vector open." : "NEXUS // gravity and memory rings desynchronized.";
         };
      }
   }
}
