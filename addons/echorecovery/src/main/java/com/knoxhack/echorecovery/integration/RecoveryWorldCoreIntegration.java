package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryWorldCoreIntegration {
    private static boolean registered;

    private RecoveryWorldCoreIntegration() {
    }

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        RecoveryIntegrations.registerSignalProvider((player, snapshot) -> {
            WorldHazardSnapshot hazard = EchoCoreServices.hazardService().hazardSnapshot(player);
            return hazard.safeZone()
                    ? Optional.empty()
                    : Optional.of("WorldCore hazard context: " + hazard.summary());
        });
    }
}
