package com.knoxhack.echosoundcore.util;

import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.client.ambience.SoundCoreAmbienceManager;
import com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager;

public final class SoundCoreDebug {
    private SoundCoreDebug() {}

    public static void printStatus() {
        EchoSoundCore.LOGGER.info("ECHO SoundCore Debug Status:");
        EchoSoundCore.LOGGER.info("  Current Music Track: {}", SoundCoreMusicManager.currentTrackId());
        EchoSoundCore.LOGGER.info("  Current Priority: {}", SoundCoreMusicManager.currentPriority());
        EchoSoundCore.LOGGER.info("  Active Ambience Loops: {}", SoundCoreAmbienceManager.activeLoops().size());
    }
}
