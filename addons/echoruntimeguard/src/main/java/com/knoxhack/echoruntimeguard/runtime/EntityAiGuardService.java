package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardAwareEntity;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardEntityPriority;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class EntityAiGuardService {
    public static final EntityAiGuardService INSTANCE = new EntityAiGuardService();
    private final Map<Integer, Long> pathfindingRecords = new ConcurrentHashMap<>();

    private EntityAiGuardService() {
    }

    public boolean shouldRunFullAI(Entity entity) {
        return getRecommendedAiTickRate(entity) <= 1
                || RuntimeProfilerService.INSTANCE.serverTick() % getRecommendedAiTickRate(entity) == 0L;
    }

    public int getRecommendedAiTickRate(Entity entity) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.ENTITY_GUARD_ENABLED, true)
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.THROTTLE_FAR_ENTITY_AI, true)
                || entity == null) {
            return 1;
        }
        if (entity instanceof RuntimeGuardAwareEntity aware) {
            if (!aware.canThrottleAi() || aware.getRuntimePriority() == RuntimeGuardEntityPriority.BOSS) {
                return 1;
            }
        }
        double distanceSqr = nearestPlayerDistanceSqr(entity);
        int far = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_DISTANCE_BLOCKS, 64);
        int veryFar = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.VERY_FAR_DISTANCE_BLOCKS, 128);
        int rate = distanceSqr <= far * far ? 1
                : distanceSqr <= veryFar * veryFar
                        ? RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_ENTITY_AI_TICK_RATE, 40)
                        : RuntimeGuardConfig.safeInt(RuntimeGuardConfig.VERY_FAR_ENTITY_AI_TICK_RATE, 100);
        RuntimeMode mode = RuntimeModeService.INSTANCE.mode();
        if (mode == RuntimeMode.EMERGENCY) {
            rate *= 2;
        } else if (mode == RuntimeMode.POTATO || mode == RuntimeMode.SERVER) {
            rate = Math.max(rate, RuntimeGuardConfig.safeInt(RuntimeGuardConfig.FAR_ENTITY_AI_TICK_RATE, 40));
        }
        return Math.max(1, rate);
    }

    public boolean shouldThrottlePathfinding(Entity entity) {
        if (entity == null) {
            return false;
        }
        long tick = RuntimeProfilerService.INSTANCE.serverTick();
        Long previous = pathfindingRecords.get(entity.getId());
        int cooldown = (int) Math.max(1.0D, 20.0D * RuntimeGuardConfig.safeDouble(RuntimeGuardConfig.PATHFINDING_COOLDOWN_MULTIPLIER, 2.0D));
        return previous != null && tick - previous < cooldown && getRecommendedAiTickRate(entity) > 1;
    }

    public void recordPathfinding(Entity entity) {
        if (entity != null) {
            pathfindingRecords.put(entity.getId(), RuntimeProfilerService.INSTANCE.serverTick());
        }
    }

    public String statusLine() {
        return "Tracked pathfinding cooldowns " + pathfindingRecords.size();
    }

    public void reset() {
        pathfindingRecords.clear();
    }

    private static double nearestPlayerDistanceSqr(Entity entity) {
        if (entity.level().players().isEmpty()) {
            return Double.MAX_VALUE;
        }
        double best = Double.MAX_VALUE;
        for (Player player : entity.level().players()) {
            best = Math.min(best, entity.distanceToSqr(player));
        }
        return best;
    }
}
