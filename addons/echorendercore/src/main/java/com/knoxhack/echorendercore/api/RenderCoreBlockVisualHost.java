package com.knoxhack.echorendercore.api;

import net.minecraft.resources.Identifier;

/**
 * Common/server-safe visual contract for block entities and block-state backed surfaces.
 */
public interface RenderCoreBlockVisualHost extends IAdvancedVisualBlockEntity {
   default float visualProgress() {
      return 0.0F;
   }

   default boolean visualMoving() {
      return false;
   }

   default boolean visualDamaged() {
      return false;
   }

   default Identifier visualParticleProfileId() {
      return null;
   }

   default String visualSurfaceType() {
      return "block_entity";
   }

   default String visualFallbackStatus() {
      return "rendercore_native";
   }
}
