package com.knoxhack.echoarmory.integration;

import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.util.ArrayList;
import java.util.List;

public final class ArmoryOptionalIntegrations {
   private static final List<Sibling> SIBLINGS = List.of(
      new Sibling("echocore", "Core route records, diagnostics, hazard telemetry, discovery, and config summaries"),
      new Sibling("echoterminal", "Terminal Armory tab, action receipts, readiness panels, and archive entries"),
      new Sibling("echologisticsnetwork", "Route-kit dispatch, depot offers, categories, and restock-safe presets"),
      new Sibling("echomissioncore", "Side-op chain from first craft through boss-prep dispatch"),
      new Sibling("echoindex", "Station recipes, compatibility, and readiness recipe index pages"),
      new Sibling("echolens", "Lens providers for gear, stations, projectiles, staged kits, and blockers"),
      new Sibling("echoholomap", "Route-kit markers and hazard-aware route hints"),
      new Sibling("echoworldcore", "Region and hazard reactions for readiness hints"),
      new Sibling("echoruntimeguard", "Budget labels for projectile scans, station searches, and readiness recompute"),
      new Sibling("echosoundcore", "Guarded Armory sound contexts"),
      new Sibling("echothemecore", "Armory visual theme tokens"),
      new Sibling("echorendercore", "Projectile and station render profiles"),
      new Sibling("echotutorialcore", "Contextual Armory tutorials"),
      new Sibling("echoashfallprotocol", "Ashfall faction, material, boss, and route profiles"),
      new Sibling("echoorbitalremnants", "Orbital route profiles and assault readiness records"),
      new Sibling("echostationfall", "Station route profiles and boarding kit recommendations"),
      new Sibling("echonexusprotocol", "Nexus route profiles and convergence gear recommendations"),
      new Sibling("echoindustrialnexus", "Industrial material and production hooks"),
      new Sibling("echoblackboxprotocol", "Blackbox material hooks and late-game upgrade records")
   );

   private ArmoryOptionalIntegrations() {
   }

   public static void register() {
      List<String> loaded = loadedSiblingIds();
      EchoArmory.LOGGER.info("ECHO Armory optional integration readiness: {}/{} loaded. Active siblings: {}",
         loaded.size(), SIBLINGS.size(), loaded.isEmpty() ? "none" : String.join(", ", loaded));
   }

   public static List<String> loadedSiblingIds() {
      ArrayList<String> loaded = new ArrayList<>();
      for (Sibling sibling : SIBLINGS) {
         if (EchoRuntimeModules.isLoaded(sibling.modId())) {
            loaded.add(sibling.modId());
         }
      }
      return List.copyOf(loaded);
   }

   public static List<String> integrationSummaryLines() {
      return SIBLINGS.stream()
         .map(sibling -> sibling.modId() + ": " + (EchoRuntimeModules.isLoaded(sibling.modId()) ? "loaded" : "guarded") + " // " + sibling.coverage())
         .toList();
   }

   private record Sibling(String modId, String coverage) {
   }
}
