package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryLogisticsIntegration {
    private RecoveryLogisticsIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerRemoteDeliveryProvider((player, snapshot) -> RecoveryConfig.REMOTE_RECOVERY_ENABLED.get()
                ? Optional.of("Logistics delivery request accepted for recovery cache " + snapshot.graveId())
                : Optional.empty());
        EchoRecovery.LOGGER.info("Recovery Logistics remote-delivery provider registered.");
    }
}
