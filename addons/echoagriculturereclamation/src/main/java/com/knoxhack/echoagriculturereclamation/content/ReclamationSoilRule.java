package com.knoxhack.echoagriculturereclamation.content;

import java.util.EnumSet;
import java.util.Set;

public record ReclamationSoilRule(
   SoilState state,
   int growthChance,
   int restorationGain,
   boolean safe,
   Set<CropCategory> supportedCategories,
   Set<CropCategory> stabilizedSupportedCategories,
   int stabilizedSupportMinStability
) {
   public static ReclamationSoilRule defaultFor(SoilState state) {
      return switch (state) {
         case DEAD -> new ReclamationSoilRule(state, 8, 0, false, categories(CropCategory.EMERGENCY, CropCategory.RESTORATION), nonNexus(), 80);
         case CONTAMINATED -> new ReclamationSoilRule(
            state,
            14,
            0,
            false,
            categories(CropCategory.EMERGENCY, CropCategory.MUTATED, CropCategory.RESTORATION, CropCategory.INDUSTRIAL),
            nonNexus(),
            80
         );
         case IRRADIATED -> new ReclamationSoilRule(state, 10, 0, false, categories(CropCategory.MUTATED, CropCategory.NEXUS_TOUCHED), nonNexus(), 80);
         case TOXIC_MUD -> new ReclamationSoilRule(state, 12, 0, false, categories(CropCategory.MUTATED, CropCategory.INDUSTRIAL), Set.of(), 80);
         case PURIFIED -> new ReclamationSoilRule(state, 28, 1, true, all(), all(), 0);
         case STABILIZED -> new ReclamationSoilRule(state, 42, 2, true, all(), all(), 0);
         case RESTORED -> new ReclamationSoilRule(state, 55, 4, true, all(), all(), 0);
      };
   }

   public boolean canSupport(CropCategory category, int stability) {
      return supportedCategories.contains(category) || stability >= stabilizedSupportMinStability && stabilizedSupportedCategories.contains(category);
   }

   public ReclamationSoilRule normalized(SoilState state) {
      return new ReclamationSoilRule(
         state,
         clamp(growthChance, 0, 95),
         Math.max(0, restorationGain),
         safe,
         copy(supportedCategories),
         copy(stabilizedSupportedCategories),
         clamp(stabilizedSupportMinStability, 0, 100)
      );
   }

   public static Set<CropCategory> categories(CropCategory first, CropCategory... rest) {
      EnumSet<CropCategory> categories = EnumSet.of(first, rest);
      return Set.copyOf(categories);
   }

   public static Set<CropCategory> all() {
      return Set.copyOf(EnumSet.allOf(CropCategory.class));
   }

   public static Set<CropCategory> nonNexus() {
      EnumSet<CropCategory> categories = EnumSet.allOf(CropCategory.class);
      categories.remove(CropCategory.NEXUS_TOUCHED);
      return Set.copyOf(categories);
   }

   private static Set<CropCategory> copy(Set<CropCategory> categories) {
      return categories.isEmpty() ? Set.of() : Set.copyOf(categories);
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
