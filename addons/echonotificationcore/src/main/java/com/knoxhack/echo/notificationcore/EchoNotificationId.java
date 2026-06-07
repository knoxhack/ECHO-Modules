package com.knoxhack.echo.notificationcore;

public record EchoNotificationId(String value) {
    public EchoNotificationId {
        value = NotificationContractGuards.id(value, "notification id");
    }

    public static EchoNotificationId of(String value) {
        return new EchoNotificationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
