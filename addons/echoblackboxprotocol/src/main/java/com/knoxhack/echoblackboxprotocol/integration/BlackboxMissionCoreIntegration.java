package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class BlackboxMissionCoreIntegration {
    private BlackboxMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoBlackboxProtocol.MODID, BlackboxMissionProvider.INSTANCE);
        BlackboxMissionHooks.registerCoverage();
    }
}
