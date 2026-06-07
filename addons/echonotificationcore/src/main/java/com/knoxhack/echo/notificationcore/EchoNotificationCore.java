package com.knoxhack.echo.notificationcore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoNotificationCore {
    public static final String MODID = "echonotificationcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoNotificationCore(Object eventBus) {
        LOGGER.info("ECHO: NotificationCore loaded neutral notification contracts.");
    }
}
