package com.knoxhack.echorendercore.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Deterministic V21 release-evidence catalog for shared clean-glass screen chrome.
 */
public final class RenderCoreScreenChromeQaCatalog {
   private static final List<ScreenChromeSurface> REQUIRED_SURFACES = List.of(
      surface("echoterminal", "echo_terminal", "Echo Terminal", "TERMINAL", "labeled:echo_terminal",
         "echoterminal", "screen/terminal_hud", false, "Verify dense terminal chrome with clean glass, no scanline banding, and no glass glints."),
      surface("echoterminal", "echo_terminal_reduced_motion", "Echo Terminal Reduced Motion", "TERMINAL", "labeled:echo_terminal",
         "echoterminal", "screen/terminal_hud", true, "Verify the reduced-motion terminal keeps clean glass, no scanline banding, but disables glints and chromatic edge."),
      surface("echosignalos", "signalos_terminal", "SignalOS Terminal", "TERMINAL", "labeled:signalos_terminal",
         "signalos", "screen/terminal_hud", false, "Verify SignalOS terminal clean glass, no scanline banding, and labeled terminal chrome."),
      surface("echosignalos", "signalos_rack", "SignalOS Rack", "CYBERGLASS", "labeled:signalos_rack",
         "signalos", "screen/server_rack", false, "Verify rack chrome stays slightly quieter than the terminal surface with clean glass and no scanline banding."),
      surface("echoholomap", "holomap_minimap", "HoloMap Minimap", "HOLOGRAM", "labeled:holomap",
         "echoholomap", "screen/minimap", false, "Verify no backdrop, light chromatic edge, map-readable hologram chrome, and no scanline banding."),
      surface("echoindex", "index_overlay", "Echo Index Overlay", "CYBERGLASS", "quiet_unlabeled",
         "echoindex", "screen/index_overlay", false, "Verify quiet unlabeled overlay chrome with fallback allowed, no backdrop, and no scanline banding."),
      surface("echolens", "lens_overlay", "Echo Lens Overlay", "HOLOGRAM", "scanning_unlabeled",
         "echolens", "screen/lens_overlay", false, "Verify scanning hologram corners, glints, no backdrop, and no scanline banding."),
      surface("echorendercore", "rendercore_cyberglass_example", "RenderCore Cyberglass Example", "CYBERGLASS", "labeled:cyberglass",
         "echorendercore", "v21_cyberglass_overlay", false, "Verify the generic example profile remains the reference cyberglass overlay with clean glass and no scanline banding.")
   );

   private RenderCoreScreenChromeQaCatalog() {
   }

   public static List<ScreenChromeSurface> requiredSurfaces() {
      return REQUIRED_SURFACES;
   }

   public static ScreenChromeSurface surface(String surfaceId) {
      String normalized = normalize(surfaceId, "");
      return REQUIRED_SURFACES.stream()
         .filter(surface -> surface.surfaceId().equals(normalized))
         .findFirst()
         .orElse(null);
   }

   public static List<CreatorVisualQaReport.ScreenChromeEvidence> pendingEvidence() {
      return REQUIRED_SURFACES.stream()
         .map(ScreenChromeSurface::pendingEvidence)
         .toList();
   }

   public static CreatorVisualQaReport.ScreenChromeEvidence evidence(
      String surfaceId,
      String status,
      String screenshotPath,
      String notes
   ) {
      ScreenChromeSurface surface = surface(surfaceId);
      if (surface == null) {
         return null;
      }
      return surface.evidence(status, screenshotPath, notes);
   }

   public static List<CreatorVisualQaReport.ScreenChromeEvidence> withRequiredSurfaces(
      List<CreatorVisualQaReport.ScreenChromeEvidence> evidence
   ) {
      return withRequiredSurfacesForNamespace("all", evidence);
   }

