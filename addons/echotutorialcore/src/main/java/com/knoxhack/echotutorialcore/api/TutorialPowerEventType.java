package com.knoxhack.echotutorialcore.api;

public enum TutorialPowerEventType {
    NO_POWER,
    BROWNOUT,
    OVERLOAD,
    BREAKER_TRIPPED;

    public static TutorialPowerEventType byName(String name) {
        if (name != null) {
            String clean = name.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
            if ("TRIPPED".equals(clean) || "BREAKER".equals(clean)) {
                return BREAKER_TRIPPED;
            }
            for (TutorialPowerEventType type : values()) {
                if (type.name().equals(clean)) {
                    return type;
                }
            }
        }
        return NO_POWER;
    }
}
