package com.knoxhack.echo.notificationcore;

public enum EchoNotificationDeliveryState {
    QUEUED("queued"),
    READY("ready"),
    DELIVERED("delivered"),
    ACKNOWLEDGED("acknowledged"),
    EXPIRED("expired"),
    DISMISSED("dismissed"),
    SUPPRESSED("suppressed"),
    FAILED("failed"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoNotificationDeliveryState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean terminal() {
        return this == ACKNOWLEDGED || this == EXPIRED || this == DISMISSED || this == SUPPRESSED || this == FAILED;
    }
}
