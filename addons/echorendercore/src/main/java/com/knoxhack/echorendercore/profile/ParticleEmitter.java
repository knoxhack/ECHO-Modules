package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.api.VisualState;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record ParticleEmitter(
   String id,
   String anchor,
   Identifier particle,
   VisualState state,
   Set<VisualState> states,
   float rate,
   int burstCount,
   RenderCoreVector offset,
   RenderCoreVector velocity,
   RenderCoreVector spread,
   int lifetime,
   int color,
   boolean requiresMoving,
   boolean requiresDamaged,
   float minProgress,
   float maxProgress,
   ParticleOptionsSpec options
) {
   public ParticleEmitter(String id, String anchor, Identifier particle, VisualState state, float rate, int burstCount,
         RenderCoreVector offset, RenderCoreVector velocity, RenderCoreVector spread, int lifetime, int color) {
      this(id, anchor, particle, state, Set.of(), rate, burstCount, offset, velocity, spread, lifetime, color,
         false, false, 0.0F, 1.0F, new ParticleOptionsSpec(color, 1.0F, lifetime, java.util.Map.of()));
   }

   public ParticleEmitter {
      id = id == null || id.isBlank() ? "emitter" : id.trim();
      anchor = anchor == null ? "" : anchor.trim();
      states = states == null ? Set.of() : Set.copyOf(states);
      rate = Math.max(0.0F, rate);
      burstCount = Math.max(0, burstCount);
      offset = offset == null ? RenderCoreVector.ZERO : offset;
      velocity = velocity == null ? RenderCoreVector.ZERO : velocity;
      spread = spread == null ? RenderCoreVector.ZERO : spread;
      lifetime = Math.max(0, lifetime);
      minProgress = Math.max(0.0F, Math.min(1.0F, minProgress));
      maxProgress = Math.max(minProgress, Math.min(1.0F, maxProgress));
      options = options == null ? new ParticleOptionsSpec(color, 1.0F, lifetime, java.util.Map.of()) : options;
   }

   public boolean matches(VisualState visualState) {
      return (state == null && states.isEmpty()) || state == visualState || states.contains(visualState);
   }

   public boolean runtimeMatches(VisualState visualState, boolean moving, boolean damaged, float progress) {
      return matches(visualState)
         && (!requiresMoving || moving)
         && (!requiresDamaged || damaged || visualState == VisualState.DAMAGED)
         && progress >= minProgress
         && progress <= maxProgress;
   }
}
