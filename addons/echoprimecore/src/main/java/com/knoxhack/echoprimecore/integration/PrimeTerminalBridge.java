package com.knoxhack.echoprimecore.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;

public final class PrimeTerminalBridge {
    private PrimeTerminalBridge() {
    }

    public static void register(PrimeIntegrationRegistry registry) {
        for (PrimeTerminalRegistry.PrimeTerminalCard card : registry.cards()) {
            EchoCoreServices.terminalService().registerDashboardCard(card.id());
        }
    }
}
