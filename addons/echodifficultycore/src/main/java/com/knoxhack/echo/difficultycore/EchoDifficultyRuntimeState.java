package com.knoxhack.echo.difficultycore;

import java.util.Map;

public final class EchoDifficultyRuntimeState {
    private static volatile EchoServerDifficultyPolicy activeServerPolicy;

    private EchoDifficultyRuntimeState() {
    }

    public static EchoServerDifficultyPolicy activeServerPolicy() {
        return activeServerPolicy;
    }

    public static EchoServerDifficultyPolicy materializeServerPolicy(
            String policyId,
            EchoDifficultyProfileId forcedProfile,
            String serverClassName
    ) {
        EchoServerDifficultyPolicy policy = new EchoServerDifficultyPolicy(
                policyId,
                forcedProfile,
                true,
                true,
                false,
                "Server difficulty policy active",
                "DifficultyCore resolved a server-authoritative profile during the live server-start hook.",
                Map.of(
                        "source", "server.starting",
                        "serverClass", serverClassName == null || serverClassName.isBlank() ? "unknown" : serverClassName
                )
        );
        activeServerPolicy = policy;
        return policy;
    }
}
