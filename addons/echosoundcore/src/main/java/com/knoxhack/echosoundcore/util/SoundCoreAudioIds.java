package com.knoxhack.echosoundcore.util;

import com.knoxhack.echosoundcore.EchoSoundCore;
import net.minecraft.resources.Identifier;

public final class SoundCoreAudioIds {
    private SoundCoreAudioIds() {
    }

    public static boolean isSoundCoreMusic(Identifier id) {
        return id != null && EchoSoundCore.MODID.equals(id.getNamespace()) && id.getPath().startsWith("music.");
    }

    public static boolean matchesControlledMusicStop(Identifier requested, Identifier currentTrackId, Identifier currentSoundId) {
        return requested != null
                && (requested.equals(currentTrackId)
                || requested.equals(currentSoundId)
                || isSoundCoreMusic(requested));
    }
}
