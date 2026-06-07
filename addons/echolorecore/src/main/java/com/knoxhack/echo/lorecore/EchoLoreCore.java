package com.knoxhack.echo.lorecore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoLoreCore {
    public static final String MODID = "echolorecore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoLoreCore(Object eventBus) {
        LOGGER.info("ECHO: LoreCore loaded neutral lore contracts.");
    }
}
