package com.knoxhack.echorendercore.profile;

import java.util.Locale;
import java.util.Set;

public record VisualScreenChromeProfile(
   String style,
   String label,
   boolean backdrop,
   boolean edgeGlow,
   boolean cornerBrackets,
   boolean accentRails,
   boolean scanlines,
   boolean glassGlints,
   boolean chromaticEdge,
   boolean quietFallback
) {
   public static final Set<String> SUPPORTED_STYLES = Set.of("minimal", "cyberglass", "terminal", "hologram", "neon");
   public static final VisualScreenChromeProfile DEFAULT =
      new VisualScreenChromeProfile("cyberglass", "", true, true, true, true, false, true, true, false);

   public VisualScreenChromeProfile {
      style = normalize(style, "cyberglass");
      label = label == null ? "" : label.trim();
   }

   public boolean supportedStyle() {
      return SUPPORTED_STYLES.contains(style);
   }

   public boolean drawLabel() {
      return !label.isBlank();
   }

   private static String normalize(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
   }
}
