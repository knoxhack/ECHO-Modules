package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import net.minecraft.server.level.ServerPlayer;

public final class LogisticsDispatchBridge {
   private LogisticsDispatchBridge() {
   }

   public static DispatchResult requestNearestLoadout(ServerPlayer player, String loadoutId) {
      if (player == null) {
         return new DispatchResult(false, "No player context available.", "", "");
      }
      LogisticsTerminalSupport.RequestTarget target = LogisticsTerminalSupport.resolveRequestTarget(player, loadoutId);
      if (target == null) {
         return new DispatchResult(false, "No eligible Loadout Locker, Route Requester, or Auto-Restock Station in terminal range.", "", loadoutId == null ? "" : loadoutId);
      }
      boolean dispatched = LogisticsNetworkService.requestLoadout(player, target.target().getBlockPos(), target.target().getBlockPos(), target.preset().id().toString());
      target.target().refreshSnapshot(player);
      return new DispatchResult(
         dispatched,
         dispatched ? "Dispatch queued through Logistics network " + target.networkId() + "." : "Dispatch blocked. Check stock, dock, target type, and target capacity.",
         target.networkId(),
         target.preset().id().toString()
      );
   }

   public record DispatchResult(boolean dispatched, String message, String networkId, String loadoutId) {
   }
}
