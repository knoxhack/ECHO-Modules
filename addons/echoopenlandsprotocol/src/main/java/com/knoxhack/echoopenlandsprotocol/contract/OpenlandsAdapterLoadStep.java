package com.knoxhack.echoopenlandsprotocol.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OpenlandsAdapterLoadStep(
        String id,
        OpenlandsAdapterLoadPhase phase,
        String summary,
        List<String> resourceIds,
        List<String> requiredEvidence,
        List<String> runtimeTargets,
        String successSignal,
        boolean blocksRelease
) {
    public OpenlandsAdapterLoadStep {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(successSignal, "successSignal");
        resourceIds = List.copyOf(resourceIds == null ? List.of() : resourceIds);
        requiredEvidence = List.copyOf(requiredEvidence == null ? List.of() : requiredEvidence);
        runtimeTargets = List.copyOf(runtimeTargets == null ? List.of() : runtimeTargets);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Openlands adapter load step id cannot be blank");
        }
        if (runtimeTargets.isEmpty()) {
            throw new IllegalArgumentException("Openlands adapter load step must name at least one runtime target");
        }
    }

    public Map<String, Object> asAdapterRecord() {
        return Map.of(
                "id", id,
                "phase", phase.id(),
                "phaseOrder", phase.order(),
                "summary", summary,
                "resourceIds", resourceIds,
                "requiredEvidence", requiredEvidence,
                "runtimeTargets", runtimeTargets,
                "successSignal", successSignal,
                "blocksRelease", blocksRelease
        );
    }
}
