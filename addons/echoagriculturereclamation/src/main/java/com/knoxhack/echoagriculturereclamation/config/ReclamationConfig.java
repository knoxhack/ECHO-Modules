package com.knoxhack.echoagriculturereclamation.config;

import com.knoxhack.echoagriculturereclamation.content.ReclamationMachineRules;
import com.knoxhack.echoagriculturereclamation.content.ReclamationProgressionRules;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class ReclamationConfig {
   private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();
   public static final EchoNativeConfigSpec SPEC;

   public static final EchoNativeConfigSpec.IntValue GLOBAL_GROWTH_CHANCE_BONUS;
   public static final EchoNativeConfigSpec.IntValue STABLE_SEED_GROWTH_BONUS;
   public static final EchoNativeConfigSpec.IntValue HYDROPONIC_GROWTH_TICKS;
   public static final EchoNativeConfigSpec.IntValue HYDROPONIC_NUTRIENT_CAP;
   public static final EchoNativeConfigSpec.IntValue HYDROPONIC_NUTRIENT_PER_MIX;
   public static final EchoNativeConfigSpec.IntValue GREENHOUSE_SAFE_THRESHOLD;
   public static final EchoNativeConfigSpec.IntValue GREENHOUSE_GLASS_WEIGHT;
   public static final EchoNativeConfigSpec.IntValue GREENHOUSE_FILTER_WEIGHT;
   public static final EchoNativeConfigSpec.IntValue GREENHOUSE_DOCK_WEIGHT;
   public static final EchoNativeConfigSpec.IntValue GREENHOUSE_CONTROLLER_WEIGHT;
   public static final EchoNativeConfigSpec.IntValue GREENHOUSE_TRAY_WEIGHT;
   public static final EchoNativeConfigSpec.IntValue POLLINATOR_SERVICE_RADIUS;
   public static final EchoNativeConfigSpec.IntValue POLLINATOR_SERVICE_TICKS;
   public static final EchoNativeConfigSpec.IntValue POLLINATOR_GROWTH_BONUS;
   public static final EchoNativeConfigSpec.IntValue PURIFY_THRESHOLD;
   public static final EchoNativeConfigSpec.IntValue STABILIZE_THRESHOLD;
   public static final EchoNativeConfigSpec.IntValue RESTORE_THRESHOLD;
   public static final EchoNativeConfigSpec.BooleanValue ENABLE_WEATHER_CROP_PENALTIES;
   public static final EchoNativeConfigSpec.DoubleValue WEATHER_PENALTY_MULTIPLIER;
   public static final EchoNativeConfigSpec.BooleanValue ENABLE_POWER_ACCELERATION;
   public static final EchoNativeConfigSpec.IntValue POWERED_THROUGHPUT_DIVISOR;

   static {
      BUILDER.push("growth");
      GLOBAL_GROWTH_CHANCE_BONUS = BUILDER
         .comment("Flat percentage-point bonus added to all Reclamation crop growth checks.")
         .defineInRange("global_growth_chance_bonus", 0, -50, 50);
      STABLE_SEED_GROWTH_BONUS = BUILDER
         .comment("Extra percentage-point bonus for stable seed profiles, added on top of crop data.")
         .defineInRange("stable_seed_growth_bonus", 0, 0, 50);
      BUILDER.pop();

      BUILDER.push("hydroponics");
      HYDROPONIC_GROWTH_TICKS = BUILDER.defineInRange("hydroponic_growth_ticks", 180, 20, 72000);
      HYDROPONIC_NUTRIENT_CAP = BUILDER.defineInRange("hydroponic_nutrient_cap", 8, 1, 128);
      HYDROPONIC_NUTRIENT_PER_MIX = BUILDER.defineInRange("hydroponic_nutrient_per_mix", 3, 1, 64);
      BUILDER.pop();

      BUILDER.push("greenhouse");
      GREENHOUSE_SAFE_THRESHOLD = BUILDER.defineInRange("greenhouse_safe_threshold", 70, 0, 100);
      GREENHOUSE_GLASS_WEIGHT = BUILDER.defineInRange("greenhouse_glass_weight", 2, 0, 50);
      GREENHOUSE_FILTER_WEIGHT = BUILDER.defineInRange("greenhouse_filter_weight", 18, 0, 100);
      GREENHOUSE_DOCK_WEIGHT = BUILDER.defineInRange("greenhouse_dock_weight", 14, 0, 100);
      GREENHOUSE_CONTROLLER_WEIGHT = BUILDER.defineInRange("greenhouse_controller_weight", 10, 0, 100);
      GREENHOUSE_TRAY_WEIGHT = BUILDER.defineInRange("greenhouse_tray_weight", 4, 0, 50);
      BUILDER.pop();

      BUILDER.push("pollinator");
      POLLINATOR_SERVICE_RADIUS = BUILDER.defineInRange("pollinator_service_radius", 4, 1, 32);
      POLLINATOR_SERVICE_TICKS = BUILDER.defineInRange("pollinator_service_ticks", 120, 20, 72000);
      POLLINATOR_GROWTH_BONUS = BUILDER.defineInRange("pollinator_growth_bonus", 12, 0, 100);
      BUILDER.pop();

      BUILDER.push("restoration");
      PURIFY_THRESHOLD = BUILDER.defineInRange("purify_threshold", 25, 0, 100);
      STABILIZE_THRESHOLD = BUILDER.defineInRange("stabilize_threshold", 60, 0, 100);
      RESTORE_THRESHOLD = BUILDER.defineInRange("restore_threshold", 100, 0, 100);
      BUILDER.pop();

      BUILDER.push("integrations");
      ENABLE_WEATHER_CROP_PENALTIES = BUILDER.define("enable_weather_crop_penalties", true);
      WEATHER_PENALTY_MULTIPLIER = BUILDER.defineInRange("weather_penalty_multiplier", 1.0D, 0.0D, 4.0D);
      ENABLE_POWER_ACCELERATION = BUILDER.define("enable_power_acceleration", true);
      POWERED_THROUGHPUT_DIVISOR = BUILDER.defineInRange("powered_throughput_divisor", 2, 1, 16);
      BUILDER.pop();

      SPEC = BUILDER.build();
   }

   private ReclamationConfig() {
   }

   public static ReclamationMachineRules apply(ReclamationMachineRules rules) {
      ReclamationMachineRules base = rules == null ? ReclamationMachineRules.defaults() : rules;
      return new ReclamationMachineRules(
         base.soilPurifierRadius(),
         base.soilPurifierEnzymeBlocks(),
         base.soilPurifierNutrientBlocks(),
         safeInt(HYDROPONIC_GROWTH_TICKS, base.hydroponicGrowthTicks()),
         safeInt(HYDROPONIC_NUTRIENT_CAP, base.hydroponicNutrientCap()),
         safeInt(HYDROPONIC_NUTRIENT_PER_MIX, base.hydroponicNutrientPerMix()),
         base.bioReactorOrganicOutput(),
         base.bioReactorGeneSampleOutput(),
         base.compostRecyclerOutput(),
         base.greenhouseHorizontalRange(),
         base.greenhouseDownRange(),
         base.greenhouseUpRange(),
         safeInt(GREENHOUSE_GLASS_WEIGHT, base.greenhouseGlassWeight()),
         safeInt(GREENHOUSE_FILTER_WEIGHT, base.greenhouseFilterWeight()),
         safeInt(GREENHOUSE_DOCK_WEIGHT, base.greenhouseDockWeight()),
         safeInt(GREENHOUSE_CONTROLLER_WEIGHT, base.greenhouseControllerWeight()),
         safeInt(GREENHOUSE_TRAY_WEIGHT, base.greenhouseTrayWeight()),
         safeInt(POLLINATOR_SERVICE_RADIUS, base.pollinatorDroneServiceRadius()),
         base.pollinatorDroneHomeRadius(),
         safeInt(POLLINATOR_SERVICE_TICKS, base.pollinatorDroneServiceTicks()),
         safeInt(POLLINATOR_GROWTH_BONUS, base.pollinatorDroneGrowthBonus())
      ).normalized();
   }

   public static ReclamationProgressionRules apply(ReclamationProgressionRules rules) {
      ReclamationProgressionRules base = rules == null ? ReclamationProgressionRules.defaults() : rules;
      return new ReclamationProgressionRules(
         safeInt(GREENHOUSE_SAFE_THRESHOLD, base.greenhouseSafeThreshold()),
         base.foodKnownSeedBonus(),
         base.foodItemValue(),
         base.recoveredSeedMinStability(),
         base.recoveredSeedStabilityRange(),
         base.recoveredSeedMinContamination(),
         base.recoveredSeedContaminationRange(),
         base.scannerUnsafeRestorationGain(),
         base.scannerSafeRestorationGain(),
         safeInt(PURIFY_THRESHOLD, base.purifyThreshold()),
         safeInt(STABILIZE_THRESHOLD, base.stabilizeThreshold()),
         safeInt(RESTORE_THRESHOLD, base.restoreThreshold()),
         base.cropPurifyMaxLow(),
         base.cropPurifyMaxHigh(),
         base.cropStabilizeMax(),
         base.cropRestoreMax(),
         base.scannerPurifyMaxLow(),
         base.scannerPurifyMaxHigh(),
         base.scannerStabilizeMax(),
         base.scannerRestoreMax(),
         base.restorationCropWeightForStabilization()
      ).normalized();
   }

   public static int globalGrowthChanceBonus() {
      return safeInt(GLOBAL_GROWTH_CHANCE_BONUS, 0);
   }

   public static int stableSeedGrowthBonus() {
      return safeInt(STABLE_SEED_GROWTH_BONUS, 0);
   }

   public static boolean weatherCropPenaltiesEnabled() {
      return safeBoolean(ENABLE_WEATHER_CROP_PENALTIES, true);
   }

   public static int scaleWeatherPenalty(int penalty) {
      return Math.max(0, (int)Math.round(penalty * safeDouble(WEATHER_PENALTY_MULTIPLIER, 1.0D)));
   }

   public static boolean powerAccelerationEnabled() {
      return safeBoolean(ENABLE_POWER_ACCELERATION, true);
   }

   public static int poweredThroughputDivisor() {
      return Math.max(1, safeInt(POWERED_THROUGHPUT_DIVISOR, 2));
   }

   private static boolean safeBoolean(EchoNativeConfigSpec.BooleanValue value, boolean fallback) {
      try {
         return value.get();
      } catch (IllegalStateException exception) {
         return fallback;
      }
   }

   private static int safeInt(EchoNativeConfigSpec.IntValue value, int fallback) {
      try {
         return value.get();
      } catch (IllegalStateException exception) {
         return fallback;
      }
   }

   private static double safeDouble(EchoNativeConfigSpec.DoubleValue value, double fallback) {
      try {
         return value.get();
      } catch (IllegalStateException exception) {
         return fallback;
      }
   }
}
