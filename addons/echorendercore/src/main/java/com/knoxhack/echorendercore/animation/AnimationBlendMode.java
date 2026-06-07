package com.knoxhack.echorendercore.animation;

import java.util.Locale;

public enum AnimationBlendMode {
   REPLACE,
   ADDITIVE;

   public static AnimationBlendMode byName(String value) {
      if (value == null || value.isBlank()) {
         return REPLACE;
      }
      String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
      try {
         return AnimationBlendMode.valueOf(normalized);
      } catch (IllegalArgumentException exception) {
         return REPLACE;
      }
   }
}
