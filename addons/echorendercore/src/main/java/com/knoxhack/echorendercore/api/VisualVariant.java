package com.knoxhack.echorendercore.api;

import java.util.Locale;

public record VisualVariant(String id) {
   public static final VisualVariant DEFAULT = new VisualVariant("default");

   public VisualVariant {
      id = normalize(id);
   }

   public boolean isDefault() {
      return DEFAULT.id.equals(id);
   }

   public static VisualVariant of(String id) {
      return new VisualVariant(id);
   }

   private static String normalize(String value) {
      if (value == null || value.isBlank()) {
         return "default";
      }
      return value.trim().toLowerCase(Locale.ROOT);
   }
}
