package com.knoxhack.echoruntimeguard.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

public final class RuntimeGuardProfiler {
    private static final Map<Identifier, MutableEntry> COSTS = new ConcurrentHashMap<>();

    private RuntimeGuardProfiler() {
    }

    public static void time(Identifier id, Runnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            record(id, System.nanoTime() - start);
        }
    }

    public static <T> T timeSupplier(Identifier id, Supplier<T> supplier) {
        long start = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            record(id, System.nanoTime() - start);
        }
    }

    public static void record(Identifier id, long nanos) {
        Identifier safeId = id == null ? Identifier.fromNamespaceAndPath("echoruntimeguard", "unknown") : id;
        COSTS.computeIfAbsent(safeId, ignored -> new MutableEntry()).record(Math.max(0L, nanos));
    }

    public static List<ProfilerEntry> getTopCosts() {
        return COSTS.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparingLong(ProfilerEntry::totalNanos).reversed())
                .limit(16)
                .toList();
    }

    public static void reset() {
        COSTS.clear();
    }

    private static final class MutableEntry {
        private long calls;
        private long totalNanos;
        private long maxNanos;

        synchronized void record(long nanos) {
            calls++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }

        synchronized ProfilerEntry snapshot(Identifier id) {
            return new ProfilerEntry(id, calls, totalNanos, maxNanos);
        }
    }
}
