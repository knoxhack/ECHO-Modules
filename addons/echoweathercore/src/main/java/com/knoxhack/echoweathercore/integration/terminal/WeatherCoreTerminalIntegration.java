package com.knoxhack.echoweathercore.integration.terminal;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreTerminalIntegration {
    private WeatherCoreTerminalIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore Terminal integration scaffold registered.");
    }
}
