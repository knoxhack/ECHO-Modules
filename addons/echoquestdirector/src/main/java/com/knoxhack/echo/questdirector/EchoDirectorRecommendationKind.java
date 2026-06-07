package com.knoxhack.echo.questdirector;

public enum EchoDirectorRecommendationKind {
    START_MISSION("start_mission"),
    SURFACE_REMINDER("surface_reminder"),
    DELAY_EVENT("delay_event"),
    START_EVENT("start_event"),
    REDUCE_PRESSURE("reduce_pressure"),
    INCREASE_PRESSURE("increase_pressure"),
    OFFER_RECOVERY_HELP("offer_recovery_help"),
    SUGGEST_ROUTE("suggest_route"),
    DEFER_OPTIONAL_CONTENT("defer_optional_content"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoDirectorRecommendationKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
