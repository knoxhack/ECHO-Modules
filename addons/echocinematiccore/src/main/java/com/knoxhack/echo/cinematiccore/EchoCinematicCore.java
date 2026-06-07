package com.knoxhack.echo.cinematiccore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoCinematicCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoCinematicCore(Object modEventBus) {
        LOGGER.info("ECHO: CinematicCore loaded shared cinematic contracts.");
    }
}
