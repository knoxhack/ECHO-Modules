package com.knoxhack.echoarmory.content;

import java.util.List;
import net.minecraft.resources.Identifier;

public record BossRecommendationDefinition(
   Identifier id,
   String bossName,
   int minTier,
   int fractureProtection,
   List<String> recommendedTags,
   String hint
) {
   public BossRecommendationDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Boss recommendation id is required.");
      }
      bossName = bossName == null || bossName.isBlank() ? id.getPath().replace('_', ' ') : bossName.strip();
      minTier = Math.max(1, Math.min(4, minTier));
      fractureProtection = Math.max(0, fractureProtection);
      recommendedTags = List.copyOf(recommendedTags == null ? List.of() : recommendedTags);
      hint = hint == null ? "" : hint.strip();
   }
}
