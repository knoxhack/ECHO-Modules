package com.knoxhack.echoruntimeguard.api;

public enum TickPriority {
    CRITICAL,
    GAMEPLAY,
    ACTIVE_MACHINE,
    UI_OPEN,
    BACKGROUND,
    DECORATIVE,
    DEBUG;

    public boolean alwaysRun() {
        return this == CRITICAL || this == GAMEPLAY || this == UI_OPEN;
    }

    public boolean nonCritical() {
        return this == BACKGROUND || this == DECORATIVE || this == DEBUG;
    }
}
