package com.knoxhack.echo.npcore.client.screencore;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import java.lang.reflect.InvocationTargetException;

public final class ScreenCoreNpcScreenAdapter {
    private ScreenCoreNpcScreenAdapter() {
    }

    public static boolean tryOpen(EchoNpcScreenState state) {
        if (!EchoNpcCoreConfig.bool(EchoNpcCoreConfig.USE_SCREENCORE_NPC_SCREENS, true)
                || !EchoRuntimeModules.isLoaded("echoscreencore")) {
            return false;
        }
        try {
            return invoke("open", state);
        } catch (RuntimeException | LinkageError exception) {
            EchoNpcCore.LOGGER.warn("ScreenCore NPC adapter failed; falling back to classic screen.", exception);
            return false;
        }
    }

    public static boolean trySync(EchoNpcScreenState state) {
        if (!EchoNpcCoreConfig.bool(EchoNpcCoreConfig.USE_SCREENCORE_NPC_SCREENS, true)
                || !EchoRuntimeModules.isLoaded("echoscreencore")) {
            return false;
        }
        try {
            return invoke("sync", state);
        } catch (RuntimeException | LinkageError exception) {
            EchoNpcCore.LOGGER.warn("ScreenCore NPC sync failed; falling back to classic screen.", exception);
            return false;
        }
    }

    private static boolean invoke(String methodName, EchoNpcScreenState state) {
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echo.npcore.client.screencore.ScreenCoreNpcScreenBridge");
            Object result = bridge.getMethod(methodName, EchoNpcScreenState.class).invoke(null, state);
            return result instanceof Boolean opened && opened;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof LinkageError linkageError) {
                throw linkageError;
            }
            throw new IllegalStateException("ScreenCore NPC bridge failed.", cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("ScreenCore NPC bridge is unavailable.", exception);
        }
    }
}
