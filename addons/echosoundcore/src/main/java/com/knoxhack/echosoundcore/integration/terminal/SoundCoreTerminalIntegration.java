package com.knoxhack.echosoundcore.integration.terminal;

import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.api.SoundCoreApi;

public final class SoundCoreTerminalIntegration {
    private SoundCoreTerminalIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore Terminal integration registered.");
        // Future: register terminal open/close hooks to play UI sounds and set terminal context.
    }
}
