package com.knoxhack.echosoundcore.api;

import com.knoxhack.echosoundcore.SoundCoreAudioPriority;
import com.knoxhack.echosoundcore.SoundCoreChapter;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import net.minecraft.resources.Identifier;

public record SoundCoreMusicProfile(
    Identifier id,
    Identifier sound,
    SoundCoreAudioPriority priority,
    String category,
    SoundCoreChapter chapter,
    Identifier biome,
    Identifier region,
    Identifier structure,
    Identifier faction,
    SoundCoreCombatIntensity combatIntensity,
    Identifier boss,
    int minDelay,
    int maxDelay,
    float fadeIn,
    float fadeOut,
    float weight,
    boolean conditions
) {
    public SoundCoreMusicProfile {
        if (id == null) throw new IllegalArgumentException("MusicProfile id cannot be null");
        if (sound == null) throw new IllegalArgumentException("MusicProfile sound cannot be null");
        if (priority == null) priority = SoundCoreAudioPriority.IDLE;
        if (chapter == null) chapter = SoundCoreChapter.UNKNOWN;
        if (combatIntensity == null) combatIntensity = SoundCoreCombatIntensity.NONE;
        if (minDelay < 0) minDelay = 0;
        if (maxDelay < minDelay) maxDelay = minDelay;
        if (fadeIn < 0) fadeIn = 0;
        if (fadeOut < 0) fadeOut = 0;
        if (weight < 0) weight = 1.0f;
    }
}
