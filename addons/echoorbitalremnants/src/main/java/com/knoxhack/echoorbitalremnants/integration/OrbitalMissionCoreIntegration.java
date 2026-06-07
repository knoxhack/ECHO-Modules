package com.knoxhack.echoorbitalremnants.integration;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoterminal.api.mission.TerminalMissionCoreBridge;

public final class OrbitalMissionCoreIntegration {
    private OrbitalMissionCoreIntegration() {
    }

    public static void register() {
        TerminalMissionCoreBridge.registerProvider(EchoOrbitalRemnants.MODID, OrbitalMissionProvider.INSTANCE);
        OrbitalMissionHooks.registerCoverage();
    }
}
