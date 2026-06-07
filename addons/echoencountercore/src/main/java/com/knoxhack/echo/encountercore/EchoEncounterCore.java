package com.knoxhack.echo.encountercore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoEncounterCore {
    public static final String MODID = "echoencountercore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoEncounterCore(Object eventBus) {
        LOGGER.info("ECHO: EncounterCore loaded neutral encounter contracts.");
    }
}
