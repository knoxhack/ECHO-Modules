package com.knoxhack.echoweathercore.util;

import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import com.knoxhack.echoweathercore.server.WeatherStateManager;

public final class WeatherCoreDebug {
    private WeatherCoreDebug() {}

    public static void printDebugInfo() {
        EchoWeatherCore.LOGGER.info("ECHO WeatherCore Debug Info:");
        EchoWeatherCore.LOGGER.info("  Loaded Profiles: {}", WeatherDataReloadListener.INSTANCE.getProfiles().size());
        EchoWeatherCore.LOGGER.info("  Active Events: tracked per-level via WeatherStateManager");
    }
}
