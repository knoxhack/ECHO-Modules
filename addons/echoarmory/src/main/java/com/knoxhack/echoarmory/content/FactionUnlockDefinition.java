package com.knoxhack.echoarmory.content;

import net.minecraft.resources.Identifier;

public record FactionUnlockDefinition(Identifier id, Identifier factionId, int minReputation, String unlockId, String title) {
   public FactionUnlockDefinition {
      if (id == null || factionId == null) {
         throw new IllegalArgumentException("Faction unlock id and faction id are required.");
      }
      minReputation = Math.max(0, minReputation);
      unlockId = unlockId == null ? "" : unlockId.strip();
      title = title == null || title.isBlank() ? unlockId : title.strip();
   }
}
