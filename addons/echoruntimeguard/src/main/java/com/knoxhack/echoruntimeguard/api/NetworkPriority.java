package com.knoxhack.echoruntimeguard.api;

public enum NetworkPriority {
    CRITICAL,
    GAMEPLAY,
    UI_OPEN,
    BACKGROUND_SYNC,
    DECORATIVE,
    DEBUG;

    public boolean protectedSignal() {
        return this == CRITICAL || this == GAMEPLAY || this == UI_OPEN;
    }
}
