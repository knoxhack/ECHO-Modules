package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public record ProfilePreviewIndex(
   String generator,
   List<ProfilePreviewEntry> entries,
   ProfileCacheMetrics cacheMetrics
) {
   public ProfilePreviewIndex {
      generator = generator == null || generator.isBlank() ? "echorendercore" : generator;
      entries = entries == null ? List.of() : List.copyOf(entries);
      cacheMetrics = cacheMetrics == null ? ProfileCacheMetrics.EMPTY : cacheMetrics;
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("generator", generator);
      root.addProperty("profile_count", entries.size());
      root.addProperty("cache", cacheMetrics.summaryLine());
      JsonArray profiles = new JsonArray();
      entries.stream()
         .sorted(java.util.Comparator.comparing(entry -> entry.profileId().toString()))
         .forEach(entry -> {
            JsonObject value = new JsonObject();
            value.addProperty("profile", entry.profileId().toString());
            value.addProperty("schema_version", entry.schemaVersion());
            value.addProperty("layers", entry.layerCount());
            value.addProperty("masked_layers", entry.maskedLayerCount());
            value.addProperty("animation_clips", entry.animationClipCount());
            value.addProperty("animation_tracks", entry.animationTrackCount());
            value.addProperty("emitters", entry.emitterCount());
            value.addProperty("anchors", entry.anchorCount());
            value.addProperty("block_part_aliases", entry.blockPartAliasCount());
            value.addProperty("validation_warnings", entry.validationWarningCount());
            value.addProperty("validation_errors", entry.validationErrorCount());
            value.addProperty("performance_warnings", entry.performanceWarningCount());
            value.addProperty("screenshot_available", entry.screenshotAvailable());
            value.addProperty("screenshot_provider", entry.screenshotProvider());
            value.addProperty("screenshot_skipped_reason", entry.screenshotSkippedReason());
            value.addProperty("artifact", entry.suggestedArtifactPath());
            profiles.add(value);
         });
      root.add("profiles", profiles);
      return root;
   }
}
