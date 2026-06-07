package com.knoxhack.echo.statuscore;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoStatusCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoStatusCore(Object modEventBus) {
        EchoStatusCoreEvents.attach();
        LOGGER.info("{} loaded with {} built-in status kinds.", EchoStatusConstants.MOD_NAME, EchoStatusKind.BUILT_IN_STATUS_KINDS.size());
    }
}
