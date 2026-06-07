package com.knoxhack.echo.cameracore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoCameraCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoCameraCore(Object modEventBus) {
        LOGGER.info("ECHO: CameraCore loaded shared camera contracts.");
    }
}
