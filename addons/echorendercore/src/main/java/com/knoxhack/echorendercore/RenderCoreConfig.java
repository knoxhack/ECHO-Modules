package com.knoxhack.echorendercore;

import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;

public final class RenderCoreConfig {
   public enum AdvancedFxMode {
      ISOLATED,
      FULLSCREEN,
      STABLE
   }

   public static final EchoNativeConfigSpec CLIENT_SPEC;
   public static final EchoNativeConfigSpec.BooleanValue ADVANCED_FX_ENABLED;
   public static final EchoNativeConfigSpec.EnumValue<AdvancedFxMode> ADVANCED_FX_MODE;
   public static final EchoNativeConfigSpec.IntValue BLOOM_DOWNSCALE;
   public static final EchoNativeConfigSpec.IntValue MAX_BLOOM_SUBMISSIONS;
   public static final EchoNativeConfigSpec.IntValue MAX_BLOOM_PASSES;
   public static final EchoNativeConfigSpec.IntValue MAX_BLOOM_CHANNELS;
   public static final EchoNativeConfigSpec.BooleanValue FALLBACK_TO_STABLE;
   public static final EchoNativeConfigSpec.BooleanValue LOG_ADVANCED_FX_FAILURES;

   static {
      EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
      builder.push("advanced_fx");
      ADVANCED_FX_ENABLED = builder
         .comment("Enables optional client-only RenderCore advanced FX. Disabled by default; /rendercore debug advancedfx can override for the session.")
         .define("advanced_fx_enabled", false);
      ADVANCED_FX_MODE = builder
         .comment("Advanced FX mode. isolated uses the V12 budget-aware bloom mask path; fullscreen keeps the fullscreen fallback chain; stable disables postprocessing.")
         .defineEnum("advanced_fx_mode", AdvancedFxMode.ISOLATED);
      BLOOM_DOWNSCALE = builder
         .comment("Default isolated bloom mask downscale. Valid values are 1, 2, or 4; other config values fall back to 2 at runtime.")
         .defineInRange("bloom_downscale", 2, 1, 4);
      MAX_BLOOM_SUBMISSIONS = builder
         .comment("Maximum isolated bloom mask submissions accepted per frame before lower priority visuals fall back to stable rendering.")
         .defineInRange("max_bloom_submissions", 96, 0, 4096);
      MAX_BLOOM_PASSES = builder
         .comment("Maximum total bloom pass cost accepted per frame.")
         .defineInRange("max_bloom_passes", 4, 0, 64);
      MAX_BLOOM_CHANNELS = builder
         .comment("Maximum bloom channel ids accepted per frame.")
         .defineInRange("max_bloom_channels", 4, 0, 64);
      FALLBACK_TO_STABLE = builder
         .comment("Keeps stable emissive rendering when advanced FX targets, shaders, or framegraph hooks are unavailable.")
         .define("fallback_to_stable", true);
      LOG_ADVANCED_FX_FAILURES = builder
         .comment("Logs V12 advanced FX fallback causes on the client.")
         .define("log_advanced_fx_failures", true);
      builder.pop();
      CLIENT_SPEC = builder.build();
   }

   private RenderCoreConfig() {
   }

   public static boolean advancedFxEnabled() {
      return safeBoolean(ADVANCED_FX_ENABLED, false);
   }

   public static AdvancedFxMode advancedFxMode() {
      try {
         AdvancedFxMode mode = ADVANCED_FX_MODE.get();
         return mode == null ? AdvancedFxMode.ISOLATED : mode;
      } catch (RuntimeException exception) {
         return AdvancedFxMode.ISOLATED;
      }
   }

   public static int bloomDownscale() {
      int value = safeInt(BLOOM_DOWNSCALE, 2);
      return value == 1 || value == 2 || value == 4 ? value : 2;
   }

   public static int maxBloomSubmissions() {
      return Math.max(0, safeInt(MAX_BLOOM_SUBMISSIONS, 96));
   }

   public static int maxBloomPasses() {
      return Math.max(0, safeInt(MAX_BLOOM_PASSES, 4));
   }

   public static int maxBloomChannels() {
      return Math.max(0, safeInt(MAX_BLOOM_CHANNELS, 4));
   }

   public static boolean fallbackToStable() {
      return safeBoolean(FALLBACK_TO_STABLE, true);
   }

   public static boolean logAdvancedFxFailures() {
      return safeBoolean(LOG_ADVANCED_FX_FAILURES, true);
   }

   private static boolean safeBoolean(EchoNativeConfigSpec.BooleanValue value, boolean fallback) {
      try {
         return value.get();
      } catch (RuntimeException exception) {
         return fallback;
      }
   }

   private static int safeInt(EchoNativeConfigSpec.IntValue value, int fallback) {
      try {
         return value.get();
      } catch (RuntimeException exception) {
         return fallback;
      }
   }
}
