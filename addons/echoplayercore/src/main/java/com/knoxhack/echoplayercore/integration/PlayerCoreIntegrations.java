package com.knoxhack.echoplayercore.integration;

import com.knoxhack.echoplayercore.EchoPlayerCore;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class PlayerCoreIntegrations {
    private PlayerCoreIntegrations() {
    }

    public static boolean dataCoreLoaded() {
        return EchoRuntimeModules.isLoaded("echodatacore");
    }

    public static boolean worldCoreLoaded() {
        return EchoRuntimeModules.isLoaded("echoworldcore");
    }

    public static boolean terminalLoaded() {
        return EchoRuntimeModules.isLoaded("echoterminal");
    }

    public static boolean holoMapLoaded() {
        return EchoRuntimeModules.isLoaded("echoholomap");
    }

    public static boolean runtimeGuardLoaded() {
        return EchoRuntimeModules.isLoaded("echoruntimeguard");
    }

    public static void logIntegrationStatus() {
        EchoPlayerCore.LOGGER.info(
                "ECHO PlayerCore integrations - DataCore:{}, WorldCore:{}, Terminal:{}, HoloMap:{}, RuntimeGuard:{}",
                dataCoreLoaded(), worldCoreLoaded(), terminalLoaded(), holoMapLoaded(), runtimeGuardLoaded()
        );
    }
}
