package com.knoxhack.echoritualcore.integration;

import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class RitualCoreIntegrations {
    private RitualCoreIntegrations() {
    }

    public static void registerOptional() {
        if (EchoRuntimeModules.isLoaded("echoarcanacore")) {
            tryInvoke("com.knoxhack.echoritualcore.integration.arcana.RitualCoreArcanaIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echoritualcore.integration.terminal.RitualCoreTerminalIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            tryInvoke("com.knoxhack.echoritualcore.integration.missioncore.RitualCoreMissionCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            tryInvoke("com.knoxhack.echoritualcore.integration.lens.RitualCoreLensIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoholomap")) {
            tryInvoke("com.knoxhack.echoritualcore.integration.holomap.RitualCoreHoloMapIntegration");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException exception) {
            EchoRitualCore.LOGGER.debug("Optional RitualCore integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoRitualCore.LOGGER.warn("Optional RitualCore integration {} could not be registered.", className, exception);
        }
    }
}
