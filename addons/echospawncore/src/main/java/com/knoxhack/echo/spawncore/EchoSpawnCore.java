package com.knoxhack.echo.spawncore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoSpawnCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoSpawnCore() {
        EchoSpawnCoreEvents.attach();
        LOGGER.info("{} loaded neutral spawn contracts.", EchoSpawnConstants.MOD_NAME);
    }
}
