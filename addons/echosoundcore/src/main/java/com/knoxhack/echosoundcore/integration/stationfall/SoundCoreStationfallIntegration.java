package com.knoxhack.echosoundcore.integration.stationfall;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreStationfallIntegration {
    private SoundCoreStationfallIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore Stationfall integration registered.");
        // Future: provide Stationfall horror ambience hooks.
    }
}
