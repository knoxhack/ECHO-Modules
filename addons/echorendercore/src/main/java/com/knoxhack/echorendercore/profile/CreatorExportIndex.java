package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public record CreatorExportIndex(
   CreatorPackManifest manifest,
   CreatorCertificationReport certification,
   CreatorVisualQaReport visualQa,
   List<CreatorProfileCard> cards,
   List<CreatorProfileAudit> audits,
   List<CreatorMigrationReport> migrationReports,
   List<CreatorPackArtifact> artifacts,
   List<CreatorAddonIntegration> addonIntegrations
) {
   public static final CreatorExportIndex EMPTY = new CreatorExportIndex(
      new CreatorPackManifest("echorendercore", "all", CreatorPackManifest.CREATOR_PACK_VERSION, 0, 0, 0, ProfileCacheMetrics.EMPTY),
      CreatorCertificationReport.EMPTY,
      CreatorVisualQaReport.EMPTY,
      List.of(),
      List.of(),
      List.of(),
      List.of(),
      List.of()
   );

   public CreatorExportIndex {
      manifest = manifest == null
         ? new CreatorPackManifest("echorendercore", "all", CreatorPackManifest.CREATOR_PACK_VERSION, 0, 0, 0, ProfileCacheMetrics.EMPTY)
         : manifest;
      certification = certification == null ? CreatorCertificationReport.EMPTY : certification;
      visualQa = visualQa == null ? CreatorVisualQaReport.EMPTY : visualQa;
      cards = cards == null ? List.of() : List.copyOf(cards);
      audits = audits == null ? List.of() : List.copyOf(audits);
      migrationReports = migrationReports == null ? List.of() : List.copyOf(migrationReports);
      artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
      addonIntegrations = addonIntegrations == null ? List.of() : List.copyOf(addonIntegrations);
   }

   public CreatorExportIndex forNamespace(String namespace) {
      if (namespace == null || namespace.isBlank() || namespace.equalsIgnoreCase("all")) {
         return this;
      }
      String normalized = namespace.toLowerCase(Locale.ROOT);
      List<CreatorProfileCard> filteredCards = cards.stream()
         .filter(card -> namespaceMatches(card.profileId(), normalized))
         .toList();
      List<CreatorProfileAudit> filteredAudits = audits.stream()
         .filter(audit -> namespaceMatches(audit.profileId(), normalized))
         .toList();
      List<CreatorMigrationReport> filteredReports = migrationReports.stream()
         .filter(report -> namespaceMatches(report.profileId(), normalized))
         .toList();
      List<CreatorPackArtifact> filteredArtifacts = artifacts.stream()
         .filter(artifact -> namespaceMatches(artifact.id(), normalized))
         .toList();
      List<CreatorAddonIntegration> filteredIntegrations = addonIntegrations.stream()
         .filter(integration -> normalized.equals(integration.namespace()))
         .toList();
      CreatorPackManifest filteredManifest = new CreatorPackManifest(
         manifest.generator(),
         normalized,
         manifest.schemaVersion(),
         filteredCards.size(),
         (int)filteredCards.stream().filter(CreatorProfileCard::migrationRequired).count(),
         (int)filteredCards.stream().filter(CreatorProfileCard::screenshotAvailable).count(),
         manifest.cacheMetrics()
      );
      CreatorCertificationReport filteredCertification =
         certification.forNamespace(normalized, filteredCards, filteredAudits, filteredReports);
      return new CreatorExportIndex(
         filteredManifest,
         filteredCertification,
         visualQa.forNamespace(normalized, filteredIntegrations),
         filteredCards,
         filteredAudits,
         filteredReports,
         filteredArtifacts,
         filteredIntegrations
      );
   }

   public CreatorExportIndex withVisualQa(CreatorVisualQaReport report) {
      return new CreatorExportIndex(
         manifest,
         certification,
         report == null ? CreatorVisualQaReport.EMPTY : report,
         cards,
         audits,
         migrationReports,
         artifacts,
         addonIntegrations
      );
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.add("manifest", manifest.toJson());
      root.add("certification", certification.toJson());
      root.add("visual_qa", visualQa.toJson());
      root.add("surface_integration", aggregateSurfaceIntegration().toJson());
      JsonArray profiles = new JsonArray();
      cards.stream()
         .sorted(java.util.Comparator.comparing(card -> card.profileId().toString()))
         .forEach(card -> profiles.add(card.toJson()));
      root.add("profiles", profiles);
      JsonArray auditArray = new JsonArray();
      audits.stream()
         .sorted(java.util.Comparator.comparing(audit -> audit.profileId().toString()))
         .forEach(audit -> auditArray.add(audit.toJson()));
      root.add("audits", auditArray);
      JsonArray migrations = new JsonArray();
      migrationReports.stream()
         .sorted(java.util.Comparator.comparing(report -> report.profileId().toString()))
         .forEach(report -> migrations.add(report.toJson()));
      root.add("migration_reports", migrations);
      JsonArray integrations = new JsonArray();
      addonIntegrations.stream()
         .sorted(java.util.Comparator.comparing(CreatorAddonIntegration::namespace))
         .forEach(integration -> integrations.add(integration.toJson()));
      root.add("addon_integrations", integrations);
      return root;
   }

   public String summaryLine() {
      return cards.size() + " creator profile card(s), migrations "
         + migrationReports.stream().filter(CreatorMigrationReport::migrationRequired).count()
         + ", artifacts " + artifacts.size()
         + ", integrations " + addonIntegrations.size()
         + ", surfaces " + aggregateSurfaceIntegration().worldSurfaceCount() + "/"
         + aggregateSurfaceIntegration().screenSurfaceCount() + "/"
         + aggregateSurfaceIntegration().particleOnlyStaticSurfaceCount()
         + ", " + certification.summaryLine()
         + ", " + visualQa.summaryLine();
   }

   private CreatorSurfaceIntegration aggregateSurfaceIntegration() {
      int world = 0;
      int screen = 0;
      int statics = 0;
      int blockEntities = 0;
      int fallbacks = 0;
      java.util.ArrayList<Identifier> profiles = new java.util.ArrayList<>();
      for (CreatorAddonIntegration integration : addonIntegrations) {
         CreatorSurfaceIntegration surface = integration.surfaceIntegration();
         world += surface.worldSurfaceCount();
         screen += surface.screenSurfaceCount();
         statics += surface.particleOnlyStaticSurfaceCount();
         blockEntities += surface.convertedBlockEntityCount();
         fallbacks += surface.fallbackRendererCount();
         profiles.addAll(surface.profileIds());
      }
      return new CreatorSurfaceIntegration(world, screen, statics, blockEntities, fallbacks, profiles);
   }

   private static boolean namespaceMatches(Identifier id, String namespace) {
      return id != null && id.getNamespace().equals(namespace);
   }
}
