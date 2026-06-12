package com.echoplatform.echocore.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public enum NoOpDataService implements IDataService {
    INSTANCE;

    private final Map<Identifier, IDataKey<?>> keys = new ConcurrentHashMap<>();

    @Override
    public <T> IDataKey<T> registerKey(IDataKey<T> key) {
        if (key != null && key.id() != null) {
            @SuppressWarnings("unchecked")
            IDataKey<T> existing = (IDataKey<T>) keys.putIfAbsent(key.id(), key);
            return existing == null ? key : existing;
        }
        return key;
    }

    @Override
    public Optional<IDataKey<?>> key(Identifier id) {
        return Optional.ofNullable(id == null ? null : keys.get(id));
    }

    @Override
    public Map<Identifier, DataKeyMetadata> allKeyMetadata() {
        Map<Identifier, DataKeyMetadata> metadata = new LinkedHashMap<>();
        keys.forEach((id, key) -> metadata.put(id, key.metadata()));
        return Map.copyOf(metadata);
    }

    @Override
    public List<IDataKey<?>> registeredKeys() {
        return keys.values().stream()
                .sorted(Comparator.comparing(key -> key.id().toString()))
                .toList();
    }

    @Override
    public DataServiceDiagnostics diagnostics() {
        return DataServiceDiagnostics.unavailable();
    }

    public void clearRegisteredKeysForTests() {
        keys.clear();
    }
}
