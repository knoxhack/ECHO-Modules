package com.knoxhack.echorendercore.profile;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public record VisualQaProfile(
   boolean required,
   Set<String> evidence,
   boolean reducedMotion
) {
   public static final Set<String> SUPPORTED_EVIDENCE = Set.of("screenshot", "advanced_fx", "screen_chrome", "world_surface");
   public static final VisualQaProfile DEFAULT = new VisualQaProfile(false, Set.of(), false);

   public VisualQaProfile {
      evidence = evidence == null ? Set.of() : normalize(evidence);
   }

   public boolean supportedEvidenceOnly() {
      return SUPPORTED_EVIDENCE.containsAll(evidence);
   }

   private static Set<String> normalize(Set<String> values) {
      TreeSet<String> normalized = new TreeSet<>();
      for (String value : values) {
         if (value != null && !value.isBlank()) {
            normalized.add(value.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_').replace('-', '_'));
         }
      }
      return Collections.unmodifiableSet(new LinkedHashSet<>(normalized));
    }
}
