package com.knoxhack.echoconvoyprotocol.content;

import net.minecraft.resources.Identifier;

public record ConvoyIncidentDefinition(
   Identifier profileId,
   Identifier id,
   int stageIndex,
   String displayText,
   int readinessThreshold,
   int fuelEffect,
   int integrityEffect,
   int cargoEffect,
   int delayTicks,
   Identifier requiredResponseTask,
   String holomapMarkerHint
) {
   public ConvoyIncidentDefinition {
      if (profileId == null) {
         throw new IllegalArgumentException("Convoy incident profile id is required.");
      }
      id = id == null ? Identifier.fromNamespaceAndPath(profileId.getNamespace(), profileId.getPath() + "_incident") : id;
      stageIndex = Math.max(0, stageIndex);
      displayText = displayText == null || displayText.isBlank() ? "Convoy field incident detected." : displayText.strip();
      readinessThreshold = Math.max(0, Math.min(100, readinessThreshold));
      delayTicks = Math.max(0, delayTicks);
      requiredResponseTask = requiredResponseTask == null
         ? Identifier.fromNamespaceAndPath("echoconvoyprotocol", "resolve_field_incident")
         : requiredResponseTask;
      holomapMarkerHint = holomapMarkerHint == null || holomapMarkerHint.isBlank() ? "field_incident" : holomapMarkerHint.strip();
   }

   public boolean matchesStage(int stage) {
      return stageIndex == Math.max(0, stage);
   }
}
