package com.knoxhack.echospellcore.integration;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echospellcore.EchoSpellCore;

public final class SpellCoreIntegrations {
    private SpellCoreIntegrations() {
    }

    public static void registerOptional() {
        if (EchoRuntimeModules.isLoaded("echoarcanacore")) {
            tryInvoke("com.knoxhack.echospellcore.integration.arcana.SpellCoreArcanaIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echospellcore.integration.terminal.SpellCoreTerminalIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            tryInvoke("com.knoxhack.echospellcore.integration.missioncore.SpellCoreMissionCoreIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            tryInvoke("com.knoxhack.echospellcore.integration.lens.SpellCoreLensIntegration");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException exception) {
            EchoSpellCore.LOGGER.debug("Optional SpellCore integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoSpellCore.LOGGER.warn("Optional SpellCore integration {} could not be registered.", className, exception);
        }
    }
}
