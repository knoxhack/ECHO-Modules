package com.knoxhack.echoruntimeguard.api;

public record ClientMetricsSnapshot(
        int currentFps,
        double averageFps,
        double frameTimeMs,
        boolean visualEmergency,
        RuntimeMode mode) {
    public static ClientMetricsSnapshot unavailable(RuntimeMode mode) {
        return new ClientMetricsSnapshot(-1, -1.0D, -1.0D, false, mode);
    }
}
