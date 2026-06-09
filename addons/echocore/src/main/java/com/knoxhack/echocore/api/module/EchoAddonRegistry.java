package com.knoxhack.echocore.api.module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoAddonRegistry {
    private final Map<String, EchoDiscoveryEntry> discoveries = new LinkedHashMap<>();

    public void registerDiscovery(EchoDiscoveryEntry entry) {
        discoveries.put(entry.id(), entry);
    }

    public Optional<EchoDiscoveryEntry> findDiscovery(String id) {
        return Optional.ofNullable(discoveries.get(id));
    }

    public Collection<EchoDiscoveryEntry> discoveries() {
        return List.copyOf(discoveries.values());
    }
}
