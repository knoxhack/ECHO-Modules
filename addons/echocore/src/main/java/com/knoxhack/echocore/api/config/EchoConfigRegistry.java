package com.knoxhack.echocore.api.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EchoConfigRegistry {
    private final Map<String, EchoConfigProvider> providers = new LinkedHashMap<>();

    public void register(String moduleId, EchoConfigProvider provider) {
        providers.put(moduleId, provider);
    }

    public Optional<EchoConfigProvider> find(String moduleId) {
        return Optional.ofNullable(providers.get(moduleId));
    }

    public Collection<EchoConfigModule> snapshots() {
        return providers.values().stream().map(EchoConfigProvider::describeConfig).toList();
    }
}
