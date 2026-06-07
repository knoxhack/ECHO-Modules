package com.knoxhack.echoarmory.content;

import java.util.List;
import net.minecraft.resources.Identifier;

public record GearDefinition(
   Identifier id,
   String title,
   String baseType,
   int tier,
   int moduleSlots,
   float baseDamage,
   int baseDefense,
   int energyCapacity,
   String craftingStage,
   String factionGate,
   List<String> allowedSlots,
   List<String> tags
) {
   public GearDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Gear id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      baseType = baseType == null || baseType.isBlank() ? "utility" : baseType.strip();
      tier = Math.max(1, Math.min(4, tier));
      moduleSlots = Math.max(0, Math.min(8, moduleSlots));
      baseDamage = Math.max(0.0F, baseDamage);
      baseDefense = Math.max(0, baseDefense);
      energyCapacity = Math.max(0, energyCapacity);
      craftingStage = craftingStage == null || craftingStage.isBlank() ? "Tier " + tier : craftingStage.strip();
      factionGate = factionGate == null ? "" : factionGate.strip();
      allowedSlots = List.copyOf(allowedSlots == null ? List.of() : allowedSlots);
      tags = List.copyOf(tags == null ? List.of() : tags);
   }

   public boolean allows(ModuleDefinition module) {
      if (module == null) {
         return false;
      }
      return allowedSlots.isEmpty() || allowedSlots.contains(module.slotType());
   }
}
