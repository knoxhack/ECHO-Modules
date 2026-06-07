package com.knoxhack.echoashfallprotocol.endgame;

import java.util.Locale;

public enum NexusRelayState {
    UNKNOWN,
    SCANNED,
    ACTIVE,
    STABILIZED,
    SEVERED,
    OVERRIDDEN,
    CORRUPTED;

    public boolean isResolved() {
        return this == STABILIZED || this == SEVERED || this == OVERRIDDEN;
    }

    public static NexusRelayState byName(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
