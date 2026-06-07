package com.knoxhack.echo.questdirector;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoQuestDirector {
    public static final String MODID = "echoquestdirector";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoQuestDirector(Object eventBus) {
        LOGGER.info("ECHO: QuestDirector loaded neutral director contracts.");
    }
}
