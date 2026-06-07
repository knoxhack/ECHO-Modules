package com.knoxhack.echo.logisticscore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoLogisticsCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoLogisticsCore(Object modEventBus) {
        LOGGER.info("ECHO: LogisticsCore loaded shared logistics contracts.");
        var runtime = Agent9LogisticsCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: LogisticsCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}
