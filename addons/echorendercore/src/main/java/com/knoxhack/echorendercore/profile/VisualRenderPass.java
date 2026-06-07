package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualRenderPass {
   AUTO,
   BASE,
   CUTOUT,
   TRANSLUCENT,
   EMISSIVE,
   UNSUPPORTED;

   public static VisualRenderPass byName(String name) {
      if (name == null || name.isBlank()) {
         return AUTO;
      }
      return switch (name.trim().toLowerCase(Locale.ROOT)) {
         case "auto" -> AUTO;
         case "base", "solid" -> BASE;
         case "cutout", "cut_out" -> CUTOUT;
         case "translucent", "transparent" -> TRANSLUCENT;
         case "emissive", "glow", "eyes" -> EMISSIVE;
         default -> UNSUPPORTED;
      };
   }

   public boolean supported() {
      return this != UNSUPPORTED;
   }
}
