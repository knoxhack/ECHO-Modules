package com.knoxhack.echoweathercore.integration.soundcore;

import com.knoxhack.echoweathercore.EchoWeatherCore;

public final class WeatherCoreSoundCoreIntegration {
    private WeatherCoreSoundCoreIntegration() {}

    public static void register() {
        EchoWeatherCore.LOGGER.info("WeatherCore SoundCore integration scaffold registered.");
    }
}
