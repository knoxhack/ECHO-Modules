package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.profile.ParticleEmitter;
import com.knoxhack.echorendercore.profile.ParticleProfile;
import com.knoxhack.echorendercore.profile.RenderCoreVector;
import com.knoxhack.echorendercore.profile.VisualProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RenderCoreParticleSpawner {
   private static final RandomSource RANDOM = RandomSource.create();

   private RenderCoreParticleSpawner() {
   }

   public static void spawnForEntity(Entity entity, VisualProfile visualProfile, ParticleProfile particleProfile,
         VisualState state, boolean moving, boolean damaged) {
      spawnForEntity(entity, visualProfile, particleProfile, state, moving, damaged, 0.0F);
   }

   public static void spawnForEntity(Entity entity, VisualProfile visualProfile, ParticleProfile particleProfile,
         VisualState state, boolean moving, boolean damaged, float progress) {
      spawnForEntity(entity, visualProfile, particleProfile, state, moving, damaged, progress, null);
   }

   public static void spawnForEntity(Entity entity, VisualProfile visualProfile, ParticleProfile particleProfile,
         VisualState state, boolean moving, boolean damaged, float progress, EntityModel<?> model) {
      if (entity == null || particleProfile == null || particleProfile.emitters().isEmpty()) {
         return;
      }
      Level level = Minecraft.getInstance().level;
      if (level == null) {
         return;
      }
      RandomSource random = RANDOM;
      for (ParticleEmitter emitter : particleProfile.emitters().values()) {
         if (!emitter.runtimeMatches(state, moving, damaged, progress) || !runtimeCondition(emitter, state, moving, damaged)) {
            continue;
         }
         if (emitter.rate() < 1.0F && random.nextFloat() > emitter.rate()) {
            continue;
         }
         ParticleOptions particleOptions = RenderCoreParticleOptionResolvers.resolve(emitter);
         if (particleOptions == null) {
            if (DebugVisualOverrides.missingPartWarnings()) {
               EchoRenderCore.LOGGER.warn("RenderCore particle {} is not a simple client particle; emitter {} skipped.",
                  emitter.particle(), emitter.id());
            }
            continue;
         }
         int count = Math.max(1, emitter.burstCount());
         for (int i = 0; i < count; i++) {
            Vec3 anchor = ParticleAnchorResolver.resolve(entity, visualProfile, emitter.anchor(), model).add(
               jitter(random, emitter.offset().x(), emitter.spread().x()),
               jitter(random, emitter.offset().y(), emitter.spread().y()),
               jitter(random, emitter.offset().z(), emitter.spread().z())
            );
            RenderCoreVector velocity = emitter.velocity();
            level.addParticle(
               particleOptions,
               anchor.x,
               anchor.y,
               anchor.z,
               velocity.x() + spread(random, emitter.spread().x()),
               velocity.y() + spread(random, emitter.spread().y()),
               velocity.z() + spread(random, emitter.spread().z())
            );
         }
      }
   }

   public static void spawnForBlock(Level level, Vec3 origin, VisualProfile visualProfile, ParticleProfile particleProfile,
         VisualState state, boolean moving, boolean damaged, float progress) {
      if (level == null || origin == null || particleProfile == null || particleProfile.emitters().isEmpty()) {
         return;
      }
      RandomSource random = RANDOM;
      for (ParticleEmitter emitter : particleProfile.emitters().values()) {
         if (!emitter.runtimeMatches(state, moving, damaged, progress) || !runtimeCondition(emitter, state, moving, damaged)) {
            continue;
         }
         if (emitter.rate() < 1.0F && random.nextFloat() > emitter.rate()) {
            continue;
         }
         ParticleOptions particleOptions = RenderCoreParticleOptionResolvers.resolve(emitter);
         if (particleOptions == null) {
            continue;
         }
         int count = Math.max(1, emitter.burstCount());
         for (int i = 0; i < count; i++) {
            Vec3 anchor = ParticleAnchorResolver.resolve(origin, visualProfile, emitter.anchor()).add(
               jitter(random, emitter.offset().x(), emitter.spread().x()),
               jitter(random, emitter.offset().y(), emitter.spread().y()),
               jitter(random, emitter.offset().z(), emitter.spread().z())
            );
            RenderCoreVector velocity = emitter.velocity();
            level.addParticle(
               particleOptions,
               anchor.x,
               anchor.y,
               anchor.z,
               velocity.x() + spread(random, emitter.spread().x()),
               velocity.y() + spread(random, emitter.spread().y()),
               velocity.z() + spread(random, emitter.spread().z())
            );
         }
      }
   }

   private static boolean runtimeCondition(ParticleEmitter emitter, VisualState state, boolean moving, boolean damaged) {
      return true;
   }

   private static double jitter(RandomSource random, float base, float spread) {
      return base + spread(random, spread);
   }

   private static double spread(RandomSource random, float spread) {
      return spread <= 0.0F ? 0.0D : (random.nextDouble() - 0.5D) * spread;
   }
}
