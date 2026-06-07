package com.knoxhack.echosoundcore.integration.powergrid;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCorePowerGridIntegration {
    private SoundCorePowerGridIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore PowerGrid integration registered.");
        // Future: expose breaker/brownout/overload/power restored sounds.
    }
}
