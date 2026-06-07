package com.knoxhack.echoruntimeguard.runtime;

import com.knoxhack.echoruntimeguard.RuntimeGuardConfig;
import com.knoxhack.echoruntimeguard.api.RuntimeGuardAwareBlockEntity;
import com.knoxhack.echoruntimeguard.api.SleepState;
import com.knoxhack.echoruntimeguard.api.WakeReason;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockEntitySleepService {
    public static final BlockEntitySleepService INSTANCE = new BlockEntitySleepService();
    private final Map<String, SleepRecord> states = new ConcurrentHashMap<>();

    private BlockEntitySleepService() {
    }

    public boolean canSleep(BlockEntity blockEntity) {
        return blockEntity instanceof RuntimeGuardAwareBlockEntity aware && aware.canRuntimeGuardSleep();
    }

    public boolean shouldSleep(BlockEntity blockEntity) {
        if (!RuntimeGuardConfig.enabled()
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.BLOCK_ENTITY_GUARD_ENABLED, true)
                || !RuntimeGuardConfig.safeBool(RuntimeGuardConfig.SLEEP_IDLE_BLOCK_ENTITIES, true)
                || !canSleep(blockEntity)) {
            return false;
        }
        SleepState state = getSleepState(blockEntity);
        return state == SleepState.SLEEPING || state == SleepState.WAKE_ON_EVENT;
    }

    public SleepState getSleepState(BlockEntity blockEntity) {
        SleepRecord record = states.get(key(blockEntity));
        return record == null ? SleepState.ACTIVE : record.state();
    }

    public void setSleepState(BlockEntity blockEntity, SleepState state) {
        if (blockEntity == null || state == null) {
            return;
        }
        states.put(key(blockEntity), new SleepRecord(state, RuntimeProfilerService.INSTANCE.serverTick(), null));
    }

    public void wake(BlockEntity blockEntity, WakeReason reason) {
        if (blockEntity != null) {
            states.put(key(blockEntity), new SleepRecord(SleepState.ACTIVE, RuntimeProfilerService.INSTANCE.serverTick(), reason));
        }
    }

    public void wake(Identifier systemId, BlockPos pos, WakeReason reason) {
        states.put((systemId == null ? "echoruntimeguard:unknown" : systemId.toString()) + "@" + pos,
                new SleepRecord(SleepState.ACTIVE, RuntimeProfilerService.INSTANCE.serverTick(), reason));
    }

    public int tracked() {
        return states.size();
    }

    public void reset() {
        states.clear();
    }

    private static String key(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return "null";
        }
        String dimension = blockEntity.getLevel() == null ? "unloaded" : blockEntity.getLevel().dimension().identifier().toString();
        return dimension + "@" + blockEntity.getBlockPos().toShortString();
    }

    private record SleepRecord(SleepState state, long tick, WakeReason reason) {
    }
}
