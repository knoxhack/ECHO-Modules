package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryPowerGridIntegration {
    private RecoveryPowerGridIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerRemoteDeliveryProvider((player, snapshot) -> RecoveryConfig.REMOTE_RECOVERY_ENABLED.get()
                ? Optional.of("Powered recovery beacon check passed for cache " + snapshot.graveId())
                : Optional.empty());
        EchoRecovery.LOGGER.info("Recovery PowerGrid beacon provider registered.");
    }
}
