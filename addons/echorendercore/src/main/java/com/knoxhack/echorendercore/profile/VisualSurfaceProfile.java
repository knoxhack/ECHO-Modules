package com.knoxhack.echorendercore.profile;

import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public record VisualSurfaceProfile(
   VisualSurfaceType type,
   String ownerAddon,
   String displayName,
   List<String> tags,
   String integrationMode
) {
   public static VisualSurfaceProfile defaults(Identifier profileId) {
      String namespace = profileId == null ? "unknown" : profileId.getNamespace();
      String display = profileId == null ? "Unknown Surface" : profileId.getPath().replace('/', ' ').replace('_', ' ');
      return new VisualSurfaceProfile(VisualSurfaceType.UNKNOWN, namespace, display, List.of(), "optional");
   }

   public VisualSurfaceProfile {
      type = type == null ? VisualSurfaceType.UNKNOWN : type;
      ownerAddon = normalize(ownerAddon, "unknown");
      displayName = displayName == null || displayName.isBlank() ? ownerAddon : displayName.trim();
      tags = tags == null ? List.of() : tags.stream()
         .filter(value -> value != null && !value.isBlank())
         .map(value -> value.trim().toLowerCase(Locale.ROOT).replace(' ', '_'))
         .distinct()
         .sorted()
         .toList();
      integrationMode = normalize(integrationMode, "optional");
   }

   public boolean screenLike() {
      return type == VisualSurfaceType.SCREEN || type == VisualSurfaceType.HUD_OVERLAY;
   }

   private static String normalize(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
   }
}
