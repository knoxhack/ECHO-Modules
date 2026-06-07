package dev.echo.api.event;

public record EchoEventSubscription(EchoEventType eventType, EchoEventPriority priority) {
}
