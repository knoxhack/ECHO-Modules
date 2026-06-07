package com.knoxhack.echoweathercore.integration.powergrid;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCorePowerGridIntegration {
    private WeatherCorePowerGridIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore PowerGrid integration scaffold registered.");
    }
}
