package com.knoxhack.echoweathercore.integration.drone;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreDroneIntegration {
    private WeatherCoreDroneIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore Drone integration scaffold registered.");
    }
}
