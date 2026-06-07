package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;
import net.minecraft.world.level.Level;

public final class EchoStructureCoreEvents {
    private static volatile boolean levelTickHookAttached;

    private EchoStructureCoreEvents() {
    }

    public static synchronized void attach() {
        if (levelTickHookAttached) {
            return;
        }
        levelTickHookAttached = true;
    }

    public static boolean levelTickHookAttached() {
        return levelTickHookAttached;
    }

    public static EchoStructureRuntimeState.LiveStructureTickState activeStructureTick() {
        return EchoStructureRuntimeState.activeStructureTick();
    }

    public static void recordAgent7LiveHookForTests(long gameTick) {
        recordAgent7LiveHook(Math.max(0L, gameTick), "EchoStructureCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onLevelTick(Level level) {
        if (level == null || level.isClientSide()) {
            return;
        }
        recordAgent7LiveHook(level.getGameTime(), "EchoStructureCoreEvents.onLevelTick");
        EchoStructureRuntimeState.materializeLevelTick(
                level.getGameTime(),
                "echo_native.level_tick.post");
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echostructurecore",
                "level_tick.post",
                gameTick,
                sourceReason);
    }
}
