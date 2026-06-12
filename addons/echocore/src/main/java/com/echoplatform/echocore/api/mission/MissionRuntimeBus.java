package com.echoplatform.echocore.api.mission;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class MissionRuntimeBus {
    private static final List<Consumer<MissionRuntimeEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    private MissionRuntimeBus() {
    }

    public static AutoCloseable register(Consumer<MissionRuntimeEvent> listener) {
        if (listener == null) {
            return () -> {
            };
        }
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    public static void fire(MissionRuntimeEvent event) {
        if (event == null) {
            return;
        }
        for (Consumer<MissionRuntimeEvent> listener : LISTENERS) {
            listener.accept(event);
        }
    }

    public static void clearForTests() {
        LISTENERS.clear();
    }
}
