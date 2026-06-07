package com.knoxhack.echorendercore.profile;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.Identifier;

public record ProfileCacheMetrics(
   int visualProfileCount,
   int animationProfileCount,
   int particleProfileCount,
   int discoveredJsonCount,
   int loadedJsonCount,
   int failedJsonCount,
   long validationWarningCount,
   long validationErrorCount,
   int performanceWarningCount,
   Set<String> namespaces,
   int minSchemaVersion,
   int maxSchemaVersion
) {
   public static final ProfileCacheMetrics EMPTY = new ProfileCacheMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, Set.of(), 0, 0);

   public ProfileCacheMetrics {
      visualProfileCount = Math.max(0, visualProfileCount);
      animationProfileCount = Math.max(0, animationProfileCount);
      particleProfileCount = Math.max(0, particleProfileCount);
      discoveredJsonCount = Math.max(0, discoveredJsonCount);
      loadedJsonCount = Math.max(0, loadedJsonCount);
      failedJsonCount = Math.max(0, failedJsonCount);
      validationWarningCount = Math.max(0, validationWarningCount);
      validationErrorCount = Math.max(0, validationErrorCount);
      performanceWarningCount = Math.max(0, performanceWarningCount);
      namespaces = namespaces == null ? Set.of() : Set.copyOf(namespaces);
      minSchemaVersion = Math.max(0, minSchemaVersion);
      maxSchemaVersion = Math.max(0, maxSchemaVersion);
   }

   public static ProfileCacheMetrics from(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         ProfileValidationReport validationReport,
         ProfilePerformanceReport performanceReport,
         int discoveredJsonCount,
         int loadedJsonCount,
         int failedJsonCount) {
      TreeSet<String> namespaces = new TreeSet<>();
      if (visuals != null) {
         visuals.keySet().forEach(id -> namespaces.add(id.getNamespace()));
      }
      if (animations != null) {
         animations.keySet().forEach(id -> namespaces.add(id.getNamespace()));
      }
      if (particles != null) {
         particles.keySet().forEach(id -> namespaces.add(id.getNamespace()));
      }
      ProfileValidationReport validation = validationReport == null ? ProfileValidationReport.EMPTY : validationReport;
      validation.issues().stream()
         .map(ProfileValidationIssue::profileId)
         .filter(id -> id != null)
         .forEach(id -> namespaces.add(id.getNamespace()));
      int minSchema = 0;
      int maxSchema = 0;
      if (visuals != null && !visuals.isEmpty()) {
         minSchema = visuals.values().stream().mapToInt(VisualProfile::schemaVersion).min().orElse(0);
         maxSchema = visuals.values().stream().mapToInt(VisualProfile::schemaVersion).max().orElse(0);
      }
      ProfilePerformanceReport performance = performanceReport == null ? ProfilePerformanceReport.EMPTY : performanceReport;
      return new ProfileCacheMetrics(
         visuals == null ? 0 : visuals.size(),
         animations == null ? 0 : animations.size(),
         particles == null ? 0 : particles.size(),
         discoveredJsonCount,
         loadedJsonCount,
         failedJsonCount,
         validation.warnings(),
         validation.errors(),
         performance.warningCount(),
         namespaces,
         minSchema,
         maxSchema
      );
   }

   public String summaryLine() {
      return "profiles " + visualProfileCount + "/" + animationProfileCount + "/" + particleProfileCount
         + ", json " + loadedJsonCount + "/" + discoveredJsonCount + " loaded, failed " + failedJsonCount
         + ", validation W:" + validationWarningCount + " E:" + validationErrorCount
         + ", perf W:" + performanceWarningCount
         + ", schema " + minSchemaVersion + "-" + maxSchemaVersion;
   }
}
