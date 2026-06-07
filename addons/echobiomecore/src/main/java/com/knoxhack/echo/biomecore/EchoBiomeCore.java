package com.knoxhack.echo.biomecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoBiomeCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoBiomeCore() {
        EchoBiomeCoreEvents.attach();
        LOGGER.info("ECHO: BiomeCore loaded shared biome profile contracts.");
    }
}
