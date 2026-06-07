package com.knoxhack.echo.powercore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoPowerCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoPowerCore() {
        LOGGER.info("ECHO: PowerCore loaded shared power network contracts.");
        var runtime = Agent9PowerCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: PowerCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}
