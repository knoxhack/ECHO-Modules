package com.knoxhack.echoworldcore.event;

import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.Config;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import net.minecraft.server.level.ServerPlayer;

public final class WorldCoreEvents {
    private static volatile boolean playerTickHookAttached;

    private WorldCoreEvents() {
    }

    public static synchronized void attach() {
        if (playerTickHookAttached) {
            return;
        }
        playerTickHookAttached = true;
        EchoWorldCore.LOGGER.info("WorldCore live player tick gameplay hook attached.");
    }

    public static boolean playerTickHookAttached() {
        return playerTickHookAttached;
    }

    public static void recordAgent7LiveHookForTests(long gameTick) {
        recordAgent7LiveHook(Math.max(0L, gameTick), "WorldCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player != null && player.tickCount % Config.playerScanInterval() == 0) {
            recordAgent7LiveHook(player.level().getGameTime(), "WorldCoreEvents.onPlayerTick");
            WorldRegionService.INSTANCE.tickPlayer(player);
        }
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echoworldcore",
                "player_tick.post",
                gameTick,
                sourceReason);
    }
}
