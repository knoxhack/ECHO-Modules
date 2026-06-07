package com.knoxhack.echo.progressioncore;

import java.util.Map;
import java.util.Objects;

public record EchoObjectiveProgress(
        EchoObjectiveId objectiveId,
        EchoObjectiveScope scope,
        String subjectId,
        int current,
        int required,
        boolean revealed,
        boolean completed,
        Map<String, String> attributes
) {
    public EchoObjectiveProgress {
        Objects.requireNonNull(objectiveId, "objectiveId");
        scope = scope == null ? EchoObjectiveScope.PLAYER : scope;
        subjectId = ProgressionContractGuards.optionalText(subjectId);
        current = ProgressionContractGuards.nonNegative(current, "objective current progress");
        required = ProgressionContractGuards.positiveOrOne(required);
        completed = completed || current >= required;
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public static EchoObjectiveProgress empty(EchoObjectiveId objectiveId, int required) {
        return new EchoObjectiveProgress(objectiveId, EchoObjectiveScope.PLAYER, "", 0, required, false, false, Map.of());
    }

    public double percent() {
        return ProgressionContractGuards.boundedPercent((double) current / (double) required);
    }
}
