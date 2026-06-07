package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.RuntimeMetricsSnapshot;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class RuntimeProfilerService {
    public static final RuntimeProfilerService INSTANCE = new RuntimeProfilerService();
    private static final int WINDOW_TICKS = 20 * 60;
    private final Deque<TickSample> samples = new ArrayDeque<>();
    private long tickStartNanos;
    private long sampledTicks;
    private double currentMspt;
    private double currentTps = 20.0D;
    private RuntimeMetricsSnapshot lastSnapshot = RuntimeMetricsSnapshot.unavailable(RuntimeModeService.INSTANCE.mode(), false);

    private RuntimeProfilerService() {
    }

    public synchronized void onServerTickPre(Object event) {
        tickStartNanos = System.nanoTime();
    }

    public synchronized void onServerTickPost(Object event) {
        long now = System.nanoTime();
        if (tickStartNanos <= 0L) {
            tickStartNanos = now;
        }
        currentMspt = Math.max(0.0D, (now - tickStartNanos) / 1_000_000.0D);
        currentTps = currentMspt <= 0.0D ? 20.0D : Math.min(20.0D, 1000.0D / currentMspt);
        sampledTicks++;
        boolean spike = currentMspt >= 50.0D || currentTps <= RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.WARNING_TPS, 18.0D);
        samples.addLast(new TickSample(sampledTicks, currentMspt, currentTps, spike));
        while (samples.size() > WINDOW_TICKS) {
            samples.removeFirst();
        }
        if (spike) {
            LagSpikeReporter.INSTANCE.record(currentMspt, currentTps, "server tick exceeded RuntimeGuard warning threshold");
        }
        lastSnapshot = snapshot(EchoBackendWorldEventBridge.serverTickServer(event));
        RuntimeModeService.INSTANCE.tickServer(lastSnapshot);
    }

    public synchronized RuntimeMetricsSnapshot snapshot(MinecraftServer server) {
        double totalMspt = 0.0D;
        double totalTps = 0.0D;
        double worstMspt = 0.0D;
        int spikes = 0;
        for (TickSample sample : samples) {
            totalMspt += sample.mspt();
            totalTps += sample.tps();
            worstMspt = Math.max(worstMspt, sample.mspt());
            if (sample.spike()) {
                spikes++;
            }
        }
        int count = Math.max(1, samples.size());
        int players = server == null ? 0 : server.getPlayerCount();
        String loadedChunks = loadedChunkCount(server);
        String entityCount = entityCount(server);
        String blockEntityCount = blockEntityCount(server);
        return new RuntimeMetricsSnapshot(
                currentTps,
                totalTps / count,
                currentMspt,
                totalMspt / count,
                worstMspt,
                spikes,
                sampledTicks,
                RuntimeModeService.INSTANCE.mode(),
                RuntimeModeService.INSTANCE.isEmergency(),
                loadedChunks,
                entityCount,
                blockEntityCount,
                players);
    }

    public synchronized RuntimeMetricsSnapshot lastSnapshot() {
        return lastSnapshot;
    }

    public synchronized long serverTick() {
        return sampledTicks;
    }

    public synchronized void reset() {
        samples.clear();
        tickStartNanos = 0L;
        sampledTicks = 0L;
        currentMspt = 0.0D;
        currentTps = 20.0D;
        lastSnapshot = RuntimeMetricsSnapshot.unavailable(RuntimeModeService.INSTANCE.mode(), RuntimeModeService.INSTANCE.isEmergency());
    }

    private static String loadedChunkCount(MinecraftServer server) {
        if (server == null) {
            return "unavailable";
        }
        try {
            int loaded = 0;
            for (ServerLevel level : server.getAllLevels()) {
                loaded += level.getChunkSource().getLoadedChunksCount();
            }
            return Integer.toString(loaded);
        } catch (RuntimeException exception) {
            return "unavailable";
        }
    }

    private static String entityCount(MinecraftServer server) {
        if (server == null) {
            return "unavailable";
        }
        try {
            int count = 0;
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity ignored : level.getAllEntities()) {
                    count++;
                }
            }
            return Integer.toString(count);
        } catch (RuntimeException exception) {
            return "unavailable";
        }
    }

    private static String blockEntityCount(MinecraftServer server) {
        if (server == null) {
            return "unavailable";
        }
        try {
            int count = 0;
            for (ServerLevel level : server.getAllLevels()) {
                count += BlockEntityTickerCounter.count(level);
            }
            return Integer.toString(count);
        } catch (RuntimeException exception) {
            return "unavailable";
        }
    }

    private static final class BlockEntityTickerCounter {
        private static final java.lang.reflect.Field FIELD = resolve();

        private static int count(ServerLevel level) {
            if (FIELD == null || level == null) {
                return 0;
            }
            try {
                Object value = FIELD.get(level);
                return value instanceof java.util.Collection<?> collection ? collection.size() : 0;
            } catch (IllegalAccessException | RuntimeException exception) {
                return 0;
            }
        }

        private static java.lang.reflect.Field resolve() {
            try {
                java.lang.reflect.Field field = net.minecraft.world.level.Level.class.getDeclaredField("blockEntityTickers");
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return null;
            }
        }
    }

    private record TickSample(long tick, double mspt, double tps, boolean spike) {
    }
}
