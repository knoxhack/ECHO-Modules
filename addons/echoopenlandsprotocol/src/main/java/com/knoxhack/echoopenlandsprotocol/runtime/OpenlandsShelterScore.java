package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.List;
import java.util.Map;

public record OpenlandsShelterScore(
        int total,
        boolean sleepMilestoneAllowed,
        Map<String, Integer> componentScores,
        List<String> missingImprovements
) {
    public OpenlandsShelterScore {
        total = Math.max(0, Math.min(100, total));
        componentScores = Map.copyOf(componentScores);
        missingImprovements = List.copyOf(missingImprovements);
    }
}
