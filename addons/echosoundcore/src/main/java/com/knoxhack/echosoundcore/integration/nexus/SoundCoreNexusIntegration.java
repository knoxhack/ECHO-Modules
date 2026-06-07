package com.knoxhack.echosoundcore.integration.nexus;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreNexusIntegration {
    private SoundCoreNexusIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore Nexus Protocol integration registered.");
        // Future: provide Nexus ambience and music hooks.
    }
}
