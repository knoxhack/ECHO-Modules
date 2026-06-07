package com.knoxhack.echorendercore.profile;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record ParticleProfile(Identifier id, Map<String, ParticleEmitter> emitters) {
   public ParticleProfile {
      emitters = emitters == null ? Map.of() : Map.copyOf(emitters);
   }

   public static ParticleProfile empty(Identifier id) {
      return new ParticleProfile(id, Map.of());
   }
}
