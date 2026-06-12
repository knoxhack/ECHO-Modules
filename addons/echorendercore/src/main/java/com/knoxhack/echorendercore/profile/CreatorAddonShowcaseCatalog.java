package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.EchoRenderCore;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class CreatorAddonShowcaseCatalog {
   private CreatorAddonShowcaseCatalog() {
   }

   public static List<CreatorAddonIntegration> echoVisionCoverage() {
      return CreatorAddonShowcaseRegistry.sorted(List.of(
         integrated("echoagriculturereclamation", "Agriculture Reclamation", "global_atmosphere", "crop-restoration atmosphere field", "v13_atmosphere_field", "stable atmosphere required", "Verify aura particles and stable fallback."),
         noSurface("echoarmory", "Armory", "Equipment systems have no dedicated Render Core surface yet."),
         integrated("echoblackboxprotocol", "Blackbox Protocol", "terminal_hud", "diagnostic memory-core hologram", "v13_terminal_hud", "stable clean-glass HUD required", "Verify diagnostic HUD and memory-core hologram fallback without scanline banding."),
         integrated("echoblockworks", "Blockworks", "machines_blocks", "block part mask and material showcase", "v13_neon_cube_core", "isolated bloom optional, stable emissive required", "Verify baked block masks and fallback-safe block visuals."),
         integrated("echoconvoyprotocol", "Convoy Protocol", "motion_particles", "vehicle trails and orbiting route particles", "v13_orbit_particles", "stable particles required", "Verify convoy trails, sparks, and ring particles."),
         noSurface("echodatacore", "Data Core", "Common data host only."),
         integrated("echoholomap", "HoloMap", "holo_display", "holographic display layer and clean map chrome", "v13_hologram_display", "stable hologram required", "Verify hologram layers, clean glass chrome, and thumbnail capture."),
         integrated("echoindex", "Index", "terminal_hud", "index terminal overlay and inspection card", "v13_terminal_hud", "stable clean-glass HUD required", "Verify index browser HUD overlays without scanline banding."),
         integrated("echoindustrialnexus", "Industrial Nexus", "machines_blocks", "machine bloom masks and status panels", "v13_advanced_bloom", "isolated bloom optional, stable emissive required", "Verify machine named parts, block masks, and diagnostics."),
         integrated("echolens", "Lens", "holo_display", "debug inspection target and effect audit overlay", "v13_terminal_hud", "stable overlay required", "Verify lens debug targets and fallback visuals."),
         integrated("echologisticsnetwork", "Logistics Network", "motion_particles", "network node routing pulses", "v13_orbit_particles", "stable particles required", "Verify route particle resolver metadata."),
         noSurface("echomissioncore", "Mission Core", "Mission service integration only."),
         integrated("echomultiblockcore", "Multiblock Core", "machines_blocks", "anchor and named-part authoring coverage", "v13_neon_cube_core", "stable anchors required", "Verify multiblock anchors and block mask eligibility."),
         noSurface("echonetcore", "Net Core", "Network service integration only."),
         integrated("echonexusprotocol", "Nexus Protocol", "global_atmosphere", "global field and nexus bloom target", "v13_advanced_bloom", "fullscreen fallback allowed, stable required", "Verify global atmosphere profile routing."),
         integrated("echoorbitalremnants", "Orbital Remnants", "global_atmosphere", "ruin atmosphere and beacon particles", "v13_atmosphere_field", "stable atmosphere required", "Verify event-driven atmosphere particles."),
         integrated("echorendercore", "Render Core", "core_reference", "V21 V12 clean-glass and advanced-FX reference pack", "v21_cyberglass_overlay", "all fallback modes required", "Verify isolated, fullscreen, stable, shader unavailable, resize/reload, entity masks, block masks, screen chrome evidence, and world-surface evidence."),
         noSurface("echoruntimeguard", "Runtime Guard", "Runtime safety checks are service-only."),
         integrated("echosignalos", "Signal OS", "terminal_hud", "server rack terminal HUD overlay", "v13_terminal_hud", "stable clean-glass HUD required", "Verify terminal HUD overlays and clean fallback."),
         integrated("echostationfall", "Stationfall", "global_atmosphere", "station anomaly aura and fallback-safe overlay", "v13_fallback_safe", "stable fallback required", "Verify setpiece atmosphere and fallback status."),
         integrated("echoterminal", "Terminal", "terminal_hud", "terminal HUD and creator certification view", "v13_terminal_hud", "stable clean-glass HUD required", "Verify terminal overlays, thumbnails, and export metadata without scanline banding."),
         noSurface("echoworldcore", "World Core", "World service integration only.")
      ));
   }

   private static CreatorAddonIntegration integrated(
      String namespace,
      String displayName,
      String surfaceType,
      String surface,
      String profilePath,
      String fallbackExpectation,
      String qaNotes
   ) {
      return CreatorAddonShowcaseRegistry.integrated(
         namespace,
         displayName,
         surfaceType,
         surface,
         showcaseProfile(namespace, profilePath),
         fallbackExpectation,
         qaNotes,
         renderIntegrationStatus(namespace),
         convertedEntityCount(namespace),
         surfaceIntegration(namespace),
         "V21 showcase target declared through RenderCore creator-pack exports."
      );
   }

   private static Identifier showcaseProfile(String namespace, String profilePath) {
      return switch (namespace) {
         case "echoblockworks" -> Identifier.fromNamespaceAndPath(namespace, "static/hologram_floor_projector");
         case "echoterminal" -> Identifier.fromNamespaceAndPath(namespace, "echo_terminal");
         case "echosignalos" -> Identifier.fromNamespaceAndPath("signalos", "terminal");
         case "echoholomap" -> Identifier.fromNamespaceAndPath(namespace, "screen/minimap");
         case "echoindex" -> Identifier.fromNamespaceAndPath(namespace, "screen/index_overlay");
         case "echolens" -> Identifier.fromNamespaceAndPath(namespace, "screen/lens_overlay");
         case "echomultiblockcore" -> Identifier.fromNamespaceAndPath(namespace, "multiblock_controller");
         case "echoindustrialnexus" -> Identifier.fromNamespaceAndPath(namespace, "industrial_machine");
         case "echonexusprotocol" -> Identifier.fromNamespaceAndPath(namespace, "echo_mobs/nexus_guardian");
         case "echoorbitalremnants" -> Identifier.fromNamespaceAndPath(namespace, "echo_mobs/echo_zero");
         case "echologisticsnetwork" -> Identifier.fromNamespaceAndPath(namespace, "echo_mobs/courier_drone");
         case "echoconvoyprotocol" -> Identifier.fromNamespaceAndPath(namespace, "wasteland_rover");
         default -> Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, profilePath);
      };
   }

   private static String renderIntegrationStatus(String namespace) {
      return switch (namespace) {
         case "echonexusprotocol", "echoorbitalremnants", "echoindustrialnexus", "echologisticsnetwork",
              "echoconvoyprotocol" -> "rendercore_native";
         default -> "rendercore_native";
      };
   }

   private static int convertedEntityCount(String namespace) {
      return switch (namespace) {
         case "echonexusprotocol" -> 7;
         case "echoorbitalremnants" -> 11;
         case "echoindustrialnexus" -> 2;
         case "echologisticsnetwork", "echoconvoyprotocol" -> 1;
         default -> 0;
      };
   }

   private static CreatorSurfaceIntegration surfaceIntegration(String namespace) {
      return switch (namespace) {
         case "echoindustrialnexus" -> new CreatorSurfaceIntegration(1, 0, 2, 1, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "industrial_machine"),
            Identifier.fromNamespaceAndPath(namespace, "static/machine_status_panel"),
            Identifier.fromNamespaceAndPath(namespace, "static/warning_light")
         ));
         case "echoterminal" -> new CreatorSurfaceIntegration(1, 1, 0, 1, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "echo_terminal"),
            Identifier.fromNamespaceAndPath(namespace, "screen/terminal_hud")
         ));
         case "echosignalos" -> new CreatorSurfaceIntegration(2, 2, 0, 2, 0, List.of(
            Identifier.fromNamespaceAndPath("signalos", "terminal"),
            Identifier.fromNamespaceAndPath("signalos", "server_rack"),
            Identifier.fromNamespaceAndPath("signalos", "screen/terminal_hud"),
            Identifier.fromNamespaceAndPath("signalos", "screen/server_rack")
         ));
         case "echoholomap" -> new CreatorSurfaceIntegration(0, 1, 0, 0, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "screen/minimap")
         ));
         case "echoindex" -> new CreatorSurfaceIntegration(0, 1, 0, 0, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "screen/index_overlay")
         ));
         case "echolens" -> new CreatorSurfaceIntegration(0, 1, 0, 0, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "screen/lens_overlay")
         ));
         case "echoblockworks" -> new CreatorSurfaceIntegration(0, 0, 6, 0, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "static/broken_monitor"),
            Identifier.fromNamespaceAndPath(namespace, "static/hologram_floor_projector"),
            Identifier.fromNamespaceAndPath(namespace, "static/flickering_warning_light"),
            Identifier.fromNamespaceAndPath(namespace, "static/echo_strip_light"),
            Identifier.fromNamespaceAndPath(namespace, "static/sparking_cable_panel"),
            Identifier.fromNamespaceAndPath(namespace, "static/steam_vent")
         ));
         case "echomultiblockcore" -> new CreatorSurfaceIntegration(2, 0, 0, 2, 0, List.of(
            Identifier.fromNamespaceAndPath(namespace, "multiblock_controller"),
            Identifier.fromNamespaceAndPath(namespace, "robotic_arm")
         ));
         default -> CreatorSurfaceIntegration.EMPTY;
      };
   }

   private static CreatorAddonIntegration noSurface(String namespace, String displayName, String notes) {
      return CreatorAddonShowcaseRegistry.noVisualSurface(namespace, displayName, notes);
   }
}
