package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryAshfallIntegration {
    private RecoveryAshfallIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerSignalProvider((player, snapshot) -> snapshot.contaminated()
                ? Optional.of("Field Recovery signal integrity degraded by Ashfall conditions.")
                : Optional.empty());
        EchoRecovery.LOGGER.info("Recovery Ashfall field-cache signal provider registered.");
    }
}
