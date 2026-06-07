package com.knoxhack.echo.socialcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoSocialCore {
    public static final String MODID = "echosocialcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoSocialCore(Object modEventBus) {
        LOGGER.info("ECHO: SocialCore online with faction, dialogue, NPC, and villager replacement plan contracts.");
    }
}
