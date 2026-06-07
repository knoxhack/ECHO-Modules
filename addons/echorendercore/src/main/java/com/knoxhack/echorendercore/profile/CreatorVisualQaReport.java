package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Deterministic release-evidence summary for V21 creator-pack exports.
 */
public record CreatorVisualQaReport(
   String advancedFxMode,
   String advancedFxStatus,
   int screenshotEvidenceCount,
   List<String> testedFallbackModes,
   List<String> testedAddonNamespaces,
   List<String> remainingBlockers,
   List<EvidenceSnapshot> snapshots,
   List<ScreenChromeEvidence> screenChromeEvidence,
   List<String> screenChromeBlockers,
   List<WorldSurfaceEvidence> worldSurfaceEvidence,
   List<String> worldSurfaceBlockers
) {
   public static final CreatorVisualQaReport EMPTY = new CreatorVisualQaReport(
      "unverified",
      "visual_qa_pending",
      0,
      List.of(),
      List.of(),
      List.of(
         "isolated_bloom_evidence_missing",
         "fullscreen_fallback_evidence_missing",
         "stable_fallback_evidence_missing",
         "screenshot_evidence_missing"
      ),
      List.of(),
      List.of(),
      List.of(),
      List.of(),
      List.of()
   );

   private static final List<String> REQUIRED_FALLBACK_MODES = List.of(
      "isolated",
      "fullscreen_fallback",
      "stable_fallback",
      "shader_unavailable",
      "resize_reload",
      "entity_masks",
      "block_masks"
   );

   public CreatorVisualQaReport {
      advancedFxMode = normalizeText(advancedFxMode, "unverified");
      advancedFxStatus = normalizeText(advancedFxStatus, "visual_qa_pending");
      screenshotEvidenceCount = Math.max(0, screenshotEvidenceCount);
      testedFallbackModes = sortedStrings(testedFallbackModes);
      testedAddonNamespaces = sortedStrings(testedAddonNamespaces);
      remainingBlockers = sortedStrings(remainingBlockers);
      snapshots = snapshots == null
         ? List.of()
         : snapshots.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator
               .comparing(EvidenceSnapshot::label)
               .thenComparing(EvidenceSnapshot::statusLine)
               .thenComparing(EvidenceSnapshot::modeLine))
            .toList();
      screenChromeEvidence = screenChromeEvidence == null
         ? List.of()
         : screenChromeEvidence.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator
               .comparing(ScreenChromeEvidence::addonId)
               .thenComparing(ScreenChromeEvidence::surfaceId)
               .thenComparing(ScreenChromeEvidence::reducedMotion))
            .toList();
      screenChromeBlockers = sortedStrings(screenChromeBlockers);
      worldSurfaceEvidence = worldSurfaceEvidence == null
         ? List.of()
         : worldSurfaceEvidence.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator
               .comparing(WorldSurfaceEvidence::addonId)
               .thenComparing(WorldSurfaceEvidence::surfaceId))
            .toList();
      worldSurfaceBlockers = sortedStrings(worldSurfaceBlockers);
   }

   public CreatorVisualQaReport(
      String advancedFxMode,
      String advancedFxStatus,
      int screenshotEvidenceCount,
      List<String> testedFallbackModes,
      List<String> testedAddonNamespaces,
      List<String> remainingBlockers,
      List<EvidenceSnapshot> snapshots,
      List<ScreenChromeEvidence> screenChromeEvidence,
      List<String> screenChromeBlockers
   ) {
      this(
         advancedFxMode,
         advancedFxStatus,
         screenshotEvidenceCount,
         testedFallbackModes,
         testedAddonNamespaces,
         remainingBlockers,
         snapshots,
         screenChromeEvidence,
         screenChromeBlockers,
         List.of(),
         List.of()
      );
   }

   public CreatorVisualQaReport(
      String advancedFxMode,
      String advancedFxStatus,
      int screenshotEvidenceCount,
      List<String> testedFallbackModes,
      List<String> testedAddonNamespaces,
      List<String> remainingBlockers,
      List<EvidenceSnapshot> snapshots
   ) {
      this(
         advancedFxMode,
         advancedFxStatus,
         screenshotEvidenceCount,
         testedFallbackModes,
         testedAddonNamespaces,
         remainingBlockers,
         snapshots,
         List.of(),
         List.of()
      );
   }

   public static CreatorVisualQaReport fromAddonIntegrations(List<CreatorAddonIntegration> addonIntegrations) {
      return fromSnapshots(List.of(), addonIntegrations);
   }

   public static CreatorVisualQaReport fromSnapshots(
      List<EvidenceSnapshot> snapshots,
      List<CreatorAddonIntegration> addonIntegrations
   ) {
      return fromSnapshots(snapshots, addonIntegrations, List.of());
   }

   public static CreatorVisualQaReport fromSnapshots(
      List<EvidenceSnapshot> snapshots,
      List<CreatorAddonIntegration> addonIntegrations,
      List<ScreenChromeEvidence> screenChromeEvidence
   ) {
      return fromSnapshots(snapshots, addonIntegrations, screenChromeEvidence, List.of());
   }

   public static CreatorVisualQaReport fromSnapshots(
      List<EvidenceSnapshot> snapshots,
      List<CreatorAddonIntegration> addonIntegrations,
      List<ScreenChromeEvidence> screenChromeEvidence,
      List<WorldSurfaceEvidence> worldSurfaceEvidence
   ) {
      List<EvidenceSnapshot> safeSnapshots = snapshots == null ? List.of() : snapshots;
      List<ScreenChromeEvidence> resolvedScreenChrome =
         RenderCoreScreenChromeQaCatalog.withRequiredSurfaces(screenChromeEvidence);
      List<WorldSurfaceEvidence> resolvedWorldSurface = worldSurfaceEvidence == null ? List.of() : worldSurfaceEvidence;
      Set<String> modes = new TreeSet<>();
      int screenshotCount = 0;
      String latestMode = "unverified";
      String latestStatus = "visual_qa_pending";

      for (EvidenceSnapshot snapshot : safeSnapshots) {
         if (snapshot == null) {
            continue;
         }
         if (!snapshot.screenshotPath().isBlank()) {
            screenshotCount++;
         }
         modes.addAll(snapshot.inferredModes());
         latestMode = normalizeText(snapshot.modeLine(), latestMode);
         latestStatus = normalizeText(snapshot.statusLine(), latestStatus);
      }

      Set<String> namespaces = new TreeSet<>();
      if (addonIntegrations != null) {
         for (CreatorAddonIntegration integration : addonIntegrations) {
            if (integration != null && "integrated".equalsIgnoreCase(integration.status())) {
               namespaces.add(integration.namespace());
            }
         }
      }

      Set<String> blockers = new TreeSet<>();
      for (String requiredMode : REQUIRED_FALLBACK_MODES) {
         if (!modes.contains(requiredMode)) {
            blockers.add(requiredMode + "_evidence_missing");
         }
      }
      if (screenshotCount == 0) {
         blockers.add("screenshot_evidence_missing");
      }

      return new CreatorVisualQaReport(
         latestMode,
         latestStatus,
         screenshotCount,
         List.copyOf(modes),
         List.copyOf(namespaces),
         List.copyOf(blockers),
         safeSnapshots,
         resolvedScreenChrome,
         RenderCoreScreenChromeQaCatalog.blockers(resolvedScreenChrome),
         resolvedWorldSurface,
         worldSurfaceBlockers(resolvedWorldSurface)
      );
   }

   public CreatorVisualQaReport forNamespace(String namespace, List<CreatorAddonIntegration> addonIntegrations) {
      String normalized = normalizeText(namespace, "all");
      if ("all".equals(normalized)) {
         return this;
      }
      Set<String> namespaces = new TreeSet<>();
      if (addonIntegrations != null) {
         for (CreatorAddonIntegration integration : addonIntegrations) {
            if (integration != null
               && normalized.equals(integration.namespace())
               && "integrated".equalsIgnoreCase(integration.status())) {
               namespaces.add(integration.namespace());
            }
         }
      }
      List<ScreenChromeEvidence> filteredScreenChrome =
         RenderCoreScreenChromeQaCatalog.withRequiredSurfacesForNamespace(normalized, screenChromeEvidence);
      List<WorldSurfaceEvidence> filteredWorldSurface = worldSurfaceEvidence.stream()
         .filter(entry -> normalized.equals(entry.addonId()))
         .toList();
      return new CreatorVisualQaReport(
         advancedFxMode,
         advancedFxStatus,
         screenshotEvidenceCount,
         testedFallbackModes,
         List.copyOf(namespaces),
         remainingBlockers,
         snapshots,
         filteredScreenChrome,
         RenderCoreScreenChromeQaCatalog.blockers(filteredScreenChrome),
         filteredWorldSurface,
         worldSurfaceBlockers(filteredWorldSurface)
      );
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("advanced_fx_mode", advancedFxMode);
      root.addProperty("advanced_fx_status", advancedFxStatus);
      root.addProperty("screenshot_evidence_count", screenshotEvidenceCount);
      root.add("tested_fallback_modes", stringArray(testedFallbackModes));
      root.add("tested_addon_namespaces", stringArray(testedAddonNamespaces));
      root.add("remaining_blockers", stringArray(remainingBlockers));
      JsonArray snapshotArray = new JsonArray();
      for (EvidenceSnapshot snapshot : snapshots) {
         snapshotArray.add(snapshot.toJson());
      }
      root.add("snapshots", snapshotArray);
      root.addProperty("screen_chrome_surface_count", screenChromeEvidence.size());
      root.addProperty("screen_chrome_evidence_count", screenChromeEvidenceCount());
      root.add("screen_chrome_blockers", stringArray(screenChromeBlockers));
      JsonArray screenChromeArray = new JsonArray();
      for (ScreenChromeEvidence evidence : screenChromeEvidence) {
         screenChromeArray.add(evidence.toJson());
      }
      root.add("screen_chrome_evidence", screenChromeArray);
      root.addProperty("world_surface_evidence_count", worldSurfaceEvidenceCount());
      root.add("world_surface_blockers", stringArray(worldSurfaceBlockers));
      JsonArray worldSurfaceArray = new JsonArray();
      for (WorldSurfaceEvidence evidence : worldSurfaceEvidence) {
         worldSurfaceArray.add(evidence.toJson());
      }
      root.add("world_surface_evidence", worldSurfaceArray);
      return root;
   }

   public JsonObject toAdvancedFxJson() {
      JsonObject root = new JsonObject();
      root.addProperty("evidence_kind", "advanced_fx");
      root.addProperty("advanced_fx_mode", advancedFxMode);
      root.addProperty("advanced_fx_status", advancedFxStatus);
      root.addProperty("screenshot_evidence_count", screenshotEvidenceCount);
      root.add("tested_fallback_modes", stringArray(testedFallbackModes));
      root.add("tested_addon_namespaces", stringArray(testedAddonNamespaces));
      root.add("remaining_blockers", stringArray(remainingBlockers));
      JsonArray snapshotArray = new JsonArray();
      for (EvidenceSnapshot snapshot : snapshots) {
         snapshotArray.add(snapshot.toJson());
      }
      root.add("snapshots", snapshotArray);
      return root;
   }

   public JsonObject toScreenChromeJson() {
      JsonObject root = new JsonObject();
      root.addProperty("evidence_kind", "screen_chrome");
      root.addProperty("screen_chrome_surface_count", screenChromeEvidence.size());
      root.addProperty("screen_chrome_evidence_count", screenChromeEvidenceCount());
      root.add("screen_chrome_blockers", stringArray(screenChromeBlockers));
      JsonArray screenChromeArray = new JsonArray();
      for (ScreenChromeEvidence evidence : screenChromeEvidence) {
         screenChromeArray.add(evidence.toJson());
      }
      root.add("screen_chrome_evidence", screenChromeArray);
      return root;
   }

   public JsonObject toWorldSurfaceJson() {
      JsonObject root = new JsonObject();
      root.addProperty("evidence_kind", "world_surface");
      root.addProperty("world_surface_evidence_count", worldSurfaceEvidenceCount());
      root.add("world_surface_blockers", stringArray(worldSurfaceBlockers));
      JsonArray worldSurfaceArray = new JsonArray();
      for (WorldSurfaceEvidence evidence : worldSurfaceEvidence) {
         worldSurfaceArray.add(evidence.toJson());
      }
      root.add("world_surface_evidence", worldSurfaceArray);
      return root;
   }

   public String summaryLine() {
      return "visual_qa "
         + advancedFxStatus
         + " screenshots="
         + screenshotEvidenceCount
         + " modes="
         + testedFallbackModes.size()
         + " blockers="
         + remainingBlockers.size()
         + " screen_surfaces="
         + screenChromeEvidence.size()
         + " screen_blockers="
         + screenChromeBlockers.size()
         + " world_surfaces="
         + worldSurfaceEvidence.size()
         + " world_blockers="
         + worldSurfaceBlockers.size();
   }

   public int screenChromeEvidenceCount() {
      return (int)screenChromeEvidence.stream().filter(ScreenChromeEvidence::passed).count();
   }

   public int totalBlockerCount() {
      return remainingBlockers.size() + screenChromeBlockers.size() + worldSurfaceBlockers.size();
   }

   public int worldSurfaceEvidenceCount() {
      return (int)worldSurfaceEvidence.stream().filter(WorldSurfaceEvidence::passed).count();
   }

   private static List<String> sortedStrings(List<String> values) {
      if (values == null) {
         return List.of();
      }
      return values.stream()
         .filter(Objects::nonNull)
         .map(value -> normalizeText(value, ""))
         .filter(value -> !value.isBlank())
         .distinct()
         .sorted()
         .toList();
   }

   private static JsonArray stringArray(List<String> values) {
      JsonArray array = new JsonArray();
      for (String value : values) {
         array.add(value);
      }
      return array;
   }

   private static String normalizeText(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
   }

   public record EvidenceSnapshot(
      String label,
      String statusLine,
      String modeLine,
      String fallbackReason,
      String unavailableReason,
      int maskSubmissions,
      int skippedSubmissions,
      int activeEffects,
      int channelCount,
      int downscale,
      int passCount,
      int bloomCost,
      boolean compileFailed,
      String screenshotPath
   ) {
      public EvidenceSnapshot {
         label = normalizeText(label, "manual");
         statusLine = normalizeText(statusLine, "effects_stable");
         modeLine = normalizeText(modeLine, "unverified");
         fallbackReason = normalizeText(fallbackReason, "");
         unavailableReason = normalizeText(unavailableReason, "");
         maskSubmissions = Math.max(0, maskSubmissions);
         skippedSubmissions = Math.max(0, skippedSubmissions);
         activeEffects = Math.max(0, activeEffects);
         channelCount = Math.max(0, channelCount);
         downscale = Math.max(0, downscale);
         passCount = Math.max(0, passCount);
         bloomCost = Math.max(0, bloomCost);
         screenshotPath = sanitizePath(screenshotPath);
      }

      public List<String> inferredModes() {
         ArrayList<String> modes = new ArrayList<>();
         String combined = statusLine + " " + modeLine + " " + fallbackReason + " " + unavailableReason + " " + label;
         if (combined.contains("advanced_isolated")) {
            modes.add("isolated");
         }
         if (combined.contains("fullscreen_fallback")) {
            modes.add("fullscreen_fallback");
         }
         if (combined.contains("stable_fallback") || combined.contains("effects_stable")) {
            modes.add("stable_fallback");
         }
         if (compileFailed || combined.contains("compile_failed") || combined.contains("unavailable") || combined.contains("shader")) {
            modes.add("shader_unavailable");
         }
         if (combined.contains("resize") || combined.contains("reload")) {
            modes.add("resize_reload");
         }
         if (combined.contains("entity")) {
            modes.add("entity_masks");
         }
         if (combined.contains("block")) {
            modes.add("block_masks");
         }
         return modes;
      }

      public JsonObject toJson() {
         JsonObject root = new JsonObject();
         root.addProperty("label", label);
         root.addProperty("status_line", statusLine);
         root.addProperty("mode_line", modeLine);
         root.addProperty("fallback_reason", fallbackReason);
         root.addProperty("unavailable_reason", unavailableReason);
         root.addProperty("mask_submissions", maskSubmissions);
         root.addProperty("skipped_submissions", skippedSubmissions);
         root.addProperty("active_effects", activeEffects);
         root.addProperty("channel_count", channelCount);
         root.addProperty("downscale", downscale);
         root.addProperty("pass_count", passCount);
         root.addProperty("bloom_cost", bloomCost);
         root.addProperty("compile_failed", compileFailed);
         root.addProperty("screenshot_path", screenshotPath);
         root.add("tested_modes", stringArray(inferredModes().stream().distinct().sorted().toList()));
         return root;
      }
   }

   public record ScreenChromeEvidence(
      String addonId,
      String surfaceId,
      String displayName,
      String chromeStyle,
      String labelPolicy,
      String profileId,
      boolean reducedMotion,
      String qaStatus,
      String notes,
      String screenshotPath
   ) {
      public ScreenChromeEvidence {
         addonId = normalizeText(addonId, "unknown");
         surfaceId = normalizeText(surfaceId, "manual");
         displayName = cleanText(displayName, surfaceId);
         chromeStyle = normalizeText(chromeStyle, "cyberglass").toUpperCase(Locale.ROOT);
         labelPolicy = normalizeText(labelPolicy, "unlabeled");
         profileId = normalizeText(profileId, "");
         qaStatus = normalizeText(qaStatus, "pending");
         notes = cleanText(notes, "");
         screenshotPath = sanitizePath(screenshotPath);
      }

      public boolean passed() {
         return "pass".equals(qaStatus) && !screenshotPath.isBlank();
      }

      public ScreenChromeEvidence withQa(String status, String path, String qaNotes) {
         return new ScreenChromeEvidence(
            addonId,
            surfaceId,
            displayName,
            chromeStyle,
            labelPolicy,
            profileId,
            reducedMotion,
            status,
            qaNotes,
            path
         );
      }

      public JsonObject toJson() {
         JsonObject root = new JsonObject();
         root.addProperty("addon_id", addonId);
         root.addProperty("surface_id", surfaceId);
         root.addProperty("display_name", displayName);
         root.addProperty("chrome_style", chromeStyle);
         root.addProperty("label_policy", labelPolicy);
         root.addProperty("profile_id", profileId);
         root.addProperty("reduced_motion", reducedMotion);
         root.addProperty("qa_status", qaStatus);
         root.addProperty("notes", notes);
         root.addProperty("screenshot_path", screenshotPath);
         return root;
      }
   }

   public record WorldSurfaceEvidence(
      String addonId,
      String surfaceId,
      String profileId,
      String surfaceType,
      String qaStatus,
      String notes,
      String screenshotPath
   ) {
      public WorldSurfaceEvidence {
         addonId = normalizeText(addonId, "unknown");
         surfaceId = normalizeText(surfaceId, "manual");
         profileId = normalizeText(profileId, "");
         surfaceType = normalizeText(surfaceType, "unknown");
         qaStatus = normalizeText(qaStatus, "pending");
         notes = cleanText(notes, "");
         screenshotPath = sanitizePath(screenshotPath);
      }

      public boolean passed() {
         return "pass".equals(qaStatus) && !screenshotPath.isBlank();
      }

      public JsonObject toJson() {
         JsonObject root = new JsonObject();
         root.addProperty("addon_id", addonId);
         root.addProperty("surface_id", surfaceId);
         root.addProperty("profile_id", profileId);
         root.addProperty("surface_type", surfaceType);
         root.addProperty("qa_status", qaStatus);
         root.addProperty("notes", notes);
         root.addProperty("screenshot_path", screenshotPath);
         return root;
      }
   }

   private static List<String> worldSurfaceBlockers(List<WorldSurfaceEvidence> evidence) {
      if (evidence == null || evidence.isEmpty()) {
         return List.of();
      }
      return evidence.stream()
         .filter(entry -> entry != null && !entry.passed())
         .map(entry -> entry.surfaceId() + "_world_surface_evidence_missing")
         .sorted()
         .toList();
   }

   private static String sanitizePath(String value) {
      if (value == null || value.isBlank()) {
         return "";
      }
      String normalized = value.replace('\\', '/').replaceFirst("^/+", "");
      if (normalized.matches("^[A-Za-z]:/.*")) {
         return "";
      }
      return normalized;
   }

   private static String cleanText(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback == null ? "" : fallback;
      }
      return value.trim();
   }
}
