package com.knoxhack.echo.contentcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoContentCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoContentCore() {
        LOGGER.info("ECHO: ContentCore loaded shared content ownership contracts.");
    }
}
