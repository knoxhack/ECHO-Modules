package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryNexusIntegration {
    private RecoveryNexusIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerSignalProvider((player, snapshot) -> snapshot.contaminated()
                ? Optional.of("Nexus corruption may create false-marker noise near this cache.")
                : Optional.empty());
        EchoRecovery.LOGGER.info("Recovery Nexus signal annotations registered.");
    }
}
