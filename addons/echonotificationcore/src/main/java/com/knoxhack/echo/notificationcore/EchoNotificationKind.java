package com.knoxhack.echo.notificationcore;

public enum EchoNotificationKind {
    TOAST("toast"),
    MISSION_UPDATE("mission_update"),
    SYSTEM_ALERT("system_alert"),
    HAZARD_WARNING("hazard_warning"),
    FACTION_REPUTATION("faction_reputation"),
    ITEM_UNLOCK("item_unlock"),
    TUTORIAL_HINT("tutorial_hint"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoNotificationKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
