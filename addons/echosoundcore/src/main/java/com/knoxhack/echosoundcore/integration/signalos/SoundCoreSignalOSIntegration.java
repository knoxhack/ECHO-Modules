package com.knoxhack.echosoundcore.integration.signalos;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreSignalOSIntegration {
    private SoundCoreSignalOSIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore SignalOS integration registered.");
        // Future: expose SignalOS UI sounds.
    }
}
