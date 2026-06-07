package com.knoxhack.echoconvoyprotocol.content;

import com.knoxhack.echoconvoyprotocol.Config;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record ConvoyIncidentProfile(Identifier id, List<ConvoyIncidentDefinition> incidents) {
   public ConvoyIncidentProfile {
      if (id == null) {
         throw new IllegalArgumentException("Convoy incident profile id is required.");
      }
      incidents = List.copyOf(incidents == null ? List.of() : incidents);
   }

   public Optional<ConvoyIncidentDefinition> firstBlockedIncident(int stage, int operationScore, Set<Identifier> resolvedIncidents) {
      int frequency = Config.incidentFrequencyPercent();
      if (frequency <= 0) {
         return Optional.empty();
      }
      Set<Identifier> resolved = resolvedIncidents == null ? Set.of() : resolvedIncidents;
      return incidents.stream()
         .filter(incident -> incident.matchesStage(stage))
         .filter(incident -> operationScore < incident.readinessThreshold())
         .filter(incident -> !resolved.contains(incident.id()))
         .filter(incident -> frequency >= 100 || Math.floorMod(incident.id().toString().hashCode() + stage, 100) < frequency)
         .findFirst();
   }
}
