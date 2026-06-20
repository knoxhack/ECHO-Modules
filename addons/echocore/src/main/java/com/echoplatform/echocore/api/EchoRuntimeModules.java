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
        return MODULES.containsKey(moduleId) || isNativeModuleLoaded(moduleId) || isNeoForgeModLoaded(moduleId);
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
        boolean loaded = isLoaded(id);
        return new EchoRuntimeModule(id == null ? "" : id, "dev", "runtime", loaded);
    }

    public Collection<EchoRuntimeModule> all() {
        return List.copyOf(MODULES.values());
    }

    public record EchoRuntimeModule(String id, String version, String side, boolean official) {
        public String displayName() {
            return id == null ? "" : id;
        }
    }

    private static boolean isNeoForgeModLoaded(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return false;
        }
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object loaded = modListClass.getMethod("isLoaded", String.class).invoke(modList, moduleId);
            return Boolean.TRUE.equals(loaded);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static boolean isNativeModuleLoaded(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return false;
        }
        String nativeModuleIds = System.getProperty("echo.native.moduleIds", "");
        if (nativeModuleIds.isBlank()) {
            return false;
        }
        for (String token : nativeModuleIds.split("[,;\\s]+")) {
            if (moduleId.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }
}
