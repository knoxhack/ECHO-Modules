package com.knoxhack.echoruntimeguard;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoruntimeguard.client.ClientFpsMonitor;

public final class EchoRuntimeGuardClient {
    public EchoRuntimeGuardClient() {
        EchoBackendLifecycleBridge.registerGameEventHandler(ClientFpsMonitor::onClientTick);
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalIntegration();
        }
    }

    private static void registerTerminalIntegration() {
        try {
            Class.forName("com.knoxhack.echoruntimeguard.integration.RuntimeGuardTerminalClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoRuntimeGuard.LOGGER.warn("RuntimeGuard Terminal page could not be registered.", exception);
        }
    }
}
