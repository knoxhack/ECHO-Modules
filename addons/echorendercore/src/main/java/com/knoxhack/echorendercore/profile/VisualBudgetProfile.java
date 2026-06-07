package com.knoxhack.echorendercore.profile;

public record VisualBudgetProfile(
   VisualBudgetTier tier,
   Integer maxLayers,
   Integer maxEmitters,
   Integer maxEffectCost,
   Integer maxBloomCost,
   Integer maxMaskSubmissions,
   VisualAdvancedFxPolicy advancedFx
) {
   public static final VisualBudgetProfile DEFAULT =
      new VisualBudgetProfile(VisualBudgetTier.NORMAL, null, null, null, null, null, VisualAdvancedFxPolicy.AUTO);

   public VisualBudgetProfile {
      tier = tier == null ? VisualBudgetTier.NORMAL : tier;
      maxLayers = positiveOrNull(maxLayers);
      maxEmitters = positiveOrNull(maxEmitters);
      maxEffectCost = positiveOrNull(maxEffectCost);
      maxBloomCost = positiveOrNull(maxBloomCost);
      maxMaskSubmissions = positiveOrNull(maxMaskSubmissions);
      advancedFx = advancedFx == null ? VisualAdvancedFxPolicy.AUTO : advancedFx;
   }

   public int effectiveMaxLayers() {
      return maxLayers == null ? tier.maxLayers() : maxLayers;
   }

   public int effectiveMaxEmitters() {
      return maxEmitters == null ? tier.maxEmitters() : maxEmitters;
   }

   public int effectiveMaxEffectCost() {
      return maxEffectCost == null ? tier.maxEffectCost() : maxEffectCost;
   }

   public int effectiveMaxBloomCost() {
      return maxBloomCost == null ? tier.maxBloomCost() : maxBloomCost;
   }

   public int effectiveMaxMaskSubmissions() {
      return maxMaskSubmissions == null ? tier.maxMaskSubmissions() : maxMaskSubmissions;
   }

   public boolean advancedFxDenied() {
      return advancedFx == VisualAdvancedFxPolicy.DENY;
   }

   private static Integer positiveOrNull(Integer value) {
      return value == null || value <= 0 ? null : value;
   }
}
