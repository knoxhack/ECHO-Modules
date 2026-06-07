package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;

public record CreatorProfileCard(
   Identifier profileId,
   String title,
   int schemaVersion,
   boolean migrationRequired,
   int layerCount,
   int materialCount,
   int emitterCount,
   int activeEffectCount,
   List<String> effectPresets,
   long validationWarningCount,
   long validationErrorCount,
   int performanceWarningCount,
   boolean screenshotAvailable,
   String screenshotProvider,
   String screenshotPath,
   String screenshotSkippedReason,
   String suggestedArtifactPath
) {
   public CreatorProfileCard {
      title = title == null || title.isBlank() ? (profileId == null ? "unknown" : profileId.toString()) : title;
      schemaVersion = Math.max(0, schemaVersion);
      layerCount = Math.max(0, layerCount);
      materialCount = Math.max(0, materialCount);
      emitterCount = Math.max(0, emitterCount);
      activeEffectCount = Math.max(0, activeEffectCount);
      effectPresets = effectPresets == null ? List.of() : List.copyOf(effectPresets);
      validationWarningCount = Math.max(0, validationWarningCount);
      validationErrorCount = Math.max(0, validationErrorCount);
      performanceWarningCount = Math.max(0, performanceWarningCount);
      screenshotProvider = screenshotProvider == null || screenshotProvider.isBlank() ? "none" : screenshotProvider;
      screenshotPath = screenshotPath == null ? "" : screenshotPath.replace('\\', '/').replaceFirst("^/+", "");
      screenshotSkippedReason = screenshotSkippedReason == null ? "" : screenshotSkippedReason;
      suggestedArtifactPath = suggestedArtifactPath == null ? "" : suggestedArtifactPath;
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("profile", profileId == null ? "" : profileId.toString());
      root.addProperty("title", title);
      root.addProperty("schema_version", schemaVersion);
      root.addProperty("target_schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
      root.addProperty("migration_required", migrationRequired);
      root.addProperty("layers", layerCount);
      root.addProperty("materials", materialCount);
      root.addProperty("emitters", emitterCount);
      root.addProperty("active_effects", activeEffectCount);
      JsonArray effects = new JsonArray();
      effectPresets.forEach(effects::add);
      root.add("effect_presets", effects);
      root.addProperty("validation_warnings", validationWarningCount);
      root.addProperty("validation_errors", validationErrorCount);
      root.addProperty("performance_warnings", performanceWarningCount);
      JsonObject screenshot = new JsonObject();
      screenshot.addProperty("available", screenshotAvailable);
      screenshot.addProperty("provider", screenshotProvider);
      screenshot.addProperty("path", screenshotPath);
      screenshot.addProperty("skipped_reason", screenshotSkippedReason);
      root.add("screenshot", screenshot);
      root.addProperty("artifact", suggestedArtifactPath);
      return root;
   }
}