   public static List<CreatorVisualQaReport.ScreenChromeEvidence> withRequiredSurfacesForNamespace(
      String namespace,
      List<CreatorVisualQaReport.ScreenChromeEvidence> evidence
   ) {
      String normalizedNamespace = normalize(namespace, "all");
      Map<String, CreatorVisualQaReport.ScreenChromeEvidence> bySurface = new LinkedHashMap<>();
      for (ScreenChromeSurface surface : REQUIRED_SURFACES) {
         if (matchesNamespace(surface, normalizedNamespace)) {
            bySurface.put(surface.surfaceId(), surface.pendingEvidence());
         }
      }
      if (evidence != null) {
         for (CreatorVisualQaReport.ScreenChromeEvidence entry : evidence) {
            if (entry == null || !matchesNamespace(entry, normalizedNamespace)) {
               continue;
            }
            bySurface.put(entry.surfaceId(), enrich(entry));
         }
      }
      return bySurface.values().stream()
         .filter(Objects::nonNull)
         .sorted(Comparator
            .comparing(CreatorVisualQaReport.ScreenChromeEvidence::addonId)
            .thenComparing(CreatorVisualQaReport.ScreenChromeEvidence::surfaceId))
         .toList();
   }

   public static List<String> blockers(List<CreatorVisualQaReport.ScreenChromeEvidence> evidence) {
      ArrayList<String> blockers = new ArrayList<>();
      List<CreatorVisualQaReport.ScreenChromeEvidence> safeEvidence = evidence == null ? List.of() : evidence;
      for (CreatorVisualQaReport.ScreenChromeEvidence entry : safeEvidence) {
         if (entry == null || surface(entry.surfaceId()) == null || entry.passed()) {
            continue;
         }
         String suffix = "fail".equals(entry.qaStatus()) ? "_screen_chrome_evidence_failed" : "_screen_chrome_evidence_missing";
         blockers.add(entry.surfaceId() + suffix);
      }
      return blockers.stream().distinct().sorted().toList();
   }

   public static List<String> surfaceIds() {
      return REQUIRED_SURFACES.stream().map(ScreenChromeSurface::surfaceId).toList();
   }

   private static CreatorVisualQaReport.ScreenChromeEvidence enrich(CreatorVisualQaReport.ScreenChromeEvidence entry) {
      ScreenChromeSurface surface = surface(entry.surfaceId());
      if (surface == null) {
         return entry;
      }
      String notes = entry.notes().isBlank() ? surface.qaNotes() : entry.notes();
      return surface.evidence(entry.qaStatus(), entry.screenshotPath(), notes);
   }

   private static boolean matchesNamespace(ScreenChromeSurface surface, String namespace) {
      return "all".equals(namespace) || surface.addonId().equals(namespace);
   }

   private static boolean matchesNamespace(CreatorVisualQaReport.ScreenChromeEvidence entry, String namespace) {
      return "all".equals(namespace) || entry.addonId().equals(namespace);
   }

   private static ScreenChromeSurface surface(
      String addonId,
      String surfaceId,
      String displayName,
      String chromeStyle,
      String labelPolicy,
      String profileNamespace,
      String profilePath,
      boolean reducedMotion,
      String qaNotes
   ) {
      return new ScreenChromeSurface(
         addonId,
         surfaceId,
         displayName,
         chromeStyle,
         labelPolicy,
         Identifier.fromNamespaceAndPath(profileNamespace, profilePath),
         reducedMotion,
         qaNotes
      );
   }

   private static String normalize(String value, String fallback) {
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
   }

   public record ScreenChromeSurface(
      String addonId,
      String surfaceId,
      String displayName,
      String chromeStyle,
      String labelPolicy,
      Identifier profileId,
      boolean reducedMotion,
      String qaNotes
   ) {
      public ScreenChromeSurface {
         addonId = normalize(addonId, "unknown");
         surfaceId = normalize(surfaceId, "manual");
         displayName = displayName == null || displayName.isBlank() ? surfaceId : displayName.trim();
         chromeStyle = normalize(chromeStyle, "cyberglass").toUpperCase(Locale.ROOT);
         labelPolicy = normalize(labelPolicy, "unlabeled");
         profileId = profileId == null ? Identifier.fromNamespaceAndPath(addonId, surfaceId) : profileId;
         qaNotes = qaNotes == null ? "" : qaNotes.trim();
      }

      public CreatorVisualQaReport.ScreenChromeEvidence pendingEvidence() {
         return evidence("pending", "", qaNotes);
      }

      public CreatorVisualQaReport.ScreenChromeEvidence evidence(String status, String screenshotPath, String notes) {
         return new CreatorVisualQaReport.ScreenChromeEvidence(
            addonId,
            surfaceId,
            displayName,
            chromeStyle,
            labelPolicy,
            profileId.toString(),
            reducedMotion,
            status,
            notes == null || notes.isBlank() ? qaNotes : notes,
            screenshotPath
         );
      }
   }
}
