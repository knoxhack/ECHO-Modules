package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualLightMode {
   PROFILE,
   PACKED,
   FULLBRIGHT,
   EMISSIVE,
   UNSUPPORTED;

   public static VisualLightMode byName(String name) {
      if (name == null || name.isBlank()) {
         return PROFILE;
      }
      return switch (name.trim().toLowerCase(Locale.ROOT)) {
         case "profile" -> PROFILE;
         case "packed", "world" -> PACKED;
         case "fullbright", "full_bright" -> FULLBRIGHT;
         case "emissive", "glow" -> EMISSIVE;
         default -> UNSUPPORTED;
      };
   }

   public boolean supported() {
      return this != UNSUPPORTED;
   }

   public boolean fullbright() {
      return this == FULLBRIGHT || this == EMISSIVE;
   }
}
