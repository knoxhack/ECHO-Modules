package com.knoxhack.echo.inputcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoInputCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoInputCore() {
        LOGGER.info("ECHO: InputCore loaded shared input contracts.");
    }
}
