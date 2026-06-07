package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualSurfaceType {
   ENTITY("entity", true),
   BLOCK_ENTITY("block_entity", true),
   STATIC_BLOCK("static_block", true),
   SCREEN("screen", true),
   HUD_OVERLAY("hud_overlay", true),
   PARTICLE_ONLY("particle_only", true),
   WEATHER("weather", true),
   MOB_FAMILY("mob_family", true),
   UNKNOWN("unknown", true),
   UNSUPPORTED("unsupported", false);

   private final String id;
   private final boolean supported;

   VisualSurfaceType(String id, boolean supported) {
      this.id = id;
      this.supported = supported;
   }

   public String id() {
      return id;
   }

   public boolean supported() {
      return supported;
   }

   public static VisualSurfaceType byName(String name) {
      if (name == null || name.isBlank()) {
         return UNKNOWN;
      }
      String normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
      for (VisualSurfaceType type : values()) {
         if (type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
            return type;
         }
      }
      return UNSUPPORTED;
   }
}
