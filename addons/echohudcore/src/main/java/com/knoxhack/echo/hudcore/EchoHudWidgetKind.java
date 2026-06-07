package com.knoxhack.echo.hudcore;

public enum EchoHudWidgetKind {
    MISSION_TRACKER("mission_tracker"),
    COMPASS_INDICATOR("compass_indicator"),
    HAZARD_METER("hazard_meter"),
    NOTIFICATION_ANCHOR("notification_anchor"),
    STATUS_INDICATOR("status_indicator"),
    SYSTEM_READOUT("system_readout"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoHudWidgetKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
