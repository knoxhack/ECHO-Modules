package com.knoxhack.echoweathercore.integration.mission;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreMissionIntegration {
    private WeatherCoreMissionIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore MissionCore integration scaffold registered.");
    }
}
