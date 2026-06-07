package com.knoxhack.echoruntimeguard.api;

import net.minecraft.resources.Identifier;

public record ProfilerEntry(Identifier id, long calls, long totalNanos, long maxNanos) {
    public double totalMillis() {
        return totalNanos / 1_000_000.0D;
    }

    public double averageMillis() {
        return calls <= 0L ? 0.0D : (totalNanos / 1_000_000.0D) / calls;
    }

    public double maxMillis() {
        return maxNanos / 1_000_000.0D;
    }
}
