package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class ConvoyMissionCoreIntegration {
    private ConvoyMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoConvoyProtocol.MODID, ConvoyMissionProvider.INSTANCE);
        ConvoyMissionHooks.registerCoverage();
    }
}
