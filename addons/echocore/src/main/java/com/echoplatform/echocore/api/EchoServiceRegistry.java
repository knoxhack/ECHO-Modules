package com.echoplatform.echocore.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EchoServiceRegistry {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    public static <T> void register(Class<T> serviceType, T service) {
        SERVICES.put(serviceType, serviceType.cast(service));
    }

    public static <T> Optional<T> find(Class<T> serviceType) {
        return Optional.ofNullable(serviceType.cast(SERVICES.get(serviceType)));
    }

    public static <T> T require(Class<T> serviceType) {
        return find(serviceType).orElseThrow(() -> new IllegalStateException("Missing ECHO service: " + serviceType.getName()));
    }

    public static Map<Class<?>, Object> snapshot() {
        return Map.copyOf(SERVICES);
    }

    public static void clear() {
        SERVICES.clear();
    }

    public static void withClearedForTests(Runnable body) {
        Map<Class<?>, Object> snapshot = Map.copyOf(SERVICES);
        SERVICES.clear();
        try {
            body.run();
        } finally {
            SERVICES.clear();
            SERVICES.putAll(snapshot);
        }
    }
}
