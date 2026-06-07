package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import com.knoxhack.echorendercore.profile.ParticleProfile;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RenderCoreEntityVisuals {
   private static final Map<EntityRenderState, RenderData> ACTIVE_RENDER_DATA = new WeakHashMap<>();
   private static final Map<Integer, Long> LAST_PARTICLE_TICK = new ConcurrentHashMap<>();

   private RenderCoreEntityVisuals() {
   }

   public static RenderData attach(Entity entity, EntityRenderState renderState, Identifier profileId,
         Identifier fallbackTexture, float partialTick) {
      return attach(entity, renderState, profileId, fallbackTexture, VisualVariant.DEFAULT, partialTick, "rendercore_native");
   }

   public static RenderData attach(Entity entity, EntityRenderState renderState, Identifier profileId,
         Identifier fallbackTexture, VisualVariant variant, float partialTick, String fallbackStatus) {
      if (entity == null || renderState == null || profileId == null) {
         return null;
      }
      boolean moving = moving(entity);
      boolean damaged = damaged(entity);
      VisualState visualState = DebugVisualOverrides.entity(entity.getUUID())
         .orElse(damaged ? VisualState.DAMAGED : moving ? VisualState.ACTIVE : VisualState.IDLE);
      float progress = progress(entity);
      VisualContext context = new VisualContext(
         profileId,
         visualState,
         variant,
         progress,
         renderState.ageInTicks,
         partialTick,
         moving,
         damaged,
         renderState.lightCoords
      );
      VisualProfile visualProfile = RenderCoreProfiles.visual(profileId);
      Identifier particleProfileId = visualProfile != null && visualProfile.particleProfile() != null
         ? visualProfile.particleProfile()
         : profileId;
      RenderData renderData = new RenderData(
         entity,
         profileId,
         fallbackTexture,
         context,
         visualProfile,
         particleProfileId,
         RenderCoreProfiles.particle(particleProfileId),
         fallbackStatus == null || fallbackStatus.isBlank() ? "rendercore_native" : fallbackStatus
      );
      synchronized (ACTIVE_RENDER_DATA) {
         ACTIVE_RENDER_DATA.put(renderState, renderData);
      }
      ParticleProfile particleProfile = renderData.particleProfile();
      RenderCoreDebugTargets.rememberEntity(
         entity,
         visualProfile,
         context,
         particleProfile == null ? null : renderData.particleProfileId(),
         particleProfile == null ? 0 : particleProfile.emitters().size(),
         0,
         renderData.fallbackStatus()
      );
      return renderData;
   }

   public static RenderData get(EntityRenderState renderState) {
      synchronized (ACTIVE_RENDER_DATA) {
         return ACTIVE_RENDER_DATA.get(renderState);
      }
   }

   public static void spawnParticlesOncePerTick(RenderData renderData, EntityModel<?> model) {
      if (renderData == null || renderData.visualProfile() == null || renderData.particleProfile() == null) {
         return;
      }
      Entity entity = renderData.entity();
      if (entity == null) {
         return;
      }
      long tick = entity.level().getGameTime();
      Long previous = LAST_PARTICLE_TICK.put(entity.getId(), tick);
      if (previous != null && previous == tick) {
         return;
      }
      VisualContext context = renderData.context();
      RenderCoreParticleSpawner.spawnForEntity(
         entity,
         renderData.visualProfile(),
         renderData.particleProfile(),
         context.state(),
         context.moving(),
         context.damaged(),
         context.progress(),
         model
      );
   }

   private static boolean moving(Entity entity) {
      Vec3 delta = entity.getDeltaMovement();
      return delta.horizontalDistanceSqr() > 4.0E-4D || Math.abs(delta.y) > 0.01D;
   }

   private static boolean damaged(Entity entity) {
      if (entity instanceof LivingEntity living && living.getMaxHealth() > 0.0F) {
         return living.getHealth() <= living.getMaxHealth() * 0.45F;
      }
      return false;
   }

   private static float progress(Entity entity) {
      if (entity instanceof LivingEntity living && living.getMaxHealth() > 0.0F) {
         return Math.max(0.0F, Math.min(1.0F, living.getHealth() / living.getMaxHealth()));
      }
      return 1.0F;
   }

   public record RenderData(
      Entity entity,
      Identifier profileId,
      Identifier fallbackTexture,
      VisualContext context,
      VisualProfile visualProfile,
      Identifier particleProfileId,
      ParticleProfile particleProfile,
      String fallbackStatus
   ) {
   }
}
