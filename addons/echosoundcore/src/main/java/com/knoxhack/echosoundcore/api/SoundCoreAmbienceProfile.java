package com.knoxhack.echosoundcore.api;

import com.knoxhack.echosoundcore.SoundCoreChapter;
import net.minecraft.resources.Identifier;

public record SoundCoreAmbienceProfile(
    Identifier id,
    Identifier sound,
    String layer,
    SoundCoreChapter chapter,
    Identifier biome,
    String hazard,
    Identifier structure,
    Identifier faction,
    boolean loop,
    float fadeIn,
    float fadeOut,
    float volume,
    float pitch
) {
    public SoundCoreAmbienceProfile {
        if (id == null) throw new IllegalArgumentException("AmbienceProfile id cannot be null");
        if (sound == null) throw new IllegalArgumentException("AmbienceProfile sound cannot be null");
        if (layer == null) layer = "default";
        if (chapter == null) chapter = SoundCoreChapter.UNKNOWN;
        if (fadeIn < 0) fadeIn = 0;
        if (fadeOut < 0) fadeOut = 0;
        if (volume < 0) volume = 1.0f;
        if (pitch < 0) pitch = 1.0f;
    }
}
