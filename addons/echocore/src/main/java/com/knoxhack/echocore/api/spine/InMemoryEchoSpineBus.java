package com.knoxhack.echocore.api.spine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryEchoSpineBus implements EchoSpineBus {
    private final Map<String, CopyOnWriteArrayList<EchoSpineSubscriber>> subscribers = new ConcurrentHashMap<>();

    @Override
    public AutoCloseable subscribe(String channel, EchoSpineSubscriber subscriber) {
        subscribers.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>()).add(subscriber);
        return () -> subscribers.getOrDefault(channel, new CopyOnWriteArrayList<>()).remove(subscriber);
    }

    @Override
    public void publish(EchoSpineEvent event) {
        List<EchoSpineSubscriber> channelSubscribers = subscribers.getOrDefault(event.channel(), new CopyOnWriteArrayList<>());
        for (EchoSpineSubscriber subscriber : channelSubscribers) {
            subscriber.onEchoEvent(event);
        }
        for (EchoSpineSubscriber subscriber : subscribers.getOrDefault("*", new CopyOnWriteArrayList<>())) {
            subscriber.onEchoEvent(event);
        }
    }
}
