package com.knoxhack.echopowergrid.integration.holomap;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echopowergrid.EchoPowerGrid;

public final class PowerGridHoloMapIntegration {
    private static boolean registered;

    private PowerGridHoloMapIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerMapDataProvider(PowerGridMapDataProvider.INSTANCE);
        EchoPowerGrid.LOGGER.info("ECHO PowerGrid HoloMap provider registered.");
    }
}
