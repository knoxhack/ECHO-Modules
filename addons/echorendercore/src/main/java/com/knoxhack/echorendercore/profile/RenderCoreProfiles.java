package com.knoxhack.echorendercore.profile;

import java.util.Map;
import net.minecraft.resources.Identifier;

public final class RenderCoreProfiles {
   private static volatile LoadedContent loaded = LoadedContent.EMPTY;
   private static volatile ProfileHotSwapResult lastHotSwap =
      new ProfileHotSwapResult(true, LoadedContent.EMPTY, LoadedContent.EMPTY, "Initial empty profile cache.");

   private RenderCoreProfiles() {
   }

   public static LoadedContent loaded() {
      return loaded;
   }

   public static void replace(LoadedContent content) {
      loaded = content == null ? LoadedContent.EMPTY : content;
   }

   public static ProfileHotSwapResult hotSwap(LoadedContent content) {
      LoadedContent previous = loaded;
      LoadedContent candidate = content == null ? LoadedContent.EMPTY : content;
      if (shouldReject(candidate, previous)) {
         ProfileHotSwapResult result = new ProfileHotSwapResult(
            false,
            previous,
            previous,
            "Rejected RenderCore profile hot-swap: " + candidate.failed() + " JSON file(s) failed and a previous cache is available."
         );
         lastHotSwap = result;
         return result;
      }
      loaded = candidate;
      ProfileHotSwapResult result = new ProfileHotSwapResult(true, previous, candidate,
         "Accepted RenderCore profile hot-swap.");
      lastHotSwap = result;
      return result;
   }

   public static ProfileHotSwapResult lastHotSwap() {
      return lastHotSwap;
   }

   private static boolean shouldReject(LoadedContent candidate, LoadedContent previous) {
      boolean previousUsable = previous != LoadedContent.EMPTY
         && (!previous.visualProfiles().isEmpty() || !previous.animationProfiles().isEmpty() || !previous.particleProfiles().isEmpty());
      return previousUsable && (candidate.failed() > 0 || candidate.validationReport().hasErrors());
   }

   public static VisualProfile visual(Identifier id) {
      return id == null ? null : loaded.visualProfiles().get(id);
   }

   public static AnimationProfile animation(Identifier id) {
      return id == null ? null : loaded.animationProfiles().get(id);
   }

   public static ParticleProfile particle(Identifier id) {
      return id == null ? null : loaded.particleProfiles().get(id);
   }

   public record LoadedContent(
      Map<Identifier, VisualProfile> visualProfiles,
      Map<Identifier, AnimationProfile> animationProfiles,
      Map<Identifier, ParticleProfile> particleProfiles,
      ProfileValidationReport validationReport,
      ProfilePerformanceReport performanceReport,
      ProfileCacheMetrics cacheMetrics,
      ProfileDiagnosticsReport diagnosticsReport,
      int discovered,
      int loaded,
      int failed
   ) {
      public static final LoadedContent EMPTY = new LoadedContent(
         Map.of(),
         Map.of(),
         Map.of(),
         ProfileValidationReport.EMPTY,
         ProfilePerformanceReport.EMPTY,
         ProfileCacheMetrics.EMPTY,
         ProfileDiagnosticsReport.EMPTY,
         0,
         0,
         0
      );

      public LoadedContent {
         visualProfiles = visualProfiles == null ? Map.of() : Map.copyOf(visualProfiles);
         animationProfiles = animationProfiles == null ? Map.of() : Map.copyOf(animationProfiles);
         particleProfiles = particleProfiles == null ? Map.of() : Map.copyOf(particleProfiles);
         validationReport = validationReport == null ? ProfileValidationReport.EMPTY : validationReport;
         performanceReport = performanceReport == null ? ProfilePerformanceReport.EMPTY : performanceReport;
         cacheMetrics = cacheMetrics == null ? ProfileCacheMetrics.EMPTY : cacheMetrics;
         diagnosticsReport = diagnosticsReport == null ? new ProfileDiagnosticsReport(validationReport, performanceReport, cacheMetrics) : diagnosticsReport;
         discovered = Math.max(0, discovered);
         loaded = Math.max(0, loaded);
         failed = Math.max(0, failed);
      }
   }
}
