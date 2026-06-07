package com.knoxhack.echoweathercore.integration.nexus;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreNexusIntegration {
    private WeatherCoreNexusIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore NexusProtocol integration scaffold registered.");
    }
}
