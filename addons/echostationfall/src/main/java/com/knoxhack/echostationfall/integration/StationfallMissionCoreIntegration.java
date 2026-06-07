package com.knoxhack.echostationfall.integration;

import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class StationfallMissionCoreIntegration {
    private StationfallMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoStationfall.MODID, StationfallTerminalCommonIntegration.Provider.INSTANCE);
        StationfallMissionHooks.registerCoverage();
    }
}
