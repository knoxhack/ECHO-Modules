package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class NexusMissionCoreIntegration {
    private NexusMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoNexusProtocol.MODID, NexusTerminalMissionProvider.INSTANCE);
        NexusMissionHooks.registerCoverage();
    }
}
