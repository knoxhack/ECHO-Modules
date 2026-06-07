package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualFallbackMode {
   STABLE("stable", true),
   VANILLA_RENDERER("vanilla_renderer", true),
   PARTICLES_ONLY("particles_only", true),
   MINIMAL_SCREEN("minimal_screen", true),
   DISABLED("disabled", true),
   UNSUPPORTED("unsupported", false);

   private final String id;
   private final boolean supported;

   VisualFallbackMode(String id, boolean supported) {
      this.id = id;
      this.supported = supported;
   }

   public String id() {
      return id;
   }

   public boolean supported() {
      return supported;
   }

   public static VisualFallbackMode byName(String name) {
      if (name == null || name.isBlank()) {
         return STABLE;
      }
      String normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
      for (VisualFallbackMode mode : values()) {
         if (mode.id.equals(normalized) || mode.name().equalsIgnoreCase(normalized)) {
            return mode;
         }
      }
      return UNSUPPORTED;
   }
}
