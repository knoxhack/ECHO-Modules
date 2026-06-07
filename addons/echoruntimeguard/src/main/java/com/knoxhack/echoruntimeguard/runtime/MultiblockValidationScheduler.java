package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.EchoRuntimeGuard;
import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.DirtyReason;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardProfiler;
import com.knoxhack.echoruntimeguard.api.ValidationPriority;
import com.knoxhack.echoruntimeguard.api.ValidationQueueSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class MultiblockValidationScheduler {
    public static final MultiblockValidationScheduler INSTANCE = new MultiblockValidationScheduler();
    private final Map<ValidationKey, ValidationRequest> queue = new ConcurrentHashMap<>();
    private final Map<String, DirtyReason> dirty = new ConcurrentHashMap<>();
    private int ranLastTick;
    private int mergedRequests;

    private MultiblockValidationScheduler() {
    }

    public void requestValidation(Identifier systemId, Level level, BlockPos controllerPos,
            ValidationPriority priority, Runnable task) {
        if (level == null || controllerPos == null || task == null) {
            return;
        }
        Identifier safeSystem = systemId == null ? EchoRuntimeGuard.id("unknown_multiblock") : systemId;
        ValidationKey key = new ValidationKey(level.dimension().identifier().toString(), controllerPos.immutable(), safeSystem);
        ValidationPriority safePriority = priority == null ? ValidationPriority.SCHEDULED_IDLE : priority;
        queue.compute(key, (ignored, existing) -> {
            if (existing != null) {
                mergedRequests++;
                if (safePriority.rank() < existing.priority().rank()) {
                    return new ValidationRequest(key, safePriority, task, RuntimeProfilerService.INSTANCE.serverTick());
                }
                return existing;
            }
            return new ValidationRequest(key, safePriority, task, RuntimeProfilerService.INSTANCE.serverTick());
        });
    }

    public void markDirty(Level level, BlockPos controllerPos, DirtyReason reason) {
        if (level == null || controllerPos == null) {
            return;
        }
        dirty.put(level.dimension().identifier() + "@" + controllerPos.toShortString(),
                reason == null ? DirtyReason.DEBUG : reason);
    }

    public boolean canValidateNow(Level level, BlockPos controllerPos) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.MULTIBLOCK_SCHEDULER_ENABLED, true)) {
            return true;
        }
        return queue.size() < RuntimeGuardConfig.safeInt(RuntimeGuardConfig.MAX_VALIDATIONS_PER_TICK, 2) * 20;
    }

    public void onServerTick(Object event) {
        ranLastTick = 0;
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.MULTIBLOCK_SCHEDULER_ENABLED, true)
                || queue.isEmpty()) {
            return;
        }
        int max = RuntimeGuardConfig.safeInt(RuntimeGuardConfig.MAX_VALIDATIONS_PER_TICK, 2);
        List<ValidationRequest> requests = new ArrayList<>(queue.values());
        requests.sort(Comparator.comparingInt((ValidationRequest request) -> request.priority().rank())
                .thenComparingLong(ValidationRequest::requestedTick));
        for (ValidationRequest request : requests) {
            if (ranLastTick >= max) {
                break;
            }
            if (!queue.remove(request.key(), request)) {
                continue;
            }
            try {
                RuntimeGuardProfiler.time(EchoRuntimeGuard.id("multiblock_validation/" + request.key().systemId().getPath()),
                        request.task());
            } catch (RuntimeException exception) {
                EchoRuntimeGuard.LOGGER.warn("RuntimeGuard validation task {} failed; continuing.",
                        request.key().systemId(), exception);
            }
            dirty.remove(request.key().dimension() + "@" + request.key().pos().toShortString());
            ranLastTick++;
        }
    }

    public ValidationQueueSnapshot getSnapshot() {
        Map<Identifier, Integer> bySystem = new LinkedHashMap<>();
        for (ValidationRequest request : queue.values()) {
            bySystem.merge(request.key().systemId(), 1, Integer::sum);
        }
        return new ValidationQueueSnapshot(queue.size(), dirty.size(), ranLastTick, mergedRequests, bySystem);
    }

    public void reset() {
        queue.clear();
        dirty.clear();
        ranLastTick = 0;
        mergedRequests = 0;
    }

    private record ValidationKey(String dimension, BlockPos pos, Identifier systemId) {
    }

    private record ValidationRequest(ValidationKey key, ValidationPriority priority, Runnable task, long requestedTick) {
    }
}
