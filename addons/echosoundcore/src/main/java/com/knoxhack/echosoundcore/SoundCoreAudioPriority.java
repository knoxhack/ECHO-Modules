package com.knoxhack.echosoundcore;

public enum SoundCoreAudioPriority {
    IDLE(0),
    BASE(1),
    BIOME(2),
    STRUCTURE(3),
    MISSION(4),
    COMBAT(5),
    SIEGE(6),
    BOSS(7),
    SCRIPTED(8);

    private final int weight;

    SoundCoreAudioPriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean overrides(SoundCoreAudioPriority other) {
        return this.weight > other.weight;
    }
}
