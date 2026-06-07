package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class RenderCoreProfilePreviewer {
   private RenderCoreProfilePreviewer() {
   }

   public static ProfilePreviewReport preview(RenderCoreProfiles.LoadedContent content) {
      return preview(content, ProfileScreenshotPreviewProvider.NO_OP);
   }

   public static ProfilePreviewReport preview(RenderCoreProfiles.LoadedContent content, ProfileScreenshotPreviewProvider screenshotProvider) {
      RenderCoreProfiles.LoadedContent loaded = content == null ? RenderCoreProfiles.LoadedContent.EMPTY : content;
      return preview(
         loaded.visualProfiles(),
         loaded.animationProfiles(),
         loaded.particleProfiles(),
         loaded.diagnosticsReport(),
         screenshotProvider
      );
   }

   public static ProfilePreviewReport preview(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         ProfileDiagnosticsReport diagnostics) {
      return preview(visuals, animations, particles, diagnostics, ProfileScreenshotPreviewProvider.NO_OP);
   }

   public static ProfilePreviewExport export(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         ProfileDiagnosticsReport diagnostics) {
      ProfilePreviewReport report = preview(visuals, animations, particles, diagnostics, ProfileScreenshotPreviewProvider.NO_OP);
      return new ProfilePreviewExport(report, index(report), snippets(report));
   }

   public static ProfilePreviewIndex index(ProfilePreviewReport report) {
      ProfilePreviewReport safeReport = report == null ? ProfilePreviewReport.EMPTY : report;
      return new ProfilePreviewIndex("echorendercore", safeReport.entries(), safeReport.cacheMetrics());
   }

   public static List<ProfilePreviewSnippet> snippets(ProfilePreviewReport report) {
      ProfilePreviewReport safeReport = report == null ? ProfilePreviewReport.EMPTY : report;
      return safeReport.entries().stream()
         .sorted(Comparator.comparing(entry -> entry.profileId().toString()))
         .map(RenderCoreProfilePreviewer::snippet)
         .toList();
   }

   public static ProfilePreviewReport preview(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         ProfileDiagnosticsReport diagnostics,
         ProfileScreenshotPreviewProvider screenshotProvider) {
      Map<Identifier, VisualProfile> safeVisuals = visuals == null ? Map.of() : visuals;
      Map<Identifier, AnimationProfile> safeAnimations = animations == null ? Map.of() : animations;
      Map<Identifier, ParticleProfile> safeParticles = particles == null ? Map.of() : particles;
      ProfileDiagnosticsReport safeDiagnostics = diagnostics == null
         ? RenderCoreProfileValidator.diagnostics(safeVisuals, safeAnimations, safeParticles, 0, 0, 0)
         : diagnostics;
      ProfileScreenshotPreviewProvider safeScreenshotProvider = screenshotProvider == null
         ? ProfileScreenshotPreviewProvider.NO_OP
         : screenshotProvider;
      ArrayList<ProfilePreviewEntry> entries = new ArrayList<>();
      ArrayList<ProfilePreviewArtifact> artifacts = new ArrayList<>();
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      safeVisuals.values().stream()
         .sorted(Comparator.comparing(profile -> profile.id().toString()))
         .forEach(profile -> buildPreview(profile, safeAnimations, safeParticles, safeDiagnostics, safeScreenshotProvider, entries, artifacts, issues));
      issues.addAll(safeDiagnostics.validationReport().issues().stream()
         .filter(issue -> "profile_preview_generation_failed".equals(issue.code()))
         .toList());
      return new ProfilePreviewReport(entries, artifacts, issues, safeDiagnostics.cacheMetrics());
   }

   private static void buildPreview(
         VisualProfile profile,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         ProfileDiagnosticsReport diagnostics,
         ProfileScreenshotPreviewProvider screenshotProvider,
         List<ProfilePreviewEntry> entries,
         List<ProfilePreviewArtifact> artifacts,
         List<ProfileValidationIssue> issues) {
      try {
         AnimationProfile animationProfile = profile.animationProfile() == null ? null : animations.get(profile.animationProfile());
         ParticleProfile particleProfile = profile.particleProfile() == null ? null : particles.get(profile.particleProfile());
         int clipCount = animationProfile == null ? 0 : animationProfile.animations().size();
         int trackCount = animationProfile == null ? 0 : animationProfile.animations().values().stream()
            .mapToInt(clip -> clip.tracks().size())
            .sum();
         int emitterCount = particleProfile == null ? 0 : particleProfile.emitters().size();
         int maskedLayerCount = (int)profile.layers().stream().filter(layer -> !layer.partFilter().isEmpty()).count();
         long warnings = diagnostics.validationReport().issues().stream()
            .filter(issue -> profile.id().equals(issue.profileId()))
            .filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING)
            .count();
         long errors = diagnostics.validationReport().issues().stream()
            .filter(issue -> profile.id().equals(issue.profileId()))
            .filter(issue -> issue.severity() == ProfileValidationSeverity.ERROR)
            .count();
         int performanceWarnings = (int)diagnostics.performanceReport().issues().stream()
            .filter(issue -> profile.id().equals(issue.profileId()))
            .filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING)
            .count();
         boolean screenshotAvailable = screenshotProvider.available(profile);
         String screenshotSkippedReason = screenshotAvailable ? "" : screenshotProvider.skippedReason(profile);
         if (!screenshotAvailable) {
            issues.add(new ProfileValidationIssue(
               ProfileValidationSeverity.WARNING,
               profile.id(),
               "screenshot_preview_unavailable",
               "preview.screenshot",
               "Screenshot preview is not available from provider '" + screenshotProvider.id() + "'.",
               screenshotSkippedReason
            ));
         }
         String path = suggestedPath(profile.id());
         ProfilePreviewEntry entry = new ProfilePreviewEntry(
            profile.id(),
            profile.schemaVersion(),
            profile.layers().size(),
            maskedLayerCount,
            clipCount,
            trackCount,
            emitterCount,
            profile.anchors().size(),
            profile.blockParts().size(),
            warnings,
            errors,
            performanceWarnings,
            screenshotAvailable,
            screenshotProvider.id(),
            screenshotSkippedReason,
            path
         );
         entries.add(entry);
         artifacts.add(new ProfilePreviewArtifact(profile.id(), path, artifactJson(profile, animationProfile, particleProfile, entry, diagnostics)));
      } catch (RuntimeException exception) {
         issues.add(new ProfileValidationIssue(
            ProfileValidationSeverity.WARNING,
            profile == null ? null : profile.id(),
            "profile_preview_generation_failed",
            "preview",
            "Could not generate a deterministic preview artifact: " + exception.getMessage(),
            "Check the profile JSON and referenced animation or particle profiles."
         ));
      }
   }

   private static JsonObject artifactJson(
         VisualProfile profile,
         AnimationProfile animationProfile,
         ParticleProfile particleProfile,
         ProfilePreviewEntry entry,
         ProfileDiagnosticsReport diagnostics) {
      JsonObject root = new JsonObject();
      root.addProperty("profile", profile.id().toString());
      root.addProperty("schema_version", profile.schemaVersion());
      root.addProperty("base_texture", profile.baseTexture() == null ? "" : profile.baseTexture().toString());
      root.addProperty("animation_profile", profile.animationProfile() == null ? "" : profile.animationProfile().toString());
      root.addProperty("particle_profile", profile.particleProfile() == null ? "" : profile.particleProfile().toString());
      root.addProperty("suggested_artifact", entry.suggestedArtifactPath());
      JsonObject screenshot = new JsonObject();
      screenshot.addProperty("available", entry.screenshotAvailable());
      screenshot.addProperty("provider", entry.screenshotProvider());
      screenshot.addProperty("skipped_reason", entry.screenshotSkippedReason());
      root.add("screenshot", screenshot);
      root.add("summary", summaryJson(entry, diagnostics));
      root.add("effect", effectJson(profile.effect()));
      root.add("layers", layersJson(profile));
      root.add("materials", materialsJson(profile));
      root.add("includes", strings(profile.includes().stream().map(reference -> reference.profileId().toString()).toList()));
      root.add("anchors", strings(profile.anchors().keySet()));
      root.add("block_parts", strings(profile.blockParts().keySet()));
      root.add("animations", animationJson(animationProfile));
      root.add("particles", particleJson(particleProfile));
      JsonObject diagnosticsJson = new JsonObject();
      diagnosticsJson.addProperty("validation_warnings", entry.validationWarningCount());
      diagnosticsJson.addProperty("validation_errors", entry.validationErrorCount());
      diagnosticsJson.addProperty("performance_warnings", entry.performanceWarningCount());
      diagnosticsJson.addProperty("cache", diagnostics.cacheMetrics().summaryLine());
      root.add("diagnostics", diagnosticsJson);
      return root;
   }

   private static ProfilePreviewSnippet snippet(ProfilePreviewEntry entry) {
      JsonObject json = new JsonObject();
      json.addProperty("schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
      json.addProperty("base_texture", entry.profileId().getNamespace() + ":textures/entity/" + entry.profileId().getPath() + ".png");
      JsonObject effect = new JsonObject();
      effect.addProperty("preset", "neon");
      effect.addProperty("glow_intensity", 1.2F);
      effect.addProperty("pulse_speed", 1.5F);
      effect.addProperty("pulse_min_alpha", 0.72F);
      effect.addProperty("pulse_max_alpha", 1.0F);
      effect.addProperty("advanced_enabled", false);
      effect.addProperty("target_scope", "profile");
      effect.addProperty("bloom_mask_mode", "auto");
      effect.addProperty("bloom_channel", "default");
      effect.addProperty("bloom_downscale", 2);
      effect.addProperty("advanced_priority", 0);
      json.add("effect", effect);
      JsonArray includes = new JsonArray();
      JsonObject include = new JsonObject();
      include.addProperty("profile", entry.profileId().getNamespace() + ":shared/base_machine");
      includes.add(include);
      json.add("includes", includes);
      JsonObject preview = new JsonObject();
      preview.addProperty("title", entry.profileId().toString());
      preview.addProperty("artifact", entry.suggestedArtifactPath());
      JsonObject screenshot = new JsonObject();
      screenshot.addProperty("enabled", false);
      preview.add("screenshot", screenshot);
      json.add("preview", preview);
      return new ProfilePreviewSnippet(entry.profileId(), "Visual profile snippet for " + entry.profileId(), "visual_profile", json);
   }

   private static JsonObject summaryJson(ProfilePreviewEntry entry, ProfileDiagnosticsReport diagnostics) {
      JsonObject summary = new JsonObject();
      summary.addProperty("layers", entry.layerCount());
      summary.addProperty("masked_layers", entry.maskedLayerCount());
      summary.addProperty("animation_clips", entry.animationClipCount());
      summary.addProperty("animation_tracks", entry.animationTrackCount());
      summary.addProperty("emitters", entry.emitterCount());
      summary.addProperty("anchors", entry.anchorCount());
      summary.addProperty("block_part_aliases", entry.blockPartAliasCount());
      ProfilePerformanceSummary performance = diagnostics.performanceReport().summaries().get(entry.profileId().toString());
      if (performance != null) {
         summary.addProperty("active_effects", performance.activeEffectCount());
         summary.addProperty("effect_cost", performance.estimatedEffectCost());
         summary.addProperty("bloom_cost", performance.estimatedBloomCost());
         summary.addProperty("advanced_effect_passes", performance.advancedEffectPassCount());
         summary.addProperty("effect_target_scope", performance.primaryEffectTargetScope());
         summary.addProperty("estimated_mask_submissions", performance.estimatedMaskSubmissions());
         summary.addProperty("estimated_bloom_channels", performance.estimatedBloomChannelCount());
         summary.addProperty("estimated_bloom_downscale", performance.estimatedBloomDownscale());
         summary.addProperty("estimated_priority_skips", performance.estimatedPrioritySkips());
         summary.addProperty("advanced_fx_mode", performance.advancedFxMode());
         summary.addProperty("isolated_fx_available", performance.isolatedFxAvailable());
         summary.addProperty("fullscreen_fallback_available", performance.fullscreenFallbackAvailable());
         summary.addProperty("advanced_fx_available", false);
         summary.addProperty("advanced_fx_status", "client_runtime_required");
      }
      return summary;
   }

   private static JsonArray layersJson(VisualProfile profile) {
      JsonArray layers = new JsonArray();
      for (VisualLayerProfile layer : profile.layers()) {
         JsonObject value = new JsonObject();
         value.addProperty("id", layer.id());
         value.addProperty("kind", layer.kind().name().toLowerCase(java.util.Locale.ROOT));
         value.addProperty("texture", layer.texture() == null ? "" : layer.texture().toString());
         value.addProperty("material", layer.material());
         value.addProperty("light_mode", layer.lightMode().name().toLowerCase(java.util.Locale.ROOT));
         value.addProperty("render_pass", layer.renderPass().name().toLowerCase(java.util.Locale.ROOT));
         value.addProperty("sort_order", layer.sortOrder());
         value.addProperty("render_priority", layer.renderPriority());
         value.add("effect", effectJson(layer.effect()));
         value.add("states", strings(layer.states().stream().map(Enum::name).toList()));
         value.add("parts", strings(layer.partFilter()));
         layers.add(value);
      }
      return layers;
   }

   private static JsonObject materialsJson(VisualProfile profile) {
      JsonObject materials = new JsonObject();
      profile.materials().entrySet().stream()
         .sorted(Map.Entry.comparingByKey())
         .forEach(entry -> {
            VisualMaterial material = entry.getValue();
            JsonObject value = new JsonObject();
            value.addProperty("color", "#%08X".formatted(material.color()));
            value.addProperty("alpha", material.alpha());
            value.addProperty("emissive", material.emissive());
            value.addProperty("blend_mode", material.blendMode().name().toLowerCase(java.util.Locale.ROOT));
            value.addProperty("light_mode", material.lightMode().name().toLowerCase(java.util.Locale.ROOT));
            value.addProperty("render_pass", material.renderPass().name().toLowerCase(java.util.Locale.ROOT));
            value.addProperty("sort_order", material.sortOrder());
            value.addProperty("render_priority", material.renderPriority());
            value.add("effect", effectJson(material.effect()));
            materials.add(entry.getKey(), value);
         });
      return materials;
   }

   private static JsonObject effectJson(VisualEffectProfile effect) {
      VisualEffectProfile resolved = effect == null ? VisualEffectProfile.NONE : effect;
      JsonObject value = new JsonObject();
      value.addProperty("preset", resolved.kind().name().toLowerCase(java.util.Locale.ROOT));
      value.addProperty("glow_intensity", resolved.glowIntensity());
      value.addProperty("bloom_intensity", resolved.bloomIntensity());
      value.addProperty("pulse_speed", resolved.pulseSpeed());
      value.addProperty("pulse_min_alpha", resolved.pulseMinAlpha());
      value.addProperty("pulse_max_alpha", resolved.pulseMaxAlpha());
      value.addProperty("flicker_intensity", resolved.flickerIntensity());
      value.addProperty("scanline_strength", resolved.scanlineStrength());
      value.addProperty("hue_shift_speed", resolved.hueShiftSpeed());
      value.addProperty("depth_bias", resolved.depthBias());
      value.addProperty("advanced_enabled", resolved.advancedEnabled());
      value.addProperty("bloom_radius", resolved.bloomRadius());
      value.addProperty("bloom_threshold", resolved.bloomThreshold());
      value.addProperty("bloom_passes", resolved.bloomPasses());
      value.addProperty("screen_blend", resolved.screenBlend());
      value.addProperty("target_scope", resolved.targetScope().id());
      value.addProperty("bloom_mask_mode", resolved.bloomMaskMode().id());
      if (resolved.bloomTint() != null) {
         value.addProperty("bloom_tint", "#%08X".formatted(resolved.bloomTint()));
      }
      if (resolved.bloomMaskAlpha() != null) {
         value.addProperty("bloom_mask_alpha", resolved.bloomMaskAlpha());
      }
      value.addProperty("bloom_channel", resolved.effectiveBloomChannel());
      if (resolved.bloomDownscale() != null) {
         value.addProperty("bloom_downscale", resolved.bloomDownscale());
      }
      value.addProperty("advanced_priority", resolved.advancedPriority());
      value.addProperty("active", resolved.active());
      value.addProperty("cost", resolved.cost());
      value.addProperty("bloom_cost", resolved.bloomCost());
      value.addProperty("advanced_passes", resolved.advancedPassCount());
      return value;
   }

   private static JsonObject animationJson(AnimationProfile animationProfile) {
      JsonObject root = new JsonObject();
      if (animationProfile == null) {
         root.addProperty("clips", 0);
         root.addProperty("tracks", 0);
         return root;
      }
      root.addProperty("profile", animationProfile.id().toString());
      root.addProperty("clips", animationProfile.animations().size());
      root.addProperty("tracks", animationProfile.animations().values().stream().mapToInt(clip -> clip.tracks().size()).sum());
      root.add("clip_ids", strings(animationProfile.animations().keySet()));
      return root;
   }

   private static JsonObject particleJson(ParticleProfile particleProfile) {
      JsonObject root = new JsonObject();
      if (particleProfile == null) {
         root.addProperty("emitters", 0);
         return root;
      }
      root.addProperty("profile", particleProfile.id().toString());
      root.addProperty("emitters", particleProfile.emitters().size());
      root.add("emitter_ids", strings(particleProfile.emitters().keySet()));
      return root;
   }

   private static JsonArray strings(Iterable<String> values) {
      JsonArray array = new JsonArray();
      if (values != null) {
         for (String value : values) {
            array.add(value);
         }
      }
      return array;
   }

   private static String suggestedPath(Identifier id) {
      return "assets/" + id.getNamespace() + "/rendercore/previews/" + id.getPath() + ".preview.json";
   }
}
