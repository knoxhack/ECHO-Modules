package com.knoxhack.echoruntimeguard.api;

public enum ParticlePriority {
    CRITICAL,
    WARNING,
    GAMEPLAY,
    NEARBY_MACHINE,
    AMBIENT,
    DECORATIVE,
    FAR_DECORATIVE;

    public boolean protectedSignal() {
        return this == CRITICAL || this == WARNING || this == GAMEPLAY;
    }
}
