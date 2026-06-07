package com.knoxhack.echorendercore.animation;

import java.util.Locale;

public enum Easing {
   LINEAR,
   EASE_IN,
   EASE_OUT,
   EASE_IN_OUT,
   STEP;

   public float apply(float t) {
      float clamped = Math.max(0.0F, Math.min(1.0F, t));
      return switch (this) {
         case LINEAR -> clamped;
         case EASE_IN -> clamped * clamped;
         case EASE_OUT -> 1.0F - (1.0F - clamped) * (1.0F - clamped);
         case EASE_IN_OUT -> clamped < 0.5F ? 2.0F * clamped * clamped : 1.0F - (float)Math.pow(-2.0F * clamped + 2.0F, 2.0D) * 0.5F;
         case STEP -> clamped >= 1.0F ? 1.0F : 0.0F;
      };
   }

   public static Easing byName(String value) {
      if (value == null || value.isBlank()) {
         return LINEAR;
      }
      String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
      try {
         return Easing.valueOf(normalized);
      } catch (IllegalArgumentException exception) {
         return LINEAR;
      }
   }
}
