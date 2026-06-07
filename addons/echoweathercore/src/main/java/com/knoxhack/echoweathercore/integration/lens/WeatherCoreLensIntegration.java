package com.knoxhack.echoweathercore.integration.lens;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreLensIntegration {
    private WeatherCoreLensIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore Lens integration scaffold registered.");
    }
}
