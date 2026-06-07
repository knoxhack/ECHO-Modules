package com.knoxhack.echosoundcore.integration.lens;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreLensIntegration {
    private SoundCoreLensIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore Lens integration registered.");
        // Future: expose scan sound helpers.
    }
}
