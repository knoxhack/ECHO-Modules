package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class RenderCoreCreatorPackExporter {
   private RenderCoreCreatorPackExporter() {
   }

   public static CreatorExportIndex export(RenderCoreProfiles.LoadedContent content) {
      return export(content, ProfileScreenshotPreviewProvider.NO_OP);
   }

   public static CreatorExportIndex export(RenderCoreProfiles.LoadedContent content, ProfileScreenshotPreviewProvider screenshotProvider) {
      RenderCoreProfiles.LoadedContent loaded = content == null ? RenderCoreProfiles.LoadedContent.EMPTY : content;
      return export(
         loaded.visualProfiles(),
         loaded.animationProfiles(),
         loaded.particleProfiles(),
         loaded.diagnosticsReport(),
         screenshotProvider
      );
   }

   public static CreatorExportIndex export(
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
      ArrayList<CreatorProfileCard> cards = new ArrayList<>();
      ArrayList<CreatorProfileAudit> audits = new ArrayList<>();
      ArrayList<CreatorMigrationReport> migrationReports = new ArrayList<>();
      ArrayList<CreatorPackArtifact> artifacts = new ArrayList<>();
      safeVisuals.values().stream()
         .sorted(Comparator.comparing(profile -> profile.id().toString()))
         .forEach(profile -> buildProfile(
            profile,
            safeParticles,
            safeDiagnostics,
            safeScreenshotProvider,
            cards,
            audits,
            migrationReports,
            artifacts
         ));
      CreatorPackManifest manifest = new CreatorPackManifest(
         "echorendercore",
         "all",
         CreatorPackManifest.CREATOR_PACK_VERSION,
         cards.size(),
         (int)cards.stream().filter(CreatorProfileCard::migrationRequired).count(),
         (int)cards.stream().filter(CreatorProfileCard::screenshotAvailable).count(),
         safeDiagnostics.cacheMetrics()
      );
      List<CreatorAddonIntegration> addonIntegrations = CreatorAddonShowcaseCatalog.echoVisionCoverage();
      CreatorCertificationReport certification = CreatorCertificationReport.certify(
         "all",
         cards,
         audits,
         migrationReports,
         safeDiagnostics,
         CreatorCertificationPolicy.ERRORS_ONLY
      );
      CreatorVisualQaReport visualQa = CreatorVisualQaReport.fromSnapshots(
         List.of(),
         addonIntegrations,
         List.of(),
         worldSurfaceEvidence(safeVisuals)
      );
      return new CreatorExportIndex(manifest, certification, visualQa, cards, audits, migrationReports, artifacts, addonIntegrations);
   }

   private static void buildProfile(
         VisualProfile profile,
         Map<Identifier, ParticleProfile> particles,
         ProfileDiagnosticsReport diagnostics,
         ProfileScreenshotPreviewProvider screenshotProvider,
         List<CreatorProfileCard> cards,
         List<CreatorProfileAudit> audits,
         List<CreatorMigrationReport> migrationReports,
         List<CreatorPackArtifact> artifacts) {
      JsonObject source = RenderCoreProfileMigration.normalizedVisualProfileJson(profile);
      source.addProperty("schema_version", profile.schemaVersion());
      CreatorMigrationReport migrationReport = RenderCoreProfileMigration.migrateVisualProfile(profile.id(), source);
      migrationReports.add(migrationReport);
      ParticleProfile particleProfile = profile.particleProfile() == null ? null : particles.get(profile.particleProfile());
      int emitterCount = particleProfile == null ? 0 : particleProfile.emitters().size();
      ProfilePerformanceSummary summary = diagnostics.performanceReport().summaries().get(profile.id().toString());
      List<ProfileValidationIssue> validationIssues = new ArrayList<>(diagnostics.validationReport().issues().stream()
         .filter(issue -> profile.id().equals(issue.profileId()))
         .toList());
      validationIssues.addAll(migrationReport.issues());
      List<ProfilePerformanceIssue> performanceIssues = diagnostics.performanceReport().issues().stream()
         .filter(issue -> profile.id().equals(issue.profileId()))
         .toList();
      boolean screenshotAvailable = screenshotProvider.available(profile);
      String screenshotSkippedReason = screenshotAvailable ? "" : screenshotProvider.skippedReason(profile);
      String screenshotPath = screenshotAvailable ? screenshotProvider.relativePath(profile) : "";
      CreatorProfileCard card = new CreatorProfileCard(
         profile.id(),
         title(profile.id()),
         profile.schemaVersion(),
         migrationReport.migrationRequired(),
         profile.layers().size(),
         profile.materials().size(),
         emitterCount,
         summary == null ? activeEffectCount(profile) : summary.activeEffectCount(),
         effectPresets(profile),
         validationIssues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING).count(),
         validationIssues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.ERROR).count(),
         (int)performanceIssues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING).count(),
         screenshotAvailable,
         screenshotProvider.id(),
         screenshotPath,
         screenshotSkippedReason,
         suggestedArtifactPath(profile.id())
      );
      CreatorProfileAudit audit = new CreatorProfileAudit(profile.id(), validationIssues, performanceIssues, summary);
      cards.add(card);
      audits.add(audit);
      artifacts.add(new CreatorPackArtifact(profile.id(), card.suggestedArtifactPath(),
         artifactJson(profile, card, audit, migrationReport)));
   }

   private static JsonObject artifactJson(
         VisualProfile profile,
         CreatorProfileCard card,
         CreatorProfileAudit audit,
         CreatorMigrationReport migrationReport) {
      JsonObject root = new JsonObject();
      root.addProperty("creator_schema_version", CreatorPackManifest.CREATOR_PACK_VERSION);
      root.addProperty("target_schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
      root.addProperty("profile", profile.id().toString());
      root.add("card", card.toJson());
      root.add("audit", audit.toJson());
      root.add("migration", migrationReport.toJson());
      root.add("normalized_profile", migrationReport.migratedJson());
      root.add("workbench_draft", CreatorProfileDraft.from(profile, card).toJson());
      return root;
   }

   private static List<String> effectPresets(VisualProfile profile) {
      LinkedHashSet<String> presets = new LinkedHashSet<>();
      addEffectPreset(presets, profile.effect());
      profile.materials().values().forEach(material -> addEffectPreset(presets, material.effect()));
      profile.layers().forEach(layer -> addEffectPreset(presets, profile.effectFor(layer)));
      if (presets.isEmpty()) {
         presets.add("none");
      }
      return List.copyOf(presets);
   }

   private static void addEffectPreset(LinkedHashSet<String> presets, VisualEffectProfile effect) {
      VisualEffectProfile resolved = effect == null ? VisualEffectProfile.NONE : effect;
      if (resolved.active() || resolved.kind() != VisualEffectKind.NONE) {
         presets.add(resolved.kind().name().toLowerCase(java.util.Locale.ROOT));
      }
   }

   private static int activeEffectCount(VisualProfile profile) {
      int count = profile.effect().active() ? 1 : 0;
      count += (int)profile.materials().values().stream().filter(material -> material.effect().active()).count();
      count += (int)profile.layers().stream().filter(layer -> profile.effectFor(layer).active()).count();
      return count;
   }

   private static List<CreatorVisualQaReport.WorldSurfaceEvidence> worldSurfaceEvidence(Map<Identifier, VisualProfile> visuals) {
      return visuals.values().stream()
         .filter(profile -> profile.qa().required() && !profile.surface().screenLike())
         .sorted(Comparator.comparing(profile -> profile.id().toString()))
         .map(profile -> new CreatorVisualQaReport.WorldSurfaceEvidence(
            profile.surface().ownerAddon(),
            profile.id().getPath(),
            profile.id().toString(),
            profile.surface().type().id(),
            "pending",
            "Capture manual V21 world-surface evidence for this required profile.",
            ""
         ))
         .toList();
   }

   private static String suggestedArtifactPath(Identifier id) {
      return "assets/" + id.getNamespace() + "/rendercore/creator/profiles/" + id.getPath() + ".creator.json";
   }

   private static String title(Identifier id) {
      if (id == null) {
         return "Unknown Profile";
      }
      String name = id.getPath().replace('_', ' ').replace('/', ' ');
      if (name.isBlank()) {
         return id.toString();
      }
      StringBuilder builder = new StringBuilder();
      for (String part : name.split(" ")) {
         if (part.isBlank()) {
            continue;
         }
         if (!builder.isEmpty()) {
            builder.append(' ');
         }
         builder.append(Character.toUpperCase(part.charAt(0)));
         if (part.length() > 1) {
            builder.append(part.substring(1));
         }
      }
      return builder.isEmpty() ? id.toString() : builder.toString();
   }
}
