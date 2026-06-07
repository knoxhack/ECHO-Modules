package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualLayerKind {
   BASE,
   GLOW,
   OVERLAY;

   public static VisualLayerKind byName(String value) {
      if (value == null || value.isBlank()) {
         return OVERLAY;
      }
      String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
      try {
         return VisualLayerKind.valueOf(normalized);
      } catch (IllegalArgumentException exception) {
         return OVERLAY;
      }
   }
}
