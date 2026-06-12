package com.knoxhack.echocursecore.integration;

import com.knoxhack.echocursecore.EchoCurseCore;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class CurseCoreIntegrations {
    private CurseCoreIntegrations() {
    }

    public static void registerOptional() {
        if (EchoRuntimeModules.isLoaded("echoarcanacore")) {
            tryInvoke("com.knoxhack.echocursecore.integration.arcana.CurseCoreArcanaIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echocursecore.integration.terminal.CurseCoreTerminalIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            tryInvoke("com.knoxhack.echocursecore.integration.missioncore.CurseCoreMissionCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            tryInvoke("com.knoxhack.echocursecore.integration.lens.CurseCoreLensIntegration");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException exception) {
            EchoCurseCore.LOGGER.debug("Optional CurseCore integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoCurseCore.LOGGER.warn("Optional CurseCore integration {} could not be registered.", className, exception);
        }
    }
}
