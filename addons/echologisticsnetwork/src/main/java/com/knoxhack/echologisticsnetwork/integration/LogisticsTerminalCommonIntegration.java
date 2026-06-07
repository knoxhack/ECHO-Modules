package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LogisticsTerminalCommonIntegration {
   private LogisticsTerminalCommonIntegration() {
   }

   public static void register() {
      TerminalActionRegistry.register(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.SCAN_ACTION,
         LogisticsTerminalCommonIntegration::scanNetwork);
      TerminalActionRegistry.register(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REQUEST_ACTION,
         (player, payload) -> requestSelectedLoadout(player, payload, "request"));
      TerminalActionRegistry.register(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.DISPATCH_ACTION,
         (player, payload) -> requestSelectedLoadout(player, payload, "dispatch"));
      TerminalActionRegistry.register(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.CLAIM_RELAY_ACTION,
         LogisticsTerminalCommonIntegration::claimRelay);
      TerminalActionRegistry.register(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REFRESH_OFFERS_ACTION,
         LogisticsTerminalCommonIntegration::refreshOffers);
      TerminalActionRegistry.register(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.CANCEL_ACTION,
         LogisticsTerminalCommonIntegration::cancelDeliveries);
      registerArchive();
      EchoLogisticsNetwork.LOGGER.info("ECHO Logistics Network terminal actions registered.");
   }

   private static void scanNetwork(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
      LogisticsNetworkService.LogisticsSnapshot snapshot = view.snapshot();
      long ready = snapshot.loadoutReadiness().stream().filter(LogisticsNetworkService.LoadoutReadiness::ready).count();
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Network " + view.networkId()
         + " | blocks " + view.blockCount()
         + " | endpoints " + view.endpointCount()
         + " | dock " + (view.dockOnline() ? "online" : "offline")
         + " | relay " + (view.relayOnline() ? "online" : "offline")
         + " | ready " + ready + "/" + snapshot.loadoutReadiness().size()
         + " | active drones " + snapshot.activeDeliveries()
         + " | factory restock " + snapshot.factoryRestock().message() + "."));
      if (!view.selectedLoadoutId().isBlank()) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Selected loadout: " + view.selectedLoadoutTitle()
            + (view.selectedReady() ? " ready for dispatch." : " missing " + view.selectedMissing() + " required item(s).")));
      }
      EchoCoreServices.discoverVisibleRouteRecords(player);
   }

   private static void requestSelectedLoadout(ServerPlayer player, String payload, String verb) {
      if (player == null) {
         return;
      }
      LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
      if ("dispatch".equals(verb) && !view.snapshot().canDispatch()) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Dispatch blocked. " + view.snapshot().dispatchBlockedReason()));
         return;
      }
      if ("request".equals(verb) && !view.snapshot().canRequest()) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Request blocked. " + view.snapshot().requestBlockedReason()));
         return;
      }
      LogisticsTerminalSupport.RequestTarget target = LogisticsTerminalSupport.resolveRequestTarget(player, payload);
      if (target == null) {
         String reason = view.snapshot().requestBlockedReason().isBlank()
            ? "No eligible Loadout Locker, Route Requester, or Auto-Restock Station in terminal range."
            : view.snapshot().requestBlockedReason();
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Request blocked. " + reason));
         return;
      }
      boolean dispatched = LogisticsNetworkService.requestLoadout(player, target.target().getBlockPos(), target.target().getBlockPos(), target.preset().id().toString());
      target.target().refreshSnapshot(player);
      if (!dispatched) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Terminal " + verb + " did not dispatch. Check stock, dock, target type, and target capacity."));
      }
   }

   private static void claimRelay(ServerPlayer player, String payload) {
      if (player != null) {
         LogisticsNetworkService.claimRelayRewards(player, LogisticsTerminalSupport.view(player).relay());
      }
   }

   private static void refreshOffers(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Faction offers " + view.snapshot().depotOffers().size()
         + " | depot " + (view.depotOnline() ? "online" : "offline")
         + (view.depotCooldown() > 0 ? " | cooldown " + ticks(view.depotCooldown()) + "." : ".")));
   }

   private static void cancelDeliveries(ServerPlayer player, String payload) {
      if (player == null) {
         return;
      }
      LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(player);
      int cancelled = LogisticsNetworkService.cancelActiveDeliveries(player, view.origin(), view.networkId());
      player.sendSystemMessage(Component.literal(cancelled > 0
         ? "ECHO LOGISTICS // Cancelled " + cancelled + " active delivery job(s); sealed payload recovery routed to dock."
         : "ECHO LOGISTICS // No cancellable delivery jobs found in network " + view.networkId() + "."));
   }

   private static void registerArchive() {
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(LogisticsTerminalIds.id("archive/supply_networks"),
         "Logistics Network", "Supply Networks", "ONLINE",
         List.of("Smart Storage Labels bind nearby inventories into supply categories.",
            "Loadout requests reserve items before courier drones launch, preventing duplicate delivery payloads."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(LogisticsTerminalIds.id("archive/courier_drones"),
         "Logistics Network", "Courier Drones", "ACTIVE",
         List.of("Courier drones use deterministic dock-to-target movement instead of expensive pathfinding.",
            "Failed deliveries return payloads to the dock or drop recoverable crates at the source."), false));
   }

   private static String ticks(int ticks) {
      int safeTicks = Math.max(0, ticks);
      if (safeTicks < 20) {
         return safeTicks + "t";
      }
      int seconds = Math.round(safeTicks / 20.0F);
      return safeTicks + "t (~" + seconds + "s)";
   }
}
