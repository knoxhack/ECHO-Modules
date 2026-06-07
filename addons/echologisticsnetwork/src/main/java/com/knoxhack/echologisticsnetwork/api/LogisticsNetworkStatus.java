package com.knoxhack.echologisticsnetwork.api;

import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;

/**
 * Small immutable status view for optional sibling addons that need Logistics readiness
 * without reflecting through the full dashboard snapshot.
 */
public record LogisticsNetworkStatus(
   String networkId,
   int blockCount,
   int endpointCount,
   boolean dockOnline,
   boolean relayOnline,
   boolean depotOnline,
   int activeDeliveries,
   String selectedLoadoutId,
   String selectedLoadoutTitle,
   boolean selectedReady,
   int selectedMissing,
   String factoryRestockMessage
) {
   public LogisticsNetworkStatus {
      networkId = networkId == null || networkId.isBlank() ? "global" : networkId;
      blockCount = Math.max(0, blockCount);
      endpointCount = Math.max(0, endpointCount);
      activeDeliveries = Math.max(0, activeDeliveries);
      selectedLoadoutId = selectedLoadoutId == null ? "" : selectedLoadoutId;
      selectedLoadoutTitle = selectedLoadoutTitle == null || selectedLoadoutTitle.isBlank() ? "None" : selectedLoadoutTitle;
      selectedMissing = Math.max(0, selectedMissing);
      factoryRestockMessage = factoryRestockMessage == null || factoryRestockMessage.isBlank()
         ? "No factory restock status."
         : factoryRestockMessage.strip();
   }

   public static LogisticsNetworkStatus from(LogisticsNetworkService.LogisticsSnapshot snapshot) {
      if (snapshot == null) {
         return empty("global");
      }
      return new LogisticsNetworkStatus(
         snapshot.networkId(),
         snapshot.blockCount(),
         snapshot.endpointCount(),
         snapshot.dockOnline(),
         snapshot.relayOnline(),
         snapshot.depotOnline(),
         snapshot.activeDeliveries(),
         snapshot.selectedLoadoutId(),
         snapshot.selectedLoadoutTitle(),
         snapshot.selectedReady(),
         snapshot.selectedMissing(),
         snapshot.factoryRestock().message()
      );
   }

   public static LogisticsNetworkStatus empty(String networkId) {
      return new LogisticsNetworkStatus(networkId, 0, 0, false, false, false, 0, "", "None", false, 0, "No factory restock status.");
   }
}
