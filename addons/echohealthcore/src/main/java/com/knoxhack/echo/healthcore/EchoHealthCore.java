package com.knoxhack.echo.healthcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoHealthCore {
    public static final String MODID = EchoHealthConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoHealthCore() {
        LOGGER.info(
                "ECHO: HealthCore loaded {} local-first health metric contracts.",
                EchoHealthConstants.TRACKED_METRIC_IDS.size()
        );
    }
}
