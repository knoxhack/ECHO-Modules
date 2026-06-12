package com.knoxhack.echoopenlandsprotocol.contract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OpenlandsRuntimeEvidence(
        String id,
        String category,
        String description,
        String successCriteria,
        String failureAction,
        boolean requiredForPublicAlpha,
        List<String> runtimeTargets,
        List<String> checkedBy
) {
    public OpenlandsRuntimeEvidence {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(successCriteria, "successCriteria");
        Objects.requireNonNull(failureAction, "failureAction");
        runtimeTargets = List.copyOf(runtimeTargets == null ? List.of() : runtimeTargets);
        checkedBy = List.copyOf(checkedBy == null ? List.of() : checkedBy);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Openlands runtime evidence id cannot be blank");
        }
        if (runtimeTargets.isEmpty()) {
            throw new IllegalArgumentException("Openlands runtime evidence must name at least one runtime target");
        }
    }

    public Map<String, Object> asAdapterRecord() {
        return Map.of(
                "id", id,
                "category", category,
                "description", description,
                "successCriteria", successCriteria,
                "failureAction", failureAction,
                "requiredForPublicAlpha", requiredForPublicAlpha,
                "runtimeTargets", runtimeTargets,
                "checkedBy", checkedBy
        );
    }
}
