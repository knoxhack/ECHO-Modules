package com.knoxhack.echoashfallprotocol.endgame;

/**
 * Storm survival is now centralized in PostNexusEventHandler so the DESTROY path
 * cannot double-count the same storm from two event subscribers.
 */
public final class StormSurvivalTracker {
    private StormSurvivalTracker() {}

    public static void onPlayerTick(Object event) {
        // Intentionally no-op.
    }
}
