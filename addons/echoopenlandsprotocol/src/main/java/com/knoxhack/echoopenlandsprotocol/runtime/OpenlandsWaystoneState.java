package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.Arrays;
import java.util.List;

public enum OpenlandsWaystoneState {
    UNDISCOVERED("undiscovered"),
    DISCOVERED("discovered"),
    DEBRIS_CLEARED("debris_cleared"),
    STONE_REPAIRED("stone_repaired"),
    FITTED("fitted"),
    CHARGED("charged"),
    BOUND("bound"),
    ACTIVE("active");

    private final String id;

    OpenlandsWaystoneState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public OpenlandsWaystoneState next() {
        return switch (this) {
            case UNDISCOVERED -> DISCOVERED;
            case DISCOVERED -> DEBRIS_CLEARED;
            case DEBRIS_CLEARED -> STONE_REPAIRED;
            case STONE_REPAIRED -> FITTED;
            case FITTED -> CHARGED;
            case CHARGED -> BOUND;
            case BOUND -> ACTIVE;
            case ACTIVE -> null;
        };
    }

    public boolean active() {
        return this == ACTIVE;
    }

    public static OpenlandsWaystoneState fromId(String id) {
        return Arrays.stream(values())
                .filter(state -> state.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Openlands waystone state: " + id));
    }

    public static List<String> stateIds() {
        return Arrays.stream(values()).map(OpenlandsWaystoneState::id).toList();
    }
}
