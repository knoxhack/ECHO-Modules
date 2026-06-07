package com.knoxhack.echo.biomecore;

import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;
import net.minecraft.world.level.Level;

public final class EchoBiomeCoreEvents {
    private static volatile boolean levelTickHookAttached;

    private EchoBiomeCoreEvents() {
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

    public static EchoBiomeRuntimeState.LiveBiomeTickState activeBiomeTick() {
        return EchoBiomeRuntimeState.activeBiomeTick();
    }

    public static void recordAgent7LiveHookForTests(long gameTick) {
        recordAgent7LiveHook(Math.max(0L, gameTick), "EchoBiomeCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onLevelTick(Level level) {
        if (level == null || level.isClientSide()) {
            return;
        }
        recordAgent7LiveHook(level.getGameTime(), "EchoBiomeCoreEvents.onLevelTick");
        EchoBiomeRuntimeState.materializeLevelTick(
                level.getGameTime(),
                "echo_native.level_tick.post");
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echobiomecore",
                "level_tick.post",
                gameTick,
                sourceReason);
    }
}
