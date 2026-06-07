package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;

public record CreatorPackManifest(
   String generator,
   String namespace,
   int schemaVersion,
   int profileCount,
   int migrationRequiredCount,
   int screenshotAvailableCount,
   ProfileCacheMetrics cacheMetrics
) {
   public static final int CREATOR_PACK_VERSION = 21;

   public CreatorPackManifest {
      generator = generator == null || generator.isBlank() ? "echorendercore" : generator;
      namespace = namespace == null || namespace.isBlank() ? "all" : namespace;
      schemaVersion = Math.max(1, schemaVersion);
      profileCount = Math.max(0, profileCount);
      migrationRequiredCount = Math.max(0, migrationRequiredCount);
      screenshotAvailableCount = Math.max(0, screenshotAvailableCount);
      cacheMetrics = cacheMetrics == null ? ProfileCacheMetrics.EMPTY : cacheMetrics;
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("generator", generator);
      root.addProperty("namespace", namespace);
      root.addProperty("schema_version", schemaVersion);
      root.addProperty("target_schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
      root.addProperty("profile_count", profileCount);
      root.addProperty("migration_required_count", migrationRequiredCount);
      root.addProperty("screenshot_available_count", screenshotAvailableCount);
      root.addProperty("cache", cacheMetrics.summaryLine());
      return root;
   }
}
