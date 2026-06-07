package com.knoxhack.echo.eventcore;

public enum EchoEventScheduleKind {
    ALWAYS_AVAILABLE("always_available"),
    INTERVAL("interval"),
    WINDOWED("windowed"),
    COOLDOWN("cooldown"),
    CAMPAIGN_PHASE("campaign_phase"),
    PACK_VARIANT("pack_variant"),
    SERVER_POLICY("server_policy"),
    MANUAL("manual"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoEventScheduleKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
