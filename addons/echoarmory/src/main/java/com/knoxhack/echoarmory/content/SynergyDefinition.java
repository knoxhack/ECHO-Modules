package com.knoxhack.echoarmory.content;

import java.util.List;
import net.minecraft.resources.Identifier;

public record SynergyDefinition(
   Identifier id,
   String title,
   List<String> requiredTags,
   String effect,
   int potency,
   String terminalHint
) {
   public SynergyDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Synergy id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath().replace('_', ' ') : title.strip();
      requiredTags = List.copyOf(requiredTags == null ? List.of() : requiredTags);
      effect = effect == null || effect.isBlank() ? "status" : effect.strip();
      potency = Math.max(0, potency);
      terminalHint = terminalHint == null ? "" : terminalHint.strip();
   }
}
