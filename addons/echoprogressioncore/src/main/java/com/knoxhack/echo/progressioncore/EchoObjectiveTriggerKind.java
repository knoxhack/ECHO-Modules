package com.knoxhack.echo.progressioncore;

public enum EchoObjectiveTriggerKind {
    WORLD_TRIGGER("world_trigger"),
    ENTITY_TRIGGER("entity_trigger"),
    ITEM_TRIGGER("item_trigger"),
    REGION_TRIGGER("region_trigger"),
    DIALOGUE_TRIGGER("dialogue_trigger"),
    RECIPE_TRIGGER("recipe_trigger"),
    FEATURE_TRIGGER("feature_trigger"),
    MISSION_TRIGGER("mission_trigger"),
    EVENT_TRIGGER("event_trigger"),
    TIMER_TRIGGER("timer_trigger"),
    MANUAL_REVIEW("manual_review"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoObjectiveTriggerKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
