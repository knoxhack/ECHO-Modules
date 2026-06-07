package com.knoxhack.echo.guidecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoGuideCore {
    public static final String MODID = "echoguidecore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoGuideCore(Object eventBus) {
        LOGGER.info("ECHO: GuideCore loaded neutral guide contracts.");
    }
}
