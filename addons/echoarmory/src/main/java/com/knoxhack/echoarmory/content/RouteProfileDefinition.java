package com.knoxhack.echoarmory.content;

import com.knoxhack.echoarmory.item.ArmoryData;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record RouteProfileDefinition(
   Identifier id,
   String title,
   String routeFamily,
   String hazardType,
   String bossId,
   String loadoutId,
   int minTier,
   Map<ArmoryData.ProtectionType, Integer> requiredProtections,
   List<String> recommendedTags,
   int order
) {
   public RouteProfileDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Route profile id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      routeFamily = routeFamily == null || routeFamily.isBlank() ? id.getPath() : routeFamily.strip();
      hazardType = hazardType == null ? "" : hazardType.strip();
      bossId = bossId == null ? "" : bossId.strip();
      loadoutId = loadoutId == null ? "" : loadoutId.strip();
      minTier = Math.max(1, Math.min(4, minTier));
      requiredProtections = sanitize(requiredProtections);
      recommendedTags = List.copyOf(recommendedTags == null ? List.of() : recommendedTags);
      order = Math.max(0, order);
   }

   private static Map<ArmoryData.ProtectionType, Integer> sanitize(Map<ArmoryData.ProtectionType, Integer> requirements) {
      if (requirements == null || requirements.isEmpty()) {
         return Map.of();
      }
      EnumMap<ArmoryData.ProtectionType, Integer> sanitized = new EnumMap<>(ArmoryData.ProtectionType.class);
      for (Map.Entry<ArmoryData.ProtectionType, Integer> entry : requirements.entrySet()) {
         if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
            sanitized.put(entry.getKey(), Math.min(100, entry.getValue()));
         }
      }
      return Map.copyOf(sanitized);
   }
}
