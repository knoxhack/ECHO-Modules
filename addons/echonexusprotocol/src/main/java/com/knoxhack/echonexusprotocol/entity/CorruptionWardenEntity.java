package com.knoxhack.echonexusprotocol.entity;

import com.knoxhack.echonexusprotocol.registry.ModEntities;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class CorruptionWardenEntity extends NexusMobEntity {
   private int lastTelegraphTick = -1;
   private int pulseCount;
   private int telegraphCount;

   public CorruptionWardenEntity(EntityType<? extends CorruptionWardenEntity> type, Level level) { super(type, level); }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel serverLevel) {
         if (this.tickCount % 100 == 80) {
            telegraphPulse(serverLevel);
         }
         if (this.tickCount == 1 || this.tickCount % 100 == 0) {
            pulse(serverLevel);
         }
      }
   }

   public void pulse(ServerLevel serverLevel) {
      this.pulseCount++;
      NexusWorldData.get(serverLevel).addCorruptionPressure(new ChunkPos(this.blockPosition().getX() >> 4, this.blockPosition().getZ() >> 4), 4);
      serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
      serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.9D, this.getZ(), 24, 1.2D, 0.7D, 1.2D, 0.08D);
      serverLevel.playSound(null, this.blockPosition(), ModSounds.WARDEN_PULSE.get(), SoundSource.HOSTILE, 0.8F, 0.8F);
      for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.0D))) {
         player.hurtServer(serverLevel, this.damageSources().magic(), this.scaledBossDamage(4.0F));
      }
      int crawlers = serverLevel.getEntitiesOfClass(StaticCrawlerEntity.class, this.getBoundingBox().inflate(12.0D)).size();
      boolean hasCloseCrawler = !serverLevel.getEntitiesOfClass(StaticCrawlerEntity.class, this.getBoundingBox().inflate(5.0D)).isEmpty();
      if (crawlers >= 4 && hasCloseCrawler) {
         return;
      }
      Entity crawler = ModEntities.STATIC_CRAWLER.get().create(serverLevel, EntitySpawnReason.EVENT);
      if (crawler != null) {
         crawler.setPos(this.getX() + this.random.nextDouble() * 3.0D - 1.5D, this.getY(), this.getZ() + this.random.nextDouble() * 3.0D - 1.5D);
         serverLevel.addFreshEntity(crawler);
      }
   }

   public int pulseCount() {
      return this.pulseCount;
   }

   public int telegraphCount() {
      return this.telegraphCount;
   }

   public int lastTelegraphTick() {
      return this.lastTelegraphTick;
   }

   private void telegraphPulse(ServerLevel serverLevel) {
      this.lastTelegraphTick = this.tickCount;
      this.telegraphCount++;
      serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2D, this.getZ(), 16, 0.7D, 0.45D, 0.7D, 0.04D);
      serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.8D, this.getZ(), 12, 1.0D, 0.4D, 1.0D, 0.05D);
      serverLevel.playSound(null, this.blockPosition(), ModSounds.WARDEN_PULSE.get(), SoundSource.HOSTILE, 0.45F, 1.25F);
   }
}
