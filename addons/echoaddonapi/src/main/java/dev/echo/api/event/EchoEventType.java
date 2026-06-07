package dev.echo.api.event;

import java.util.Objects;

public record EchoEventType(String value) {
    public EchoEventType {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("event type must not be blank");
        }
    }
}
