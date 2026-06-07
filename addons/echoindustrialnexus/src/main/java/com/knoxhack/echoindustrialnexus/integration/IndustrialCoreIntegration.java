package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoRouteRecordService;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class IndustrialCoreIntegration {
   public static final String CHAPTER_ID = "industrial_nexus";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final EchoRouteRecordService ROUTE_SERVICE = IndustrialCoreIntegration::routeRecords;
   private static final EchoDiagnosticService DIAGNOSTIC_SERVICE = IndustrialCoreIntegration::diagnostics;

   private IndustrialCoreIntegration() {
   }

   public static void registerAddonChapter() {
      if (REGISTERED.compareAndSet(false, true) && !EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
         EchoAddonRegistry.register(new EchoAddonChapter() {
            public String id() {
               return CHAPTER_ID;
            }

            public String modId() {
               return EchoIndustrialNexus.MODID;
            }

            public String displayName() {
               return "ECHO: Industrial Nexus";
            }

            public String summary() {
               return "Industrial automation chapter for Thermal Flux, factory recovery, scrubber support, and Furnace Warden production survival.";
            }

            public boolean isAvailable(Player player) {
               return player != null;
            }

            public String statusLine(Player player) {
               if (player == null) {
                  return "INDUSTRIAL: telemetry offline until player context is available.";
               }
               int flux = IndustrialProgress.value(player, "thermal_flux_generated");
               int machines = IndustrialProgress.value(player, "machines");
               boolean safeZone = IndustrialProgress.flag(player, "safe_zone");
               boolean wardenDefeated = IndustrialProgress.flag(player, "furnace_warden_defeated");
               return "INDUSTRIAL: " + flux + " TF, " + machines + " machines, safe zone "
                  + (safeZone ? "online" : "pending") + ", Warden " + (wardenDefeated ? "defeated" : "active") + ".";
            }
         });
      }

      EchoCoreServices.registerRouteRecordService(ROUTE_SERVICE);
      EchoCoreServices.registerDiagnosticService(DIAGNOSTIC_SERVICE);
      EchoIndustrialNexus.LOGGER.info("ECHO platform providers after Industrial setup: {}", EchoCoreServices.platformProviderSummary());
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      List<EchoRouteRecord> records = new ArrayList<>();
      for (IndustrialMissionProvider.RouteMission mission : IndustrialMissionProvider.routeMissions()) {
         float progress = progress(player, mission);
         boolean claimed = claimed(player, mission);
         boolean complete = progress >= 1.0F;
         records.add(new EchoRouteRecord(
            routeId(mission),
            CHAPTER_ID,
            mission.title(),
            mission.category(),
            "Overworld / Industrial POI",
            claimed ? "CLAIMED" : complete ? "CACHE READY" : progress > 0.0F ? "ACTIVE" : "PENDING",
            routeSummary(mission, progress, claimed, complete),
            complete || claimed
         ));
      }
      return List.copyOf(records);
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      }
      for (IndustrialMissionProvider.RouteMission mission : IndustrialMissionProvider.routeMissions()) {
         float progress = progress(player, mission);
         boolean claimed = IndustrialProgress.claimed(player, mission.key());
         if (progress < 1.0F && !claimed) {
            EchoDiagnosticBlocker.Severity severity = progress > 0.0F
               ? EchoDiagnosticBlocker.Severity.INFO
               : EchoDiagnosticBlocker.Severity.BLOCKED;
            return List.of(new EchoDiagnosticBlocker(
               diagnosticId(mission),
               CHAPTER_ID,
               severity,
               mission.title(),
               mission.briefing(),
               mission.nextAction()
            ));
         }
      }
      return List.of();
   }

   private static float progress(Player player, IndustrialMissionProvider.RouteMission mission) {
      if (player == null) {
         return 0.0F;
      }
      try {
         return IndustrialProgress.progress(player, mission.key());
      } catch (RuntimeException exception) {
         EchoIndustrialNexus.LOGGER.debug("Industrial route progress fallback for {}.", mission.key(), exception);
         return 0.0F;
      }
   }

   private static boolean claimed(Player player, IndustrialMissionProvider.RouteMission mission) {
      if (player == null) {
         return false;
      }
      try {
         return IndustrialProgress.claimed(player, mission.key());
      } catch (RuntimeException exception) {
         EchoIndustrialNexus.LOGGER.debug("Industrial route claim fallback for {}.", mission.key(), exception);
         return false;
      }
   }

   private static String routeSummary(
      IndustrialMissionProvider.RouteMission mission, float progress, boolean claimed, boolean complete
   ) {
      if (claimed) {
         return "Industrial support cache claimed. " + mission.briefing();
      }
      if (complete) {
         return "Industrial objective complete. Claim the support cache from the Industrial Nexus terminal.";
      }
      int percent = Math.max(0, Math.min(100, Math.round(progress * 100.0F)));
      return percent + "% complete. " + mission.briefing();
   }

   private static Identifier routeId(IndustrialMissionProvider.RouteMission mission) {
      return IndustrialTerminalIds.id("route/" + mission.key());
   }

   private static Identifier diagnosticId(IndustrialMissionProvider.RouteMission mission) {
      return IndustrialTerminalIds.id("diagnostic/" + mission.key());
   }
}
