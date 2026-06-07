package com.knoxhack.echo.bridgecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoBridgeCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoBridgeCore(Object modEventBus) {
        LOGGER.info("ECHO: BridgeCore loaded neutral local bridge contracts.");
    }
}
