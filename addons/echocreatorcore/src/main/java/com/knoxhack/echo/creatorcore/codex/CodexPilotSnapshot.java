package com.knoxhack.echo.creatorcore.codex;

import java.util.List;

public record CodexPilotSnapshot(
        boolean enabled,
        boolean spawned,
        boolean paused,
        boolean autopilotAllowed,
        boolean worldActionsAllowed,
        String profile,
        String label,
        String dimension,
        String position,
        String lastMessage,
        int pendingActions,
        List<String> recentEvents) {
    public CodexPilotSnapshot {
        profile = safe(profile);
        label = safe(label);
        dimension = safe(dimension);
        position = safe(position);
        lastMessage = safe(lastMessage);
        recentEvents = recentEvents == null ? List.of() : List.copyOf(recentEvents);
    }

    public static CodexPilotSnapshot empty(String message) {
        return new CodexPilotSnapshot(false, false, false, false, false,
                "", "", "", "{}", message, 0, List.of());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
