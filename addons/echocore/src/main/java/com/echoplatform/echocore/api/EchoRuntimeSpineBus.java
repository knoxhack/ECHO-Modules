package com.echoplatform.echocore.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class EchoRuntimeSpineBus {
    private static final List<EchoRuntimeSpineEvent> GLOBAL_EVENTS = new ArrayList<>();
    private static final List<Predicate<EchoRuntimeSpineEvent>> GATES = new ArrayList<>();
    private static final List<Consumer<EchoRuntimeSpineEvent>> LISTENERS = new ArrayList<>();
    private final List<EchoRuntimeSpineEvent> events = new ArrayList<>();

    public static synchronized boolean publish(EchoRuntimeSpineEvent event) {
        if (event == null) {
            return false;
        }
        for (Predicate<EchoRuntimeSpineEvent> gate : GATES) {
            if (gate != null && !gate.test(event)) {
                return false;
            }
        }
        GLOBAL_EVENTS.add(event);
        for (Consumer<EchoRuntimeSpineEvent> listener : List.copyOf(LISTENERS)) {
            listener.accept(event);
        }
        return true;
    }

    public static synchronized AutoCloseable register(Consumer<EchoRuntimeSpineEvent> listener) {
        if (listener == null) {
            return () -> { };
        }
        LISTENERS.add(listener);
        return () -> unregister(listener);
    }

    private static synchronized void unregister(Consumer<EchoRuntimeSpineEvent> listener) {
        LISTENERS.remove(listener);
    }

    public static synchronized void registerGate(Predicate<EchoRuntimeSpineEvent> gate) {
        if (gate != null && !GATES.contains(gate)) {
            GATES.add(gate);
        }
    }

    public static synchronized void clearForTests() {
        GLOBAL_EVENTS.clear();
        GATES.clear();
        LISTENERS.clear();
    }

    public static synchronized List<EchoRuntimeSpineEvent> publishedEvents() {
        return List.copyOf(GLOBAL_EVENTS);
    }

    public void publishLocal(EchoRuntimeSpineEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    public List<EchoRuntimeSpineEvent> events() {
        return List.copyOf(events);
    }
}
