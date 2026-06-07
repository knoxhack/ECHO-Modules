package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;

public record CreatorProfileAudit(
   Identifier profileId,
   List<ProfileValidationIssue> validationIssues,
   List<ProfilePerformanceIssue> performanceIssues,
   ProfilePerformanceSummary performanceSummary
) {
   public CreatorProfileAudit {
      validationIssues = validationIssues == null ? List.of() : List.copyOf(validationIssues);
      performanceIssues = performanceIssues == null ? List.of() : List.copyOf(performanceIssues);
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("profile", profileId == null ? "" : profileId.toString());
      JsonArray validation = new JsonArray();
      for (ProfileValidationIssue issue : validationIssues) {
         JsonObject value = new JsonObject();
         value.addProperty("severity", issue.severity().name().toLowerCase(java.util.Locale.ROOT));
         value.addProperty("code", issue.code());
         value.addProperty("path", issue.path());
         value.addProperty("message", issue.message());
         value.addProperty("suggestion", issue.suggestion());
         validation.add(value);
      }
      root.add("validation", validation);
      JsonArray performance = new JsonArray();
      for (ProfilePerformanceIssue issue : performanceIssues) {
         JsonObject value = new JsonObject();
         value.addProperty("severity", issue.severity().name().toLowerCase(java.util.Locale.ROOT));
         value.addProperty("code", issue.code());
         value.addProperty("message", issue.message());
         value.addProperty("value", issue.value());
         value.addProperty("threshold", issue.threshold());
         performance.add(value);
      }
      root.add("performance", performance);
      root.add("summary", performanceSummaryJson(performanceSummary));
      return root;
   }

   private static JsonObject performanceSummaryJson(ProfilePerformanceSummary summary) {
      JsonObject root = new JsonObject();
      if (summary == null) {
         return root;
      }
      root.addProperty("layers", summary.layerCount());
      root.addProperty("masked_layers", summary.maskedLayerCount());
      root.addProperty("animation_clips", summary.animationClipCount());
      root.addProperty("animation_tracks", summary.animationTrackCount());
      root.addProperty("emitters", summary.emitterCount());
      root.addProperty("estimated_emitter_rate", summary.estimatedEmitterRate());
      root.addProperty("active_effects", summary.activeEffectCount());
      root.addProperty("effect_cost", summary.estimatedEffectCost());
      root.addProperty("bloom_cost", summary.estimatedBloomCost());
      root.addProperty("advanced_effect_passes", summary.advancedEffectPassCount());
      root.addProperty("effect_target_scope", summary.primaryEffectTargetScope());
      root.addProperty("estimated_mask_submissions", summary.estimatedMaskSubmissions());
      root.addProperty("estimated_bloom_channels", summary.estimatedBloomChannelCount());
      root.addProperty("estimated_bloom_downscale", summary.estimatedBloomDownscale());
      root.addProperty("estimated_priority_skips", summary.estimatedPrioritySkips());
      root.addProperty("advanced_fx_mode", summary.advancedFxMode());
      root.addProperty("isolated_fx_available", summary.isolatedFxAvailable());
      root.addProperty("fullscreen_fallback_available", summary.fullscreenFallbackAvailable());
      return root;
   }
}
