package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryConvoyIntegration {
    private RecoveryConvoyIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerRemoteDeliveryProvider((player, snapshot) -> RecoveryConfig.REMOTE_RECOVERY_ENABLED.get()
                ? Optional.of("Convoy field retrieval hook available for recovery cache " + snapshot.graveId())
                : Optional.empty());
        EchoRecovery.LOGGER.info("Recovery Convoy retrieval provider registered.");
    }
}
