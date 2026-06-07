package com.knoxhack.echosoundcore.integration.blackbox;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreBlackboxIntegration {
    private SoundCoreBlackboxIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore Blackbox integration registered.");
        // Future: provide Blackbox memory ambience hooks.
    }
}
