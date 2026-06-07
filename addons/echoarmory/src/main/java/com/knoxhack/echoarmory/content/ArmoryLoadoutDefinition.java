package com.knoxhack.echoarmory.content;

import com.knoxhack.echoarmory.item.ArmoryData;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record ArmoryLoadoutDefinition(
   Identifier id,
   String title,
   int order,
   Identifier icon,
   String weapon,
   List<String> armor,
   List<String> modules,
   int minTier,
   int minProtection,
   Map<ArmoryData.ProtectionType, Integer> requiredProtections,
   String logisticsPreset
) {
   public ArmoryLoadoutDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Loadout id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      icon = icon == null ? Identifier.withDefaultNamespace("chest") : icon;
      weapon = weapon == null ? "" : weapon.strip();
      armor = List.copyOf(armor == null ? List.of() : armor);
      modules = List.copyOf(modules == null ? List.of() : modules);
      minTier = Math.max(1, Math.min(4, minTier));
      minProtection = Math.max(0, minProtection);
      requiredProtections = sanitizeProtections(requiredProtections, minProtection);
      logisticsPreset = logisticsPreset == null ? "" : logisticsPreset.strip();
   }

   private static Map<ArmoryData.ProtectionType, Integer> sanitizeProtections(
      Map<ArmoryData.ProtectionType, Integer> requirements,
      int fallbackFracture
   ) {
      if (requirements == null) {
         return fallbackFracture <= 0
            ? Map.of()
            : Map.of(ArmoryData.ProtectionType.FRACTURE, Math.max(0, fallbackFracture));
      }
      java.util.EnumMap<ArmoryData.ProtectionType, Integer> sanitized = new java.util.EnumMap<>(ArmoryData.ProtectionType.class);
      for (Map.Entry<ArmoryData.ProtectionType, Integer> entry : requirements.entrySet()) {
         if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
            sanitized.put(entry.getKey(), entry.getValue());
         }
      }
      return Map.copyOf(sanitized);
   }

   public int requiredProtection(ArmoryData.ProtectionType type) {
      return requiredProtections.getOrDefault(type, 0);
   }
}
