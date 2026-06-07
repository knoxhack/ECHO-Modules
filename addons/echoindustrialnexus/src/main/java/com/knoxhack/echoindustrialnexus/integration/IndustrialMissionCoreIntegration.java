package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class IndustrialMissionCoreIntegration {
    private IndustrialMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoIndustrialNexus.MODID, IndustrialMissionProvider.INSTANCE);
        IndustrialMissionHooks.registerCoverage();
    }
}
