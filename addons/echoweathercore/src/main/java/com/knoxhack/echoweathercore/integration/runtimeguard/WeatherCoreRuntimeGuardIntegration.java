package com.knoxhack.echoweathercore.integration.runtimeguard;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreRuntimeGuardIntegration {
    private WeatherCoreRuntimeGuardIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore RuntimeGuard integration scaffold registered.");
    }
}
