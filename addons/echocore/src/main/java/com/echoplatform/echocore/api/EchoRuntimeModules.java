package com.echoplatform.echocore.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoRuntimeModules {
    private static final Map<String, EchoRuntimeModule> MODULES = new LinkedHashMap<>();

    public void register(EchoRuntimeModule module) {
        MODULES.put(module.id(), module);
    }

    public Optional<EchoRuntimeModule> find(String moduleId) {
        return Optional.ofNullable(MODULES.get(moduleId));
    }

    public static boolean isLoaded(String moduleId) {
        return MODULES.containsKey(moduleId);
    }

    public static void markLoaded(String moduleId, String displayName, String version) {
        if (moduleId == null || moduleId.isBlank()) {
            return;
        }
        MODULES.put(moduleId, new EchoRuntimeModule(moduleId, version == null || version.isBlank() ? "dev" : version, "runtime", true));
    }

    public static EchoRuntimeModule metadata(String moduleId, String fallbackName) {
        EchoRuntimeModule module = MODULES.get(moduleId);
        if (module != null) {
            return module;
        }
        String id = moduleId == null || moduleId.isBlank() ? fallbackName : moduleId;
        return new EchoRuntimeModule(id == null ? "" : id, "dev", "runtime", false);
    }

    public Collection<EchoRuntimeModule> all() {
        return List.copyOf(MODULES.values());
    }

    public record EchoRuntimeModule(String id, String version, String side, boolean official) {
        public String displayName() {
            return id == null ? "" : id;
        }
    }
}
