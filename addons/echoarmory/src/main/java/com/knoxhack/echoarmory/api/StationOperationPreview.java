package com.knoxhack.echoarmory.api;

public record StationOperationPreview(
   String stationKind,
   boolean accepted,
   String operation,
   String blocker,
   String result,
   String readinessImpact,
   int fuelCount,
   int energy,
   int moduleCount
) {
   public StationOperationPreview {
      stationKind = stationKind == null || stationKind.isBlank() ? "armory_bench" : stationKind.strip();
      operation = operation == null || operation.isBlank() ? "inspect" : operation.strip();
      blocker = blocker == null ? "" : blocker.strip();
      result = result == null ? "" : result.strip();
      readinessImpact = readinessImpact == null ? "" : readinessImpact.strip();
      fuelCount = Math.max(0, fuelCount);
      energy = Math.max(0, energy);
      moduleCount = Math.max(0, moduleCount);
   }

   public static StationOperationPreview blocked(String stationKind, String operation, String blocker) {
      return new StationOperationPreview(stationKind, false, operation, blocker, "", "", 0, 0, 0);
   }
}
