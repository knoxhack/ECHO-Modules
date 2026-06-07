package com.knoxhack.echoarmory.content;

import java.util.List;
import net.minecraft.resources.Identifier;

public record FiringModeDefinition(
   Identifier id,
   String title,
   String family,
   ProjectileKind projectileKind,
   int ammoCost,
   int energyCost,
   int cooldownTicks,
   double range,
   double velocity,
   float damageScale,
   int instability,
   List<String> gearTags
) {
   public FiringModeDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Firing mode id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      family = family == null || family.isBlank() ? "energy" : family.strip();
      projectileKind = projectileKind == null ? ProjectileKind.ENERGY_BOLT : projectileKind;
      ammoCost = Math.max(0, ammoCost);
      energyCost = Math.max(0, energyCost);
      cooldownTicks = Math.max(1, cooldownTicks);
      range = Math.max(2.0D, range);
      velocity = Math.max(0.1D, velocity);
      damageScale = Math.max(0.1F, damageScale);
      instability = Math.max(0, Math.min(100, instability));
      gearTags = List.copyOf(gearTags == null ? List.of() : gearTags);
   }

   public boolean matches(GearDefinition gear) {
      if (gear == null) {
         return false;
      }
      if (family.equals(gear.baseType()) || gear.tags().contains(family)) {
         return true;
      }
      return gearTags.isEmpty() || gearTags.stream().anyMatch(tag -> gear.baseType().equals(tag) || gear.tags().contains(tag));
   }

   public enum ProjectileKind {
      ENERGY_BOLT,
      VEIL_ARROW,
      SIGIL_CHAKRAM
   }
}
