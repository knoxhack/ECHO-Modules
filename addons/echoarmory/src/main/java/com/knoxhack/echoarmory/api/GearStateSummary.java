package com.knoxhack.echoarmory.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record GearStateSummary(
   Identifier itemId,
   String title,
   String kind,
   int tier,
   int moduleCount,
   int energy,
   int energyCapacity,
   int instability,
   List<String> modules
) {
   public GearStateSummary {
      title = title == null || title.isBlank() ? "unknown" : title.strip();
      kind = kind == null || kind.isBlank() ? "unknown" : kind.strip();
      tier = Math.max(0, tier);
      moduleCount = Math.max(0, moduleCount);
      energy = Math.max(0, energy);
      energyCapacity = Math.max(0, energyCapacity);
      instability = Math.max(0, instability);
      modules = List.copyOf(modules == null ? List.of() : modules);
   }
}
