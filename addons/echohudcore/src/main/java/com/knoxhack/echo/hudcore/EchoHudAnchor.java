package com.knoxhack.echo.hudcore;

public enum EchoHudAnchor {
    TOP_LEFT("top_left"),
    TOP_CENTER("top_center"),
    TOP_RIGHT("top_right"),
    CENTER_LEFT("center_left"),
    CENTER_RIGHT("center_right"),
    BOTTOM_LEFT("bottom_left"),
    BOTTOM_CENTER("bottom_center"),
    BOTTOM_RIGHT("bottom_right"),
    MISSION_TRACKER("mission_tracker"),
    COMPASS("compass"),
    HAZARD_BAR("hazard_bar"),
    NOTIFICATION_STACK("notification_stack"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoHudAnchor(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
