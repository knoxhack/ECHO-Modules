package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.Map;

public record OpenlandsWaystoneTransition(
        OpenlandsWaystoneState before,
        OpenlandsWaystoneState after,
        boolean accepted,
        String reason,
        Map<String, Integer> consumedInputs
) {
    public OpenlandsWaystoneTransition {
        consumedInputs = Map.copyOf(consumedInputs);
    }

    public static OpenlandsWaystoneTransition accepted(
            OpenlandsWaystoneState before,
            OpenlandsWaystoneState after,
            Map<String, Integer> consumedInputs
    ) {
        return new OpenlandsWaystoneTransition(before, after, true, "accepted", consumedInputs);
    }

    public static OpenlandsWaystoneTransition rejected(OpenlandsWaystoneState before, String reason) {
        return new OpenlandsWaystoneTransition(before, before, false, reason, Map.of());
    }
}
