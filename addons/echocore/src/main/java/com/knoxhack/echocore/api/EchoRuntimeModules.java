package com.knoxhack.echocore.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoRuntimeModules {
    private final Map<String, EchoRuntimeModule> modules = new LinkedHashMap<>();

    public void register(EchoRuntimeModule module) {
        modules.put(module.id(), module);
    }

    public Optional<EchoRuntimeModule> find(String moduleId) {
        return Optional.ofNullable(modules.get(moduleId));
    }

    public boolean isLoaded(String moduleId) {
        return modules.containsKey(moduleId);
    }

    public Collection<EchoRuntimeModule> all() {
        return List.copyOf(modules.values());
    }

    public record EchoRuntimeModule(String id, String version, String side, boolean official) {
    }
}
