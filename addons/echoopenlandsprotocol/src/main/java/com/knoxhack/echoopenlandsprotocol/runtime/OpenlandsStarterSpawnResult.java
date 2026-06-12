package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.List;

public record OpenlandsStarterSpawnResult(
        boolean accepted,
        List<String> satisfied,
        List<String> missing,
        String failurePolicy
) {
    public OpenlandsStarterSpawnResult {
        satisfied = List.copyOf(satisfied);
        missing = List.copyOf(missing);
    }
}
