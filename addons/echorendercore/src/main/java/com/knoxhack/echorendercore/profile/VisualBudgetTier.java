package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualBudgetTier {
   LOW("low", 6, 2, 10, 12, 16, true),
   NORMAL("normal", 12, 8, 18, 24, 96, true),
   HIGH("high", 20, 16, 32, 48, 160, true),
   SHOWCASE("showcase", 32, 32, 48, 72, 256, true),
   UNSUPPORTED("unsupported", 12, 8, 18, 24, 96, false);

   private final String id;
   private final int maxLayers;
   private final int maxEmitters;
   private final int maxEffectCost;
   private final int maxBloomCost;
   private final int maxMaskSubmissions;
   private final boolean supported;

   VisualBudgetTier(String id, int maxLayers, int maxEmitters, int maxEffectCost, int maxBloomCost,
         int maxMaskSubmissions, boolean supported) {
      this.id = id;
      this.maxLayers = maxLayers;
      this.maxEmitters = maxEmitters;
      this.maxEffectCost = maxEffectCost;
      this.maxBloomCost = maxBloomCost;
      this.maxMaskSubmissions = maxMaskSubmissions;
      this.supported = supported;
   }

   public String id() {
      return id;
   }

   public int maxLayers() {
      return maxLayers;
   }

   public int maxEmitters() {
      return maxEmitters;
   }

   public int maxEffectCost() {
      return maxEffectCost;
   }

   public int maxBloomCost() {
      return maxBloomCost;
   }

   public int maxMaskSubmissions() {
      return maxMaskSubmissions;
   }

   public boolean supported() {
      return supported;
   }

   public static VisualBudgetTier byName(String name) {
      if (name == null || name.isBlank()) {
         return NORMAL;
      }
      String normalized = name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
      for (VisualBudgetTier tier : values()) {
         if (tier.id.equals(normalized) || tier.name().equalsIgnoreCase(normalized)) {
            return tier;
         }
      }
      return UNSUPPORTED;
   }
}
