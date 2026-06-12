package com.echoplatform.echocore.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class InMemoryDataService implements IDataService {
    private final Map<Identifier, IDataKey<?>> keys = new ConcurrentHashMap<>();
    private final Map<UUID, MutableDataView> playerStores = new ConcurrentHashMap<>();
    private final Map<String, MutableDataView> worldStores = new ConcurrentHashMap<>();

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
    public DataServiceDiagnostics diagnostics() {
        return new DataServiceDiagnostics(true, keys.size(), playerStores.size(), worldStores.size(), "Data service online.");
    }

    @Override
    public Map<Identifier, DataKeyMetadata> allKeyMetadata() {
        Map<Identifier, DataKeyMetadata> metadata = new LinkedHashMap<>();
        keys.forEach((id, key) -> metadata.put(id, key.metadata()));
        return Map.copyOf(metadata);
    }

    @Override
    public java.util.List<IDataKey<?>> registeredKeys() {
        return keys.values().stream()
                .sorted(java.util.Comparator.comparing(key -> key.id().toString()))
                .toList();
    }

    @Override
    public IPlayerDataView player(Player player) {
        if (player == null) {
            return IDataService.DataView.EMPTY;
        }
        return playerStores.computeIfAbsent(player.getUUID(), ignored -> new MutableDataView());
    }

    @Override
    public IWorldDataView world(Level level) {
        if (level == null) {
            return IDataService.DataView.EMPTY;
        }
        String key = level.dimension().identifier().toString();
        return worldStores.computeIfAbsent(key, ignored -> new MutableDataView());
    }

    @Override
    public IDataSyncBridge syncBridge() {
        return new IDataSyncBridge() {
            @Override
            public void requestFullSync(ServerPlayer player) {
            }

            @Override
            public void markDirty(DataScope scope, String ownerId, Identifier keyId) {
            }

            @Override
            public long revision() {
                return 0L;
            }
        };
    }

    private static final class MutableDataView implements IDataService.DataView {
        private final Map<Identifier, Object> values = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(IDataKey<T> key) {
            if (key == null || key.id() == null) {
                return null;
            }
            return (T) values.getOrDefault(key.id(), key.defaultValue());
        }

        @Override
        public <T> boolean set(IDataKey<T> key, T value) {
            if (key != null && key.id() != null) {
                values.put(key.id(), value == null ? key.defaultValue() : value);
                return true;
            }
            return false;
        }

        @Override
        public boolean clear(IDataKey<?> key) {
            return key != null && key.id() != null && values.remove(key.id()) != null;
        }

        @Override
        public boolean has(IDataKey<?> key) {
            return key != null && key.id() != null && values.containsKey(key.id());
        }

        @Override
        public Map<Identifier, String> debugSnapshot() {
            Map<Identifier, String> snapshot = new LinkedHashMap<>();
            values.forEach((id, value) -> snapshot.put(id, String.valueOf(value)));
            return Map.copyOf(snapshot);
        }
    }
}
