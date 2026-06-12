package com.knoxhack.echoprimecore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;

public final class PrimeTerminalBridge {
    private PrimeTerminalBridge() {
    }

    public static void register(PrimeIntegrationRegistry registry) {
        for (PrimeTerminalRegistry.PrimeTerminalCard card : registry.cards()) {
            EchoCoreServices.terminalService().registerDashboardCard(card.id());
        }
    }
}
