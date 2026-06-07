package com.knoxhack.echo.assetcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoAssetCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoAssetCore(Object modEventBus) {
        LOGGER.info("ECHO: AssetCore loaded shared asset and TextureForge contracts.");
    }
}
