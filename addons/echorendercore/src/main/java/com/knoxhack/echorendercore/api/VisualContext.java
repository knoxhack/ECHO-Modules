package com.knoxhack.echorendercore.api;

import net.minecraft.resources.Identifier;

public record VisualContext(
   Identifier profileId,
   VisualState state,
   VisualVariant variant,
   float progress,
   float ageInTicks,
   float partialTick,
   boolean moving,
   boolean damaged,
   int packedLight
) {
   public VisualContext {
      state = state == null ? VisualState.IDLE : state;
      variant = variant == null ? VisualVariant.DEFAULT : variant;
      progress = clamp01(progress);
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
