package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RenderCoreDebugTargets {
   private static final Map<String, DebugTarget> TARGETS = new LinkedHashMap<>();
   private static final int MAX_TARGETS = 96;
   private static final long TTL_TICKS = 8L;

   private RenderCoreDebugTargets() {
   }

   public static void rememberEntity(Entity entity, VisualProfile profile, VisualContext context) {
      rememberEntity(entity, profile, context, null, 0, 0, "entity", "fallback_renderer");
   }

   public static void rememberEntity(Entity entity, VisualProfile profile, VisualContext context,
         Identifier particleProfileId, int activeEmitters, int skippedEmitters, String fallbackStatus) {
      rememberEntity(entity, profile, context, particleProfileId, activeEmitters, skippedEmitters, "entity", fallbackStatus);
   }

   public static void rememberEntity(Entity entity, VisualProfile profile, VisualContext context,
         Identifier particleProfileId, int activeEmitters, int skippedEmitters, String surfaceType, String fallbackStatus) {
      if (entity == null || profile == null || context == null) {
         return;
      }
      Map<String, Vec3> anchors = new LinkedHashMap<>();
      for (String anchor : profile.anchors().keySet()) {
         anchors.put(anchor, ParticleAnchorResolver.resolve(entity, profile, anchor));
      }
      remember(new DebugTarget(
         entityKey(entity),
         context.profileId(),
         context.state(),
         context.variant(),
         profile.layersFor(context.state(), context.variant()).size(),
         profile.anchors().size(),
         particleProfileId,
         activeEmitters,
         skippedEmitters,
         surfaceType,
         fallbackStatus,
         RenderCoreProfiles.loaded().validationReport().forNamespace(context.profileId().getNamespace()).warnings(),
         anchors,
         entity.getBoundingBox(),
         tick(entity.level())
      ));
   }

   public static void rememberBlock(Level level, BlockPos pos, VisualProfile profile, VisualContext context) {
      rememberBlock(level, pos, profile, context, null, 0, 0, "block_entity", "block_visual");
   }

   public static void rememberBlock(Level level, BlockPos pos, VisualProfile profile, VisualContext context,
         Identifier particleProfileId, int activeEmitters, int skippedEmitters, String surfaceType, String fallbackStatus) {
      if (level == null || pos == null || profile == null || context == null) {
         return;
      }
      Vec3 origin = Vec3.atCenterOf(pos);
      Map<String, Vec3> anchors = new LinkedHashMap<>();
      for (String anchor : profile.anchors().keySet()) {
         anchors.put(anchor, ParticleAnchorResolver.resolve(origin, profile, anchor));
      }
      remember(new DebugTarget(
         blockKey(level, pos),
         context.profileId(),
         context.state(),
         context.variant(),
         profile.layersFor(context.state(), context.variant()).size(),
         profile.anchors().size(),
         particleProfileId,
         activeEmitters,
         skippedEmitters,
         surfaceType,
         fallbackStatus,
         RenderCoreProfiles.loaded().validationReport().forNamespace(context.profileId().getNamespace()).warnings(),
         anchors,
         new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D),
         tick(level)
      ));
   }

   public static Optional<DebugTarget> lookedAt(Minecraft minecraft) {
      if (minecraft == null || minecraft.level == null || minecraft.hitResult == null) {
         return Optional.empty();
      }
      HitResult hit = minecraft.hitResult;
      if (hit instanceof EntityHitResult entityHit) {
         return Optional.ofNullable(TARGETS.get(entityKey(entityHit.getEntity()))).filter(RenderCoreDebugTargets::fresh);
      }
      if (hit instanceof BlockHitResult blockHit) {
         return Optional.ofNullable(TARGETS.get(blockKey(minecraft.level, blockHit.getBlockPos()))).filter(RenderCoreDebugTargets::fresh);
      }
      return Optional.empty();
   }

   public static List<DebugTarget> visibleTargets() {
      return TARGETS.values().stream().filter(RenderCoreDebugTargets::fresh).toList();
   }

   private static void remember(DebugTarget target) {
      TARGETS.put(target.key(), target);
      while (TARGETS.size() > MAX_TARGETS) {
         String first = TARGETS.keySet().iterator().next();
         TARGETS.remove(first);
      }
   }

   private static boolean fresh(DebugTarget target) {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.level == null || tick(minecraft.level) - target.lastSeenTick() <= TTL_TICKS;
   }

   private static String entityKey(Entity entity) {
      return "entity:" + entity.getUUID();
   }

   private static String blockKey(Level level, BlockPos pos) {
      Identifier dimension = level.dimension().identifier();
      return "block:" + dimension + ":" + pos.asLong();
   }

   private static long tick(Level level) {
      return level == null ? 0L : level.getGameTime();
   }

   public record DebugTarget(
      String key,
      Identifier profileId,
      VisualState state,
      VisualVariant variant,
      int activeLayers,
      int anchors,
      Identifier particleProfileId,
      int activeEmitters,
      int skippedEmitters,
      String surfaceType,
      String fallbackStatus,
      long validationWarnings,
      Map<String, Vec3> anchorPositions,
      AABB bounds,
      long lastSeenTick
   ) {
      public DebugTarget {
         variant = variant == null ? VisualVariant.DEFAULT : variant;
         activeEmitters = Math.max(0, activeEmitters);
         skippedEmitters = Math.max(0, skippedEmitters);
         surfaceType = surfaceType == null || surfaceType.isBlank() ? "unknown" : surfaceType;
         fallbackStatus = fallbackStatus == null || fallbackStatus.isBlank() ? "unknown" : fallbackStatus;
         anchorPositions = anchorPositions == null ? Map.of() : Map.copyOf(anchorPositions);
      }
   }
}
