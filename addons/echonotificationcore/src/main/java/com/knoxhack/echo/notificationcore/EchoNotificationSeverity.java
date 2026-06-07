package com.knoxhack.echo.notificationcore;

public enum EchoNotificationSeverity {
    INFO("info", false),
    SUCCESS("success", false),
    WARNING("warning", false),
    DANGER("danger", true),
    CRITICAL("critical", true),
    TUTORIAL("tutorial", false),
    DISCOVERY("discovery", false),
    UNKNOWN("unknown", false);

    private final String serializedName;
    private final boolean attentionRequired;

    EchoNotificationSeverity(String serializedName, boolean attentionRequired) {
        this.serializedName = serializedName;
        this.attentionRequired = attentionRequired;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean attentionRequired() {
        return attentionRequired;
    }
}
