package com.knoxhack.echoruntimeguard.api;

public interface RuntimeGuardAwareBlockEntity {
    default boolean canRuntimeGuardSleep() {
        return true;
    }

    default TickPriority runtimeGuardPriority() {
        return TickPriority.BACKGROUND;
    }
}
