package com.knoxhack.echotutorialcore.api;

public enum TutorialConditionType {
    PROGRESS,
    MISSING_PROGRESS,
    MOD_LOADED,
    MOD_MISSING,
    GUIDE_MODE,
    HAS_ITEM,
    MISSING_ITEM,
    HAS_TAG,
    MISSING_TAG,
    ACTIVE_HAZARD,
    ACTIVE_REGION,
    MISSION_STATE,
    POWER_ALERT,
    MISTAKE,
    MISTAKE_COUNT,
    REPEATED_DEATH_COUNT,
    TIME_SINCE_PROGRESS_MINUTES,
    TERMINAL_UNUSED_MINUTES,
    SCANNER_UNUSED_MINUTES,
    HOLOMAP_UNUSED_MINUTES,
    LENS_UNUSED_MINUTES;

    public static TutorialConditionType byName(String name) {
        if (name != null) {
            String clean = name.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
            for (TutorialConditionType type : values()) {
                if (type.name().equals(clean)) {
                    return type;
                }
            }
        }
        return PROGRESS;
    }
}
