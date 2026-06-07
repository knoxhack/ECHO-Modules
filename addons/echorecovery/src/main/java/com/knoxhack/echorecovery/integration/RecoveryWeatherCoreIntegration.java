package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import java.util.Optional;

public final class RecoveryWeatherCoreIntegration {
    private RecoveryWeatherCoreIntegration() {}
    public static void registerCommon() {
        RecoveryIntegrations.registerSignalProvider((player, snapshot) ->
                Optional.of("WeatherCore online; storms may reduce recovery signal confidence."));
        EchoRecovery.LOGGER.info("Recovery WeatherCore signal provider registered.");
    }
}
