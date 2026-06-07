package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.RenderCoreBlockVisualHost;
import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import com.knoxhack.echorendercore.profile.ParticleEmitter;
import com.knoxhack.echorendercore.profile.ParticleProfile;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RenderCoreBlockVisuals {
   private static final Map<String, Long> LAST_PARTICLE_TICK = new ConcurrentHashMap<>();

   private RenderCoreBlockVisuals() {
   }

   public static RenderData resolve(Level level, BlockPos pos, BlockState blockState, RenderCoreBlockVisualHost host,
         float partialTick, int packedLight) {
      if (level == null || pos == null || host == null || host.visualProfileId() == null) {
         return null;
      }
      VisualState state = DebugVisualOverrides.block(level, pos).orElse(host.visualState());
      VisualVariant variant = host.visualVariant();
      Identifier profileId = host.visualProfileId();
      VisualContext context = new VisualContext(
         profileId,
         state,
         variant,
         host.visualProgress(),
         level.getGameTime() + partialTick,
         partialTick,
         host.visualMoving(),
         host.visualDamaged(),
         packedLight
      );
      VisualProfile visualProfile = RenderCoreProfiles.visual(profileId);
      Identifier particleProfileId = host.visualParticleProfileId();
      if (particleProfileId == null && visualProfile != null) {
         particleProfileId = visualProfile.particleProfile();
      }
      ParticleProfile particleProfile = particleProfileId == null ? null : RenderCoreProfiles.particle(particleProfileId);
      EmitterCounts counts = countEmitters(particleProfile, context);
      RenderData data = new RenderData(
         level,
         pos.immutable(),
         blockState,
         Vec3.atCenterOf(pos),
         profileId,
         context,
         visualProfile,
         particleProfileId,
         particleProfile,
         host.visualSurfaceType(),
         host.visualFallbackStatus(),
         counts.active(),
         counts.skipped()
      );
      if (visualProfile != null) {
         RenderCoreDebugTargets.rememberBlock(
            level,
            pos,
            visualProfile,
            context,
            particleProfileId,
            counts.active(),
            counts.skipped(),
            data.surfaceType(),
            data.fallbackStatus()
         );
      }
      return data;
   }

   public static void spawnParticlesOncePerTick(RenderData data) {
      if (data == null || data.visualProfile() == null || data.particleProfile() == null) {
         return;
      }
      long tick = data.level().getGameTime();
      String key = data.level().dimension().identifier() + ":" + data.pos().asLong();
      Long previous = LAST_PARTICLE_TICK.put(key, tick);
      if (previous != null && previous == tick) {
         return;
      }
      VisualContext context = data.context();
      RenderCoreParticleSpawner.spawnForBlock(
         data.level(),
         data.origin(),
         data.visualProfile(),
         data.particleProfile(),
         context.state(),
         context.moving(),
         context.damaged(),
         context.progress()
      );
   }

   public static EmitterCounts countEmitters(ParticleProfile particleProfile, VisualContext context) {
      if (particleProfile == null || context == null) {
         return new EmitterCounts(0, 0);
      }
      int active = 0;
      int skipped = 0;
      for (ParticleEmitter emitter : particleProfile.emitters().values()) {
         if (emitter.runtimeMatches(context.state(), context.moving(), context.damaged(), context.progress())) {
            active++;
         } else {
            skipped++;
         }
      }
      return new EmitterCounts(active, skipped);
   }

   public static RenderCoreBlockVisualHost staticHost(Identifier profileId, String surfaceType) {
      return new RenderCoreBlockVisualHost() {
         @Override
         public Identifier visualProfileId() {
            return profileId;
         }

         @Override
         public VisualState visualState() {
            return VisualState.ACTIVE;
         }

         @Override
         public float visualProgress() {
            return 1.0F;
         }

         @Override
         public String visualSurfaceType() {
            return surfaceType == null || surfaceType.isBlank() ? "static_block" : surfaceType;
         }

         @Override
         public String visualFallbackStatus() {
            return "rendercore_particles_only";
         }
      };
   }

   public record RenderData(
      Level level,
      BlockPos pos,
      BlockState blockState,
      Vec3 origin,
      Identifier profileId,
      VisualContext context,
      VisualProfile visualProfile,
      Identifier particleProfileId,
      ParticleProfile particleProfile,
      String surfaceType,
      String fallbackStatus,
      int activeEmitters,
      int skippedEmitters
   ) {
      public RenderData {
         surfaceType = surfaceType == null || surfaceType.isBlank() ? "block_entity" : surfaceType;
         fallbackStatus = fallbackStatus == null || fallbackStatus.isBlank() ? "rendercore_native" : fallbackStatus;
      }
   }

   public record EmitterCounts(int active, int skipped) {
      public EmitterCounts {
         active = Math.max(0, active);
         skipped = Math.max(0, skipped);
      }
   }
}
