package com.knoxhack.echomissioncore.integration;

import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;

public final class MissionCoreTerminalIntegration {
    private static boolean registered;

    private MissionCoreTerminalIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        TerminalMissionRegistry.register(MissionCoreTerminalProvider.INSTANCE);
        TerminalMissionActions.registerForTab(MissionCoreTerminalProvider.CHAPTER_ID);
    }
}
