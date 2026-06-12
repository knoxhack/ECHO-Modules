package com.echoplatform.echocore.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EchoDataBus {
    private static final List<Consumer<DataChangeMessage>> SUBSCRIBERS = new CopyOnWriteArrayList<>();

    private EchoDataBus() {
    }

    public static AutoCloseable subscribe(Consumer<DataChangeMessage> subscriber) {
        if (subscriber == null) {
            return () -> {
            };
        }
        SUBSCRIBERS.add(subscriber);
        return () -> SUBSCRIBERS.remove(subscriber);
    }

    public static void publish(DataChangeMessage message) {
        if (message == null) {
            return;
        }
        for (Consumer<DataChangeMessage> subscriber : SUBSCRIBERS) {
            subscriber.accept(message);
        }
    }

    public static void clearForTests() {
        SUBSCRIBERS.clear();
    }
}
