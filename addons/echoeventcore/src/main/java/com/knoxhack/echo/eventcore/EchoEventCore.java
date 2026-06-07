package com.knoxhack.echo.eventcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoEventCore {
    public static final String MODID = "echoeventcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoEventCore() {
        LOGGER.info("ECHO: EventCore loaded neutral world event contracts.");
    }
}
