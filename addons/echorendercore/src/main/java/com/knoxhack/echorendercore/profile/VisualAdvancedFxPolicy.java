package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualAdvancedFxPolicy {
   DENY("deny", true),
   AUTO("auto", true),
   ALLOW("allow", true),
   UNSUPPORTED("unsupported", false);

   private final String id;
   private final boolean supported;

   VisualAdvancedFxPolicy(String id, boolean supported) {
      this.id = id;
      this.supported = supported;
   }

   public String id() {
      return id;
   }

   public boolean supported() {
      return supported;
   }

   public static VisualAdvancedFxPolicy byName(String name) {
      if (name == null || name.isBlank()) {
         return AUTO;
      }
      String normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
      for (VisualAdvancedFxPolicy policy : values()) {
         if (policy.id.equals(normalized) || policy.name().equalsIgnoreCase(normalized)) {
            return policy;
         }
      }
      return UNSUPPORTED;
   }
}
