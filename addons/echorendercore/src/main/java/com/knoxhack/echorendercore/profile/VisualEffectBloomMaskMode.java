package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualEffectBloomMaskMode {
   AUTO("auto", true),
   EMISSIVE("emissive", true),
   LAYER_ALPHA("layer_alpha", true),
   SOLID("solid", true),
   NONE("none", true),
   UNSUPPORTED("unsupported", false);

   private final String id;
   private final boolean supported;

   VisualEffectBloomMaskMode(String id, boolean supported) {
      this.id = id;
      this.supported = supported;
   }

   public String id() {
      return id;
   }

   public boolean supported() {
      return supported;
   }

   public static VisualEffectBloomMaskMode byName(String name) {
      if (name == null || name.isBlank()) {
         return AUTO;
      }
      String normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
      for (VisualEffectBloomMaskMode mode : values()) {
         if (mode.id.equals(normalized)) {
            return mode;
         }
      }
      return UNSUPPORTED;
   }
}
