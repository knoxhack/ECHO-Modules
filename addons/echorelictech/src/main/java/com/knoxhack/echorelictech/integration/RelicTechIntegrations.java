package com.knoxhack.echorelictech.integration;

import com.knoxhack.echorelictech.EchoRelicTech;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class RelicTechIntegrations {
    private RelicTechIntegrations() {}

    public static void registerOptional() {
        if (EchoRuntimeModules.isLoaded("echoarcanacore")) {
            tryInvoke("com.knoxhack.echorelictech.integration.arcana.RelicTechArcanaIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echorelictech.integration.terminal.RelicTechTerminalIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            tryInvoke("com.knoxhack.echorelictech.integration.lens.RelicTechLensIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoholomap")) {
            tryInvoke("com.knoxhack.echorelictech.integration.holomap.RelicTechHoloMapIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echopowergrid")) {
            tryInvoke("com.knoxhack.echorelictech.integration.powergrid.RelicTechPowerGridIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoworldcore")) {
            tryInvoke("com.knoxhack.echorelictech.integration.worldcore.RelicTechWorldCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echosoundcore")) {
            tryInvoke("com.knoxhack.echorelictech.integration.soundcore.RelicTechSoundCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echonexusprotocol")) {
            tryInvoke("com.knoxhack.echorelictech.integration.nexus.RelicTechNexusIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            tryInvoke("com.knoxhack.echorelictech.integration.missioncore.RelicTechMissionCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echodatacore")) {
            tryInvoke("com.knoxhack.echorelictech.integration.datacore.RelicTechDataCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            tryInvoke("com.knoxhack.echorelictech.integration.RelicTechMachineCoreRuntimeProvider");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException e) {
            EchoRelicTech.LOGGER.debug("Optional integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError e) {
            EchoRelicTech.LOGGER.warn("Optional integration {} could not be registered.", className, e);
        }
    }
}
