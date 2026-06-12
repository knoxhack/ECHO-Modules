package com.knoxhack.echo.scriptcore.client;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class EchoScriptCoreClient {
    public EchoScriptCoreClient() {
        registerScreenCoreBridge();
        registerTerminalBridge();
    }

    private static void registerTerminalBridge() {
        if (!EchoRuntimeModules.isLoaded("echoterminal")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echo.scriptcore.client.terminal.ScriptCoreTerminalBridge")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoScriptCore.LOGGER.warn("ScriptCore Terminal browser could not be registered.", exception);
        }
    }

    private static void registerScreenCoreBridge() {
        if (!EchoRuntimeModules.isLoaded("echoscreencore") || !EchoRuntimeModules.isLoaded("echonetcore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echo.scriptcore.client.screencore.ScriptCoreScreenCoreBridge")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoScriptCore.LOGGER.warn("ScriptCore ScreenCore UI action bridge could not be registered.", exception);
        }
    }
}
