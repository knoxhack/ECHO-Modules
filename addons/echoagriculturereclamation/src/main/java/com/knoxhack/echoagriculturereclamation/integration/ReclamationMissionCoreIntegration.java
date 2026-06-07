package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class ReclamationMissionCoreIntegration {
    private ReclamationMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoAgricultureReclamation.MODID, ReclamationMissionProvider.INSTANCE);
        ReclamationMissionHooks.registerCoverage();
    }
}
