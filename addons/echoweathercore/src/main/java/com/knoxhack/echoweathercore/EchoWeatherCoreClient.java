package com.knoxhack.echoweathercore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoWeatherCoreClient {
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoWeatherCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::clientSetup);
    }

    private void clientSetup(Object event) {
        LOGGER.info("ECHO: WeatherCore client setup complete.");
    }
}
