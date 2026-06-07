package com.knoxhack.echo.platformcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoPlatformCore {
    public static final String MODID = EchoPlatformConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoPlatformCore() {
        LOGGER.info(
                "ECHO: PlatformCore loaded {} platform features and {} base permissions.",
                EchoPlatformConstants.PLATFORM_FEATURES.size(),
                EchoPlatformConstants.PLATFORM_PERMISSIONS.size()
        );
    }
}
