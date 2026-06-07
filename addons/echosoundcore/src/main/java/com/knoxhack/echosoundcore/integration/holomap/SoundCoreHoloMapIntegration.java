package com.knoxhack.echosoundcore.integration.holomap;

import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreHoloMapIntegration {
    private SoundCoreHoloMapIntegration() {}

    public static void register() {
        EchoSoundCore.LOGGER.info("SoundCore HoloMap integration registered.");
        // Future: expose map open/close/waypoint/route sounds.
    }
}
