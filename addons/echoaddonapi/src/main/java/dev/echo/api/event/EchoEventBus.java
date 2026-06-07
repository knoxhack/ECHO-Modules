package dev.echo.api.event;

import java.util.function.Consumer;

public interface EchoEventBus {
    <T extends EchoEvent> EchoEventSubscription subscribe(EchoEventType eventType, EchoEventPriority priority, Consumer<T> listener);

    void publish(EchoEvent event);
}
