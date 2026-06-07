package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import com.knoxhack.echoruntimeguard.api.RuntimeWorkType;
import com.knoxhack.echoruntimeguard.api.TickPriority;
import com.knoxhack.echoruntimeguard.api.WakeReason;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class SmartTickService {
    public static final SmartTickService INSTANCE = new SmartTickService();
    private final Map<String, ActivityRecord> activity = new ConcurrentHashMap<>();

    private SmartTickService() {
    }

    public boolean shouldRun(String systemId, BlockPos pos, TickPriority priority) {
        if (priority == null || priority.alwaysRun() || !RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.SMART_TICK_ENABLED, true)) {
            return true;
        }
        int rate = priority == TickPriority.DECORATIVE ? RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_TICK_RATE, 60)
                : RuntimeGuardConfig.safeInt(RuntimeGuardConfig.NEARBY_IDLE_TICK_RATE, 20);
        if (RuntimeModeService.INSTANCE.mode() == RuntimeMode.EMERGENCY && priority.nonCritical()) {
            rate *= 4;
        }
        return RuntimeProfilerService.INSTANCE.serverTick() % Math.max(1, rate) == 0L;
    }

    public boolean shouldRun(String systemId, Level level, BlockPos pos, TickPriority priority) {
        if (priority == null || priority.alwaysRun()) {
            return true;
        }
        int rate = getRecommendedTickRate(level, pos, RuntimeWorkType.BLOCK_ENTITY);
        if (priority == TickPriority.ACTIVE_MACHINE) {
            rate = Math.min(rate, RuntimeGuardConfig.safeInt(RuntimeGuardConfig.NEARBY_IDLE_TICK_RATE, 20));
        } else if (priority == TickPriority.DECORATIVE) {
            rate = Math.max(rate, RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_TICK_RATE, 60));
        }
        return RuntimeProfilerService.INSTANCE.serverTick() % Math.max(1, rate) == 0L;
    }

    public boolean shouldSkipNonCriticalTick(Level level, BlockPos pos) {
        return !shouldRun("echoruntimeguard:non_critical", level, pos, TickPriority.BACKGROUND);
    }

    public int getRecommendedTickRate(Level level, BlockPos pos, RuntimeWorkType type) {
        if (!RuntimeGuardConfig.enabled() || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.SMART_TICK_ENABLED, true)) {
            return 1;
        }
        int nearby = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.NEARBY_ACTIVE_TICK_RATE, 1);
        int idle = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.NEARBY_IDLE_TICK_RATE, 20);
        int far = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_TICK_RATE, 60);
        int veryFar = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.VERY_FAR_TICK_RATE, 200);
        double distanceSqr = nearestPlayerDistanceSqr(level, pos);
        int farDistance = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_DISTANCE_BLOCKS, 64);
        int veryFarDistance = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.VERY_FAR_DISTANCE_BLOCKS, 128);
        int rate;
        if (distanceSqr <= farDistance * farDistance) {
            rate = type == RuntimeWorkType.PARTICLE || type == RuntimeWorkType.NETWORK_SYNC ? nearby : idle;
        } else if (distanceSqr <= veryFarDistance * veryFarDistance) {
            rate = far;
        } else {
            rate = veryFar;
        }
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.POTATO || mode == RuntimeMode.SERVER) {
            rate *= 2;
        } else if (mode == RuntimeMode.EMERGENCY) {
            rate *= 4;
        } else if (mode == RuntimeMode.CINEMATIC && type == RuntimeWorkType.PARTICLE) {
            rate = Math.max(1, rate / 2);
        }
        return Math.max(1, rate);
    }

    public void markActive(Identifier systemId, BlockPos pos) {
        activity.put(key(systemId, pos), new ActivityRecord(false, RuntimeProfilerService.INSTANCE.serverTick()));
    }

    public void markIdle(Identifier systemId, BlockPos pos) {
        activity.put(key(systemId, pos), new ActivityRecord(true, RuntimeProfilerService.INSTANCE.serverTick()));
    }

    public void wake(Identifier systemId, BlockPos pos, WakeReason reason) {
        markActive(systemId, pos);
        BlockEntitySleepService.INSTANCE.wake(systemId, pos, reason);
    }

    public int trackedPositions() {
        return activity.size();
    }

    public void reset() {
        activity.clear();
    }

    private static double nearestPlayerDistanceSqr(Level level, BlockPos pos) {
        if (level == null || pos == null || level.players().isEmpty()) {
            return Double.MAX_VALUE;
        }
        double best = Double.MAX_VALUE;
        for (Player player : level.players()) {
            double dx = player.getX() - (pos.getX() + 0.5D);
            double dy = player.getY() - (pos.getY() + 0.5D);
            double dz = player.getZ() - (pos.getZ() + 0.5D);
            best = Math.min(best, dx * dx + dy * dy + dz * dz);
        }
        return best;
    }

    private static String key(Identifier id, BlockPos pos) {
        return (id == null ? "echoruntimeguard:unknown" : id.toString()) + "@"
                + (pos == null ? "none" : pos.toShortString());
    }

    private record ActivityRecord(boolean idle, long tick) {
    }
}
