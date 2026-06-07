package com.knoxhack.echo.eventcore;

public record EchoWorldEventId(String value) {
    public EchoWorldEventId {
        value = EventContractGuards.id(value, "world event id");
    }

    public static EchoWorldEventId of(String value) {
        return new EchoWorldEventId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
