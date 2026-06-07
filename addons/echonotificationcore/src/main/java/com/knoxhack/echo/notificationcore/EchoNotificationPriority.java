package com.knoxhack.echo.notificationcore;

public enum EchoNotificationPriority {
    LOW("low", 10),
    NORMAL("normal", 20),
    HIGH("high", 30),
    CRITICAL("critical", 40),
    BLOCKING("blocking", 50),
    UNKNOWN("unknown", 0);

    private final String serializedName;
    private final int rank;

    EchoNotificationPriority(String serializedName, int rank) {
        this.serializedName = serializedName;
        this.rank = rank;
    }

    public String serializedName() {
        return serializedName;
    }

    public int rank() {
        return rank;
    }
}
