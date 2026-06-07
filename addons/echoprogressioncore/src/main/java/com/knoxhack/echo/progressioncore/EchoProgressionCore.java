package com.knoxhack.echo.progressioncore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoProgressionCore {
    public static final String MODID = "echoprogressioncore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoProgressionCore(Object modEventBus) {
        LOGGER.info("ECHO: ProgressionCore online with progression, unlock graph, and objective contracts.");
    }
}
