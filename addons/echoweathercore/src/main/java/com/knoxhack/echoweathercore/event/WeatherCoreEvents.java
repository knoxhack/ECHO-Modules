package com.knoxhack.echoweathercore.event;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.server.WeatherScheduler;
import com.knoxhack.echoweathercore.server.WeatherStateManager;
import net.minecraft.server.level.ServerLevel;

public final class WeatherCoreEvents {
    private static volatile boolean levelTickHookAttached;

    private WeatherCoreEvents() {
    }

    public static synchronized void attach() {
        if (levelTickHookAttached) {
            return;
        }
        EchoBackendLifecycleBridge.registerGameEventHandler(WeatherCoreEvents::onLevelTick);
        levelTickHookAttached = true;
        EchoWeatherCore.LOGGER.info("WeatherCore live level tick gameplay hook attached.");
    }

    public static boolean levelTickHookAttached() {
        return levelTickHookAttached;
    }

    public static void recordAgent7LiveHookForTests(long gameTick) {
        recordAgent7LiveHook(Math.max(0L, gameTick), "WeatherCoreEvents.recordAgent7LiveHookForTests");
    }

    public static void onLevelTick(Object event) {
        ServerLevel level = EchoBackendWorldEventBridge.postTickServerLevel(event);
        if (level == null) {
            return;
        }
        recordAgent7LiveHook(level.getGameTime(), "WeatherCoreEvents.onLevelTick");
        WeatherScheduler.tick(level);
        WeatherStateManager.getInstance().tickLevel(level);
    }

    private static void recordAgent7LiveHook(long gameTick, String sourceReason) {
        EchoNativeAgent7LiveHookEvidenceBridge.recordExactCallback(
                "echoweathercore",
                "level_tick.post",
                gameTick,
                sourceReason);
    }
}
