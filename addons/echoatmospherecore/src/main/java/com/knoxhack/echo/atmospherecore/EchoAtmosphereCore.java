package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class EchoAtmosphereCore {
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoAtmosphereCore(Object modEventBus) {
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoAtmosphereCoreEvents::onLevelTick);
        EchoAtmosphereCoreEvents.attach();
        LOGGER.info("ECHO: AtmosphereCore loaded shared atmosphere contracts.");
    }
}
