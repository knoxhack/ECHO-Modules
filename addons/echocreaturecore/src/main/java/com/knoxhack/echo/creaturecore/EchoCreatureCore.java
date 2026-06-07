package com.knoxhack.echo.creaturecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoCreatureCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoCreatureCore(Object modEventBus) {
        LOGGER.info("{} loaded neutral creature contracts.", EchoCreatureConstants.MOD_NAME);
    }
}
