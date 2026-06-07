package com.knoxhack.echoweathercore.integration.faction;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreFactionIntegration {
    private WeatherCoreFactionIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore Faction integration scaffold registered.");
    }
}
