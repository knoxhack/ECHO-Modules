package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

public final class EchoSpawnCoreEvents {
    private static volatile boolean finalizeSpawnHookAttached;

    private EchoSpawnCoreEvents() {
    }

    public static synchronized void attach() {
        if (finalizeSpawnHookAttached) {
            return;
        }
        finalizeSpawnHookAttached = true;
    }

    public static boolean finalizeSpawnHookAttached() {
        return finalizeSpawnHookAttached;
    }

    public static EchoSpawnRuntimeState.LiveSpawnEventState activeSpawnEvent() {
        return EchoSpawnRuntimeState.activeSpawnEvent();
    }

    public static void recordAgent7LiveHookForTests(long gameTick) {
        recordAgent7LiveHook(Math.max(0L, gameTick), "EchoSpawnCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onFinalizeSpawn(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        BlockPos pos = entity.blockPosition();
        recordAgent7LiveHook(entity.level().getGameTime(), "EchoSpawnCoreEvents.onFinalizeSpawn");
        EchoSpawnRuntimeState.materializeFinalizeSpawn(
                entity.getType().toString(),
                "echospawncore:live_finalize_region",
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                entity.level().getGameTime(),
                "echo_native.finalize_spawn");
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echospawncore",
                "finalize_spawn",
                gameTick,
                sourceReason);
    }
}
