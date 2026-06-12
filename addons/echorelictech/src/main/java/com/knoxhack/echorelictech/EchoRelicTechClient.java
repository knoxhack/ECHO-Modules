package com.knoxhack.echorelictech;

import com.echoplatform.echocore.api.EchoRuntimeModules;

public class EchoRelicTechClient {
    public EchoRelicTechClient(Object modEventBus) {
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalClientIntegration();
        }
    }

    private static void registerTerminalClientIntegration() {
        try {
            Class.forName("com.knoxhack.echorelictech.integration.terminal.RelicTechTerminalClientIntegration")
                .getMethod("register")
                .invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            EchoRelicTech.LOGGER.warn("RelicTech Terminal client integration could not be registered.", e);
        }
    }
}
