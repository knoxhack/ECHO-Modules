package com.knoxhack.echocore.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EchoServiceRegistry {
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public <T> void register(Class<T> serviceType, T service) {
        services.put(serviceType, serviceType.cast(service));
    }

    public <T> Optional<T> find(Class<T> serviceType) {
        return Optional.ofNullable(serviceType.cast(services.get(serviceType)));
    }

    public <T> T require(Class<T> serviceType) {
        return find(serviceType).orElseThrow(() -> new IllegalStateException("Missing ECHO service: " + serviceType.getName()));
    }

    public Map<Class<?>, Object> snapshot() {
        return Map.copyOf(services);
    }

    public void clear() {
        services.clear();
    }
}
