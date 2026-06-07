package com.knoxhack.echoritualcore.integration.holomap;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoritualcore.EchoRitualCore;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RitualCoreHoloMapIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private RitualCoreHoloMapIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoCoreServices.registerMapDataProvider(RitualCoreMapDataProvider.INSTANCE);
            EchoRitualCore.LOGGER.info("ECHO HoloMap marker provider registered for RitualCore.");
        }
    }
}
