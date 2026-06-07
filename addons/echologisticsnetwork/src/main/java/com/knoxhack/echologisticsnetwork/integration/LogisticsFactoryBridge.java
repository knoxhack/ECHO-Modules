package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class LogisticsFactoryBridge {
   private LogisticsFactoryBridge() {
   }

   public static DispatchResult requestFactoryLoadout(ServerPlayer player, BlockPos factoryControllerPos, String loadoutId) {
      LogisticsNetworkService.FactoryDispatchResult result =
         LogisticsNetworkService.requestFactoryLoadout(player, factoryControllerPos, loadoutId);
      return new DispatchResult(result.dispatched(), result.message(), result.networkId(), result.loadoutId(), result.targetPos());
   }

   public static RestockStatus factoryRestockStatus(ServerPlayer player, BlockPos factoryControllerPos, String loadoutId) {
      LogisticsNetworkService.FactoryRestockStatus status = player == null
         ? LogisticsNetworkService.FactoryRestockStatus.blocked("No server player context available.", "global", loadoutId, BlockPos.ZERO)
         : LogisticsNetworkService.factoryRestockStatus(player.level(), factoryControllerPos, null, loadoutId);
      return RestockStatus.from(status);
   }

   public static RestockStatus requestFactoryAutoRestock(ServerPlayer player, BlockPos factoryControllerPos, String loadoutId) {
      return RestockStatus.from(LogisticsNetworkService.requestFactoryAutoRestock(player, factoryControllerPos, loadoutId));
   }

   public record DispatchResult(boolean dispatched, String message, String networkId, String loadoutId, BlockPos targetPos) {
   }

   public record RestockStatus(boolean eligible, boolean dispatched, String message, String networkId, String loadoutId,
                               BlockPos targetPos, int currentRuns, int targetRuns, int minRuns, int inFlightRuns,
                               int cooldownTicks) {
      private static RestockStatus from(LogisticsNetworkService.FactoryRestockStatus status) {
         return new RestockStatus(status.eligible(), status.dispatched(), status.message(), status.networkId(),
            status.loadoutId(), status.targetPos(), status.currentRuns(), status.targetRuns(), status.minRuns(),
            status.inFlightRuns(), status.cooldownTicks());
      }
   }
}
