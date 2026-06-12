package com.knoxhack.echotutorialcore.integration;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echotutorialcore.EchoTutorialCore;

public final class TutorialIntegrations {
    private TutorialIntegrations() {}

    public static void registerOptionalIntegrations() {
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.terminal.TutorialTerminalIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.mission.TutorialMissionCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoindex")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.index.TutorialIndexIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echopowergrid")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.powergrid.TutorialPowerGridIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.lens.TutorialLensIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoholomap")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.holomap.TutorialHoloMapIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echosoundcore")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.soundcore.TutorialSoundCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoworldcore")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.worldcore.TutorialWorldCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echodatacore")) {
            tryInvoke("com.knoxhack.echotutorialcore.integration.datacore.TutorialDataCoreIntegration");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException e) {
            EchoTutorialCore.LOGGER.debug("Optional integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError e) {
            EchoTutorialCore.LOGGER.warn("Optional integration {} could not be registered.", className, e);
        }
    }
}
