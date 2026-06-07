package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoRouteRecordService;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService.LogisticsSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class LogisticsCoreIntegration {
   public static final String CHAPTER_ID = "logistics_network";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final EchoRouteRecordService ROUTE_SERVICE = LogisticsCoreIntegration::routeRecords;
   private static final EchoDiagnosticService DIAGNOSTIC_SERVICE = LogisticsCoreIntegration::diagnostics;

   private LogisticsCoreIntegration() {
   }

   public static void registerAddonChapter() {
      if (REGISTERED.compareAndSet(false, true) && !EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
         EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
               return CHAPTER_ID;
            }

            @Override
            public String modId() {
               return EchoLogisticsNetwork.MODID;
            }

            @Override
            public String displayName() {
               return "ECHO: Logistics Network";
            }

            @Override
            public String summary() {
               return "Supply categories, expedition loadouts, courier drone delivery, depot exchanges, and reward relay support.";
            }

            @Override
            public boolean isAvailable(Player player) {
               return player != null;
            }

            @Override
            public String statusLine(Player player) {
               if (player == null) {
                  return "LOGISTICS: telemetry offline until player context is available.";
               }
               LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
               LogisticsSnapshot snapshot = view.snapshot();
               return "LOGISTICS: network " + view.networkId() + ", dock " + (view.dockOnline() ? "online" : "offline")
                  + ", " + snapshot.stockRows().size() + " categories, " + snapshot.missingRows().size()
                  + " low-stock alerts, " + snapshot.activeDeliveries() + " active drones.";
            }
         });
      }
      EchoCoreServices.registerRouteRecordService(ROUTE_SERVICE);
      EchoCoreServices.registerDiagnosticService(DIAGNOSTIC_SERVICE);
      EchoCoreServices.registerIndexContentProvider(LogisticsIndexProvider.INSTANCE);
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      if (player == null) {
         return List.of();
      }
      LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
      LogisticsSnapshot snapshot = view.snapshot();
      List<EchoRouteRecord> records = new ArrayList<>();
      boolean networkOnline = view.blockCount() > 0;
      boolean dispatchInfrastructureReady = networkOnline && view.dockOnline() && view.endpointCount() > 0;
      records.add(new EchoRouteRecord(
         LogisticsTerminalIds.id("route/network_" + safePath(view.networkId())),
         CHAPTER_ID,
         "Logistics Network: " + view.networkId(),
         "Logistics",
         "Base network",
         networkOnline ? view.dockOnline() ? "ONLINE" : "DOCK OFFLINE" : "NETWORK OFFLINE",
         "Blocks " + view.blockCount() + ", request endpoints " + view.endpointCount()
            + ", active drones " + snapshot.activeDeliveries()
            + ", low stock rows " + snapshot.missingRows().size() + ".",
         dispatchInfrastructureReady
      ));
      snapshot.loadoutReadiness().forEach(readiness -> records.add(new EchoRouteRecord(
         LogisticsTerminalIds.id("route/" + readiness.presetId().getPath()),
         CHAPTER_ID,
         readiness.title(),
         "Logistics",
         "Network " + view.networkId(),
         routeStatus(view, readiness),
         routeSummary(view, readiness),
         readiness.ready() && dispatchInfrastructureReady
      )));
      return records;
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      }
      LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
      LogisticsSnapshot snapshot = view.snapshot();
      List<EchoDiagnosticBlocker> blockers = new ArrayList<>();
      if (view.blockCount() <= 0) {
         blockers.add(new EchoDiagnosticBlocker(
            LogisticsTerminalIds.id("diagnostic/no_network"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "No logistics network nearby",
            "ECHO Core cannot see a Logistics block near the operator.",
            "Place a Logistics Terminal, Supply Crate, Route Requester, or manifest a nearby block onto a network."
         ));
      } else if (!view.dockOnline()) {
         blockers.add(new EchoDiagnosticBlocker(
            LogisticsTerminalIds.id("diagnostic/offline_dock"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.WARNING,
            "Drone dock offline",
            "Network " + view.networkId() + " has supplies, but no Drone Delivery Dock is online in logistics range.",
            "Place a Drone Delivery Dock on the same Route Manifest network."
         ));
      }
      if (view.blockCount() > 0 && view.endpointCount() <= 0) {
         blockers.add(new EchoDiagnosticBlocker(
            LogisticsTerminalIds.id("diagnostic/no_request_endpoint"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.WARNING,
            "No request endpoint",
            "Network " + view.networkId() + " has no Loadout Locker, Route Requester, or Auto-Restock Station in terminal range.",
            "Add one request endpoint and assign a Loadout Card."
         ));
      }
      int pendingRewards = LogisticsNetworkService.pendingRelayRewards(player);
      if (pendingRewards > 0 && !view.relayOnline()) {
         blockers.add(new EchoDiagnosticBlocker(
            LogisticsTerminalIds.id("diagnostic/reward_relay_offline"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Relay rewards waiting",
            pendingRewards + " terminal reward item(s) are pending, but no Remote Reward Relay is online in this logistics network.",
            "Place a Remote Reward Relay on the network before claiming through Logistics."
         ));
      }
      snapshot.missingRows().stream().limit(3).forEach(row -> blockers.add(new EchoDiagnosticBlocker(
            LogisticsTerminalIds.id("diagnostic/" + row.categoryId().getPath()),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Low " + row.title(),
            "Logistics stock is below the configured low-stock target.",
            "Label storage with Supply Tags or restock " + row.missing() + " more."
         )));
      boolean anyReady = snapshot.loadoutReadiness().stream().anyMatch(LogisticsNetworkService.LoadoutReadiness::ready);
      if (!anyReady && !LogisticsContent.loadouts().isEmpty()) {
         blockers.add(new EchoDiagnosticBlocker(
            LogisticsTerminalIds.id("diagnostic/no_ready_loadout"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "No loadout ready",
            "The logistics network is online, but no expedition preset has all required supplies.",
            "Scan the Logistics page and fill the missing categories."
         ));
      }
      return List.copyOf(blockers);
   }

   private static String routeStatus(LogisticsTerminalSupport.TerminalView view, LogisticsNetworkService.LoadoutReadiness readiness) {
      if (view.blockCount() <= 0) {
         return "NETWORK OFFLINE";
      }
      if (!readiness.ready()) {
         return "MISSING " + readiness.missingCount();
      }
      if (!view.dockOnline()) {
         return "DOCK OFFLINE";
      }
      if (view.endpointCount() <= 0) {
         return "TARGET OFFLINE";
      }
      if (view.snapshot().activeDeliveries() > 0) {
         return "READY / IN TRANSIT";
      }
      return "READY";
   }

   private static String routeSummary(LogisticsTerminalSupport.TerminalView view, LogisticsNetworkService.LoadoutReadiness readiness) {
      if (!readiness.ready()) {
         return "Missing " + readiness.missingCount() + " required supply items on network " + view.networkId() + ".";
      }
      if (!view.dockOnline()) {
         return "Supplies are ready, but delivery is blocked until a Drone Delivery Dock is online.";
      }
      if (view.endpointCount() <= 0) {
         return "Supplies are ready, but no request endpoint can receive this loadout.";
      }
      return "Loadout can dispatch from network " + view.networkId() + " through "
         + view.endpointCount() + " endpoint(s); active drones " + view.snapshot().activeDeliveries() + ".";
   }

   private static String safePath(String value) {
      String cleaned = value == null ? "" : value.toLowerCase(Locale.ROOT)
         .replaceAll("[^a-z0-9_\\-/.]+", "_")
         .replace('/', '_')
         .replace('.', '_')
         .replaceAll("_+", "_")
         .replaceAll("^_|_$", "");
      return cleaned.isBlank() ? "global" : cleaned;
   }
}
