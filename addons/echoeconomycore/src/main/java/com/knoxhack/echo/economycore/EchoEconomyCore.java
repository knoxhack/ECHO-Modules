package com.knoxhack.echo.economycore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoEconomyCore {
    public static final String MODID = "echoeconomycore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoEconomyCore(Object modEventBus) {
        LOGGER.info("ECHO: EconomyCore online with currency, barter, shop, reward, and trade validation contracts.");
        var runtime = Agent9EconomyCoreRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: EconomyCore Agent 9 native host adapter {}.", runtime.get("status"));
    }
}
