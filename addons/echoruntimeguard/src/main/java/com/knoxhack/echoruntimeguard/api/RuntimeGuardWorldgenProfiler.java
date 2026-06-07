package com.knoxhack.echoruntimeguard.api;

import com.knoxhack.echoruntimeguard.EchoRuntimeGuard;
import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

public final class RuntimeGuardWorldgenProfiler {
    private RuntimeGuardWorldgenProfiler() {
    }

    public static void profileFeature(Identifier id, ChunkPos chunkPos, Runnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            long nanos = System.nanoTime() - start;
            RuntimeGuardProfiler.record(id, nanos);
            long millis = nanos / 1_000_000L;
            if (RuntimeGuardConfig.safeBool(RuntimeGuardConfig.WORLDGEN_PROFILER_ENABLED, true)
                    && millis >= RuntimeGuardConfig.safeInt(RuntimeGuardConfig.WARN_FEATURE_GEN_MS, 50)) {
                EchoRuntimeGuard.LOGGER.warn("RuntimeGuard worldgen feature {} took {}ms at {}.",
                        id, millis, chunkPos == null ? "unknown chunk" : chunkPos);
            }
        }
    }
}
