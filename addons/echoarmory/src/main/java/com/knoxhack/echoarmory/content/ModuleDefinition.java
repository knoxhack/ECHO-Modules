package com.knoxhack.echoarmory.content;

import java.util.List;
import net.minecraft.resources.Identifier;

public record ModuleDefinition(
   Identifier id,
   String title,
   String slotType,
   String effectType,
   float damageBonus,
   int defenseBonus,
   int energyCost,
   int instability,
   int toxicProtection,
   int radiationProtection,
   int coldProtection,
   int heatProtection,
   int fractureProtection,
   List<String> compatibleTypes,
   List<String> synergyTags
) {
   public ModuleDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Module id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      slotType = slotType == null || slotType.isBlank() ? "utility" : slotType.strip();
      effectType = effectType == null || effectType.isBlank() ? "utility" : effectType.strip();
      damageBonus = Math.max(0.0F, damageBonus);
      defenseBonus = Math.max(0, defenseBonus);
      energyCost = Math.max(0, energyCost);
      instability = Math.max(0, Math.min(100, instability));
      toxicProtection = clamp(toxicProtection);
      radiationProtection = clamp(radiationProtection);
      coldProtection = clamp(coldProtection);
      heatProtection = clamp(heatProtection);
      fractureProtection = clamp(fractureProtection);
      compatibleTypes = List.copyOf(compatibleTypes == null ? List.of() : compatibleTypes);
      synergyTags = List.copyOf(synergyTags == null ? List.of() : synergyTags);
   }

   public boolean compatibleWith(GearDefinition gear) {
      return gear != null && (compatibleTypes.isEmpty() || compatibleTypes.contains(gear.baseType()) || compatibleTypes.stream().anyMatch(gear.tags()::contains));
   }

   private static int clamp(int value) {
      return Math.max(0, Math.min(100, value));
   }
}
