package com.knoxhack.echoruntimeguard.api;

public record RuntimeMetricsSnapshot(
        double currentTps,
        double averageTps,
        double currentMspt,
        double averageMspt,
        double worstMsptLastMinute,
        int lagSpikesLastMinute,
        long sampledTicks,
        RuntimeMode mode,
        boolean emergency,
        String loadedChunks,
        String entityCount,
        String blockEntityCount,
        int players) {
    public static RuntimeMetricsSnapshot unavailable(RuntimeMode mode, boolean emergency) {
        return new RuntimeMetricsSnapshot(20.0D, 20.0D, 0.0D, 0.0D, 0.0D, 0, 0L,
                mode, emergency, "unavailable", "unavailable", "unavailable", 0);
    }
}
