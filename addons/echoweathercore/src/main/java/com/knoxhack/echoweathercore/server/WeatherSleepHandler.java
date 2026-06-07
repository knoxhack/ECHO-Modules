package com.knoxhack.echoweathercore.server;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import net.minecraft.server.level.ServerLevel;

public final class WeatherSleepHandler {
    private WeatherSleepHandler() {}

    public static void onSleepFinished(Object event) {
        ServerLevel level = EchoBackendWorldEventBridge.sleepFinishedServerLevel(event);
        if (level == null || EchoBackendWorldEventBridge.sleepFinishedCanceled(event)) {
            return;
        }
        WeatherStateManager.getInstance().advanceEventsForSleep(level,
                EchoBackendWorldEventBridge.sleepFinishedSkippedTicks(event, level));
    }
}
