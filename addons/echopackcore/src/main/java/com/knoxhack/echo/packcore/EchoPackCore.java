package com.knoxhack.echo.packcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoPackCore {
    public static final String MODID = EchoPackConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoPackCore(Object ignoredModEventBus) {
        LOGGER.info(
                "ECHO: PackCore loaded {} built-in variants and {} built-in channels.",
                EchoPackConstants.BUILTIN_VARIANTS.size(),
                EchoPackConstants.BUILTIN_CHANNELS.size()
        );
    }
}
