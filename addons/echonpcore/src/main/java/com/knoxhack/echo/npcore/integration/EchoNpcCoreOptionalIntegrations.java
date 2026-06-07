package com.knoxhack.echo.npcore.integration;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.integration.holomap.NpcMapDataProvider;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EchoNpcCoreOptionalIntegrations {
    private static final AtomicBoolean COMMON_REGISTERED = new AtomicBoolean(false);

    private EchoNpcCoreOptionalIntegrations() {
    }

    public static void registerCommon() {
        if (!COMMON_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoCoreServices.registerMapDataProvider(NpcMapDataProvider.INSTANCE);
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            invoke("com.knoxhack.echo.npcore.integration.terminal.NpcTerminalIntegration");
        }
    }

    private static void invoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            EchoNpcCore.LOGGER.warn("NPCore optional integration {} failed during registration.", className, cause);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoNpcCore.LOGGER.warn("NPCore optional integration {} is unavailable.", className, exception);
        }
    }
}
