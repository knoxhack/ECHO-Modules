package com.knoxhack.echo.socialcore;

public enum EchoDialogueConditionKind {
    ALWAYS("always"),
    CONTENT_AVAILABLE("content_available"),
    CONTENT_MISSING("content_missing"),
    FACTION_REPUTATION("faction_reputation"),
    FACTION_HOSTILITY("faction_hostility"),
    ALLIANCE_STATE("alliance_state"),
    PROGRESSION_UNLOCKED("progression_unlocked"),
    OBJECTIVE_COMPLETE("objective_complete"),
    MISSION_AVAILABLE("mission_available"),
    REGION_VISITED("region_visited"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDialogueConditionKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
