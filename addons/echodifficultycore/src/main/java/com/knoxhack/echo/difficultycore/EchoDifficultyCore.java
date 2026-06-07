package com.knoxhack.echo.difficultycore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoDifficultyCore {
    public static final String MODID = "echodifficultycore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoDifficultyCore(Object eventBus) {
        EchoDifficultyCoreEvents.attach();
        LOGGER.info("ECHO: DifficultyCore loaded neutral difficulty contracts.");
    }
}
