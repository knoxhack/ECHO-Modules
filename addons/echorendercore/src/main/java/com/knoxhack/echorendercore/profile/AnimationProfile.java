package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.animation.AnimationClip;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record AnimationProfile(Identifier id, Map<String, AnimationClip> animations) {
   public AnimationProfile {
      animations = animations == null ? Map.of() : Map.copyOf(animations);
   }

   public AnimationClip clip(String name) {
      if (name == null || name.isBlank()) {
         return null;
      }
      return animations.get(name);
   }

   public static AnimationProfile empty(Identifier id) {
      return new AnimationProfile(id, Map.of());
   }
}
