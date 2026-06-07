package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRegistryBridge {
    private final String moduleId;
    private final List<Map<String, Object>> registrations = new ArrayList<>();

    public EchoNativeRegistryBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoNativeRegistryBridge register(String registry, String id, String summary) {
        return register(registry, id, summary, Map.of());
    }

    public EchoNativeRegistryBridge register(
            String registry,
            String id,
            String summary,
            Map<String, Object> properties
    ) {
        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("registry", AdapterContractGuards.requireText(registry, "registry"));
        registration.put("id", AdapterContractGuards.requireText(id, "registration id"));
        registration.put("summary", AdapterContractGuards.optionalText(summary));
        if (properties != null && !properties.isEmpty()) {
            registration.putAll(Map.copyOf(properties));
        }
        registration.put("planned", true);
        registration.put("executionMode", "native_registry_host_registration");
        registration.put("nativeRegistryHostStatus", "PENDING_RUNTIME_HOST");
        registrations.add(registration);
        return this;
    }

    public EchoNativeRegistryBridge register(EchoRegistryContractSnapshot snapshot) {
        for (EchoBlockDefinition block : snapshot.blocks()) {
            register("blocks", block.id(), block.model());
        }
        for (EchoItemDefinition item : snapshot.items()) {
            register("items", item.id(), item.model());
        }
        for (EchoEntityDefinition entity : snapshot.entities()) {
            register("entities", entity.id(), entity.model());
        }
        for (EchoRecipeDefinition recipe : snapshot.recipes()) {
            register("recipes", recipe.id(), recipe.type());
        }
        for (EchoLootDefinition lootTable : snapshot.lootTables()) {
            register("lootTables", lootTable.id(), lootTable.source());
        }
        for (EchoSoundDefinition sound : snapshot.sounds()) {
            register("sounds", sound.id(), sound.subtitle());
        }
        for (EchoStructureDefinition structure : snapshot.structures()) {
            register("structures", structure.id(), structure.kind());
        }
        for (EchoTagDefinition tag : snapshot.tags()) {
            register("tags", tag.id(), tag.kind());
        }
        for (EchoCreativeContentGroup group : snapshot.creativeGroups()) {
            register("creativeGroups", group.id(), group.source());
        }
        return this;
    }

    public Map<String, Object> describe() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moduleId", moduleId);
        data.put("registrationCount", registrations.size());
        data.put("registrations", List.copyOf(registrations));
        data.put("bridge", "adaptercore.native_registry");
        data.put("executionMode", "native_registry_host_registration");
        data.put("nativeRegistryHostStatus", registrations.isEmpty() ? "NO_REGISTRATIONS" : "PENDING_RUNTIME_HOST");
        data.put("summary", "Native registry bridge declares logical registrations; runtime status is supplied by the Native Loader registry host.");
        return data;
    }
}
