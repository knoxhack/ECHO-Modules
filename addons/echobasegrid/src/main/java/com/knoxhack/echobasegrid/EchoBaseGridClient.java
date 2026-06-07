package com.knoxhack.echobasegrid;

import com.knoxhack.echobasegrid.client.BaseGridActions;
import com.knoxhack.echobasegrid.client.BaseGridDataProviders;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class EchoBaseGridClient {
    public EchoBaseGridClient(Object container) {
        BaseGridDataProviders.register();
        BaseGridActions.register();
        registerTerminalIntegration();
        registerHoloMapIntegration();
    }

    private static void registerTerminalIntegration() {
        if (!EchoRuntimeModules.isLoaded("echoterminal")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echobasegrid.integration.BaseGridTerminalClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoBaseGrid.LOGGER.warn("ECHO: Base Grid Terminal tab could not be registered.", exception);
        }
    }

    private static void registerHoloMapIntegration() {
        if (!EchoRuntimeModules.isLoaded("echoholomap")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echobasegrid.integration.holomap.BaseGridHoloMapClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoBaseGrid.LOGGER.warn("ECHO: Base Grid HoloMap client integration could not be registered.", exception);
        }
    }
}
