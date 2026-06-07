package com.knoxhack.echodatacore;

import com.knoxhack.echocore.api.DataChangeMessage;
import com.knoxhack.echocore.api.DataChangeKind;
import com.knoxhack.echocore.api.DataKeyMetadata;
import com.knoxhack.echocore.api.DataScope;
import com.knoxhack.echocore.api.DataServiceDiagnostics;
import com.knoxhack.echocore.api.DataValueKind;
import com.knoxhack.echocore.api.EchoDataBus;
import com.knoxhack.echocore.api.IDataKey;
import com.knoxhack.echocore.api.IDataService;
import com.knoxhack.echocore.api.IDataSyncBridge;
import com.knoxhack.echocore.api.IPlayerDataView;
import com.knoxhack.echocore.api.ITeamDataView;
import com.knoxhack.echocore.api.IWorldDataView;
import com.knoxhack.echodatacore.legacy.DataCoreLegacyAdapters;
import com.knoxhack.echodatacore.network.DataCoreMetadataSyncPacket;
import com.knoxhack.echodatacore.network.DataCoreSyncPacket;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class DataCoreDataService implements IDataService {
    public static final int CURRENT_VERSION = 2;
    public static final String PLAYER_ROOT = "echodatacore";
    private static final String VALUES = "values";
    private static final String VALUE = "value";
    private static final String KIND = "kind";
    private static final String UPDATED = "updatedGameTime";
    private static final Identifier UNKNOWN_ID = Identifier.fromNamespaceAndPath(EchoDataCore.MODID, "unknown");

    public static final DataCoreDataService INSTANCE = new DataCoreDataService();

    private final Map<Identifier, IDataKey<?>> keys = new ConcurrentHashMap<>();
    private final Map<Identifier, DataKeyMetadata> metadata = new ConcurrentHashMap<>();
    private final Set<Identifier> datapackRegisteredKeys = ConcurrentHashMap.newKeySet();
    private final Map<UUID, LinkedHashSet<Identifier>> dirtyPlayerKeys = new ConcurrentHashMap<>();
    private final Map<String, LinkedHashSet<Identifier>> dirtyWorldKeys = new ConcurrentHashMap<>();
    private final Map<String, LinkedHashSet<Identifier>> dirtyTeamKeys = new ConcurrentHashMap<>();
    private final Map<String, Map<Identifier, CompoundTag>> clientPlayerValues = new ConcurrentHashMap<>();
    private final Map<String, Map<Identifier, CompoundTag>> clientWorldValues = new ConcurrentHashMap<>();
    private final Map<String, Map<Identifier, CompoundTag>> clientTeamValues = new ConcurrentHashMap<>();
    private final Map<Identifier, DataKeyMetadata> clientMetadata = new ConcurrentHashMap<>();
    private final List<String> recentChanges = java.util.Collections.synchronizedList(new ArrayList<>());
    private final DataCoreSyncBridge syncBridge = new DataCoreSyncBridge();
    private volatile long revision;
    private volatile long metadataRevision;
    private volatile int duplicateKeyConflicts;
    private volatile int metadataConflicts;
    private volatile int lastStaleDatapackMetadataRemoved;
    private volatile int lastMetadataSyncSize;

    private DataCoreDataService() {
    }

    @Override
    public <T> IDataKey<T> registerKey(IDataKey<T> key) {
        if (key == null) {
            throw new IllegalArgumentException("Data key is required.");
        }
        IDataKey<?> existing = keys.putIfAbsent(key.id(), key);
        if (existing == null) {
            metadata.putIfAbsent(key.id(), DataKeyMetadata.of(key, "java"));
            metadataRevision++;
            return key;
        }
        if (datapackRegisteredKeys.remove(key.id())) {
            DataKeyMetadata previous = metadata.get(key.id());
            keys.put(key.id(), key);
            metadata.put(key.id(), DataKeyMetadata.of(key, "java").merge(previous));
            metadataRevision++;
            return key;
        }
        if (existing.kind() != key.kind() || existing.scope() != key.scope()) {
            duplicateKeyConflicts++;
            EchoDataCore.LOGGER.warn("DataCore key {} already registered as {}/{}; keeping first definition.",
                    key.id(), existing.scope(), existing.kind());
        }
        @SuppressWarnings("unchecked")
        IDataKey<T> typed = (IDataKey<T>) existing;
        return typed;
    }

    @Override
    public Optional<IDataKey<?>> key(Identifier id) {
        return Optional.ofNullable(id == null ? null : keys.get(id));
    }

    @Override
    public List<IDataKey<?>> registeredKeys() {
        return keys.values().stream()
                .sorted(Comparator.comparing(dataKey -> dataKey.id().toString()))
                .toList();
    }

    @Override
    public Optional<DataKeyMetadata> keyMetadata(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        DataKeyMetadata meta = metadata.get(id);
        if (meta != null) {
            return Optional.of(meta);
        }
        DataKeyMetadata syncedMeta = clientMetadata.get(id);
        if (syncedMeta != null) {
            return Optional.of(syncedMeta);
        }
        return key(id).map(key -> DataKeyMetadata.of(key, "java"));
    }

    @Override
    public Map<Identifier, DataKeyMetadata> allKeyMetadata() {
        Map<Identifier, DataKeyMetadata> snapshot = new LinkedHashMap<>();
        registeredKeys().forEach(key -> snapshot.put(key.id(), keyMetadata(key.id())
                .orElseGet(() -> DataKeyMetadata.of(key, "java"))));
        metadata.values().stream()
                .sorted(Comparator.comparing(meta -> meta.id().toString()))
                .forEach(meta -> snapshot.putIfAbsent(meta.id(), meta));
        clientMetadata.values().stream()
                .sorted(Comparator.comparing(meta -> meta.id().toString()))
                .forEach(meta -> snapshot.putIfAbsent(meta.id(), meta));
        return snapshot;
    }

    @Override
    public DataServiceDiagnostics diagnostics() {
        List<IDataKey<?>> snapshot = registeredKeys();
        int dirtyOwners = dirtyPlayerKeys.size() + dirtyWorldKeys.size() + dirtyTeamKeys.size();
        List<String> changes;
        synchronized (recentChanges) {
            changes = List.copyOf(recentChanges);
        }
        return new DataServiceDiagnostics(
                true,
                getClass().getName(),
                revision,
                snapshot.size(),
                (int) snapshot.stream().filter(IDataKey::synced).count(),
                allKeyMetadata().size(),
                dirtyOwners,
                changes);
    }

    public void registerMetadata(DataKeyMetadata meta, boolean registerSimpleKey) {
        if (meta == null || meta.id() == null || meta.scope() == null || meta.kind() == null) {
            return;
        }
        IDataKey<?> existing = keys.get(meta.id());
        if (existing != null && (existing.scope() != meta.scope() || existing.kind() != meta.kind())) {
            metadataConflicts++;
            EchoDataCore.LOGGER.warn("DataCore metadata {} conflicts with Java key {}/{}; keeping Java contract.",
                    meta.id(), existing.scope(), existing.kind());
            metadata.put(meta.id(), DataKeyMetadata.of(existing, "java").merge(meta));
            metadataRevision++;
            return;
        }
        metadata.merge(meta.id(), meta, (first, second) -> first.merge(second));
        metadataRevision++;
        if (registerSimpleKey && existing == null) {
            if (meta.kind() == DataValueKind.RECORD) {
                EchoDataCore.LOGGER.warn("DataCore metadata {} requested RECORD registration without a Java codec; metadata only.",
                        meta.id());
                return;
            }
            registerKey(simpleKey(meta));
            datapackRegisteredKeys.add(meta.id());
        }
    }

    public void replaceDatapackMetadata(Map<Identifier, DataKeyMetadata> datapackMetadata) {
        Set<Identifier> incoming = datapackMetadata == null ? Set.of() : datapackMetadata.keySet();
        int removed = 0;
        for (Identifier id : List.copyOf(datapackRegisteredKeys)) {
            if (!incoming.contains(id)) {
                keys.remove(id);
                metadata.remove(id);
                datapackRegisteredKeys.remove(id);
                removed++;
            }
        }
        int metadataBefore = metadata.size();
        metadata.entrySet().removeIf(entry -> isDatapackSource(entry.getValue().source())
                && !incoming.contains(entry.getKey()));
        removed += Math.max(0, metadataBefore - metadata.size());
        lastStaleDatapackMetadataRemoved = removed;
        if (datapackMetadata != null) {
            datapackMetadata.values().forEach(meta -> registerMetadata(meta, true));
        }
        metadataRevision++;
    }

    public boolean hasStoredPlayerValue(Player player, Identifier keyId) {
        if (player == null || keyId == null) {
            return false;
        }
        return valuesRoot(playerRoot(player)).contains(keyId.toString());
    }

    public void recordLegacyMirror(Player player, IDataKey<?> key) {
        if (player == null || key == null) {
            return;
        }
        revision++;
        rememberChange(DataScope.PLAYER, player.getUUID().toString(), key.id(), DataChangeKind.LEGACY_MIRROR);
        EchoDataBus.publish(new DataChangeMessage(DataScope.PLAYER, player.getUUID().toString(), key.id(),
                key.kind(), revision, false, DataChangeKind.LEGACY_MIRROR));
    }

    public Map<String, Integer> dirtyOwnerCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("playerOwners", dirtyPlayerKeys.size());
        counts.put("worldOwners", dirtyWorldKeys.size());
        counts.put("teamOwners", dirtyTeamKeys.size());
        counts.put("totalOwners", dirtyPlayerKeys.size() + dirtyWorldKeys.size() + dirtyTeamKeys.size());
        return counts;
    }

    public Map<Identifier, String> debugClientSnapshot(DataScope scope, String ownerId) {
        Map<String, Map<Identifier, CompoundTag>> source = switch (scope == null ? DataScope.PLAYER : scope) {
            case PLAYER -> clientPlayerValues;
            case WORLD -> clientWorldValues;
            case TEAM -> clientTeamValues;
        };
        Map<Identifier, String> snapshot = new LinkedHashMap<>();
        source.getOrDefault(ownerId == null ? "" : ownerId, Map.of())
                .forEach((id, value) -> snapshot.put(id, debugValue(value)));
        return snapshot;
    }

    @Override
    public IPlayerDataView player(Player player) {
        return new PlayerView(player);
    }

    @Override
    public IWorldDataView world(Level level) {
        return new WorldView(level);
    }

    @Override
    public ITeamDataView team(Level level, Identifier teamId) {
        return new TeamView(level, teamId == null ? UNKNOWN_ID : teamId);
    }

    @Override
    public IDataSyncBridge syncBridge() {
        return syncBridge;
    }

    public void onPlayerLogin(Player playerEntity) {
        if (!(playerEntity instanceof ServerPlayer player)) {
            return;
        }
        migratePlayer(player);
        player(player).set(DataCoreBuiltinKeys.TERMINAL_PROBE, "online");
        player(player).set(DataCoreBuiltinKeys.PLAYER_SCHEMA_VERSION, (long) CURRENT_VERSION);
        sendMetadataSync(player);
        syncBridge.requestFullSync(player);
    }

    public void onPlayerClone(Player originalPlayer, Player newPlayer) {
        if (originalPlayer == null || newPlayer == null) {
            return;
        }
        CompoundTag original = originalPlayer.getPersistentData().getCompoundOrEmpty(PLAYER_ROOT);
        if (!original.isEmpty()) {
            newPlayer.getPersistentData().put(PLAYER_ROOT, original.copy());
        }
    }

    public void onPlayerTick(Player playerEntity) {
        if (!(playerEntity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        int interval = Math.max(1, Config.SYNC_INTERVAL_TICKS.get());
        if (player.tickCount % interval == 0) {
            syncBridge.flushDirty(player, false);
            if (player.level() instanceof ServerLevel serverLevel) {
                syncBridge.flushShared(serverLevel);
            }
        }
    }

    public void applyClientSync(DataCoreSyncPacket packet) {
        Map<String, Map<Identifier, CompoundTag>> target = switch (packet.scope()) {
            case PLAYER -> clientPlayerValues;
            case WORLD -> clientWorldValues;
            case TEAM -> clientTeamValues;
        };
        Map<Identifier, CompoundTag> ownerValues = target.computeIfAbsent(packet.ownerId(), ignored -> new ConcurrentHashMap<>());
        if (packet.fullSnapshot()) {
            ownerValues.clear();
        }
        for (DataCoreSyncPacket.Entry entry : packet.entries()) {
            if (entry.clear()) {
                ownerValues.remove(entry.keyId());
            } else {
                ownerValues.put(entry.keyId(), entry.data().copy());
            }
            EchoDataBus.publish(new DataChangeMessage(packet.scope(), packet.ownerId(), entry.keyId(),
                    entry.kind(), packet.revision(), packet.fullSnapshot(),
                    entry.clear() ? DataChangeKind.CLEAR
                            : packet.fullSnapshot() ? DataChangeKind.FULL_SNAPSHOT : DataChangeKind.SET));
        }
    }

    public void applyClientMetadataSync(DataCoreMetadataSyncPacket packet) {
        clientMetadata.clear();
        for (DataKeyMetadata meta : packet.metadata()) {
            clientMetadata.put(meta.id(), meta);
        }
        metadataRevision = Math.max(metadataRevision, packet.revision());
        lastMetadataSyncSize = clientMetadata.size();
    }

    public void sendMetadataSync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        List<DataKeyMetadata> visibleMetadata = allKeyMetadata().values().stream()
                .filter(DataKeyMetadata::synced)
                .sorted(Comparator.comparing(meta -> meta.id().toString()))
                .toList();
        lastMetadataSyncSize = visibleMetadata.size();
        EchoNetSend.toPlayer(player, new DataCoreMetadataSyncPacket(metadataRevision, visibleMetadata));
    }

    public void broadcastMetadataSync(Iterable<ServerPlayer> players) {
        if (players == null) {
            return;
        }
        for (ServerPlayer player : players) {
            sendMetadataSync(player);
        }
    }

    public int debugDirtyPlayerKeyCount(UUID playerId) {
        LinkedHashSet<Identifier> dirty = dirtyPlayerKeys.get(playerId);
        return dirty == null ? 0 : dirty.size();
    }

    public int duplicateKeyConflictCount() {
        return duplicateKeyConflicts;
    }

    public int metadataConflictCount() {
        return metadataConflicts;
    }

    public int datapackRegisteredKeyCount() {
        return datapackRegisteredKeys.size();
    }

    public int clientMetadataCount() {
        return clientMetadata.size();
    }

    public int lastStaleDatapackMetadataRemoved() {
        return lastStaleDatapackMetadataRemoved;
    }

    public int lastMetadataSyncSize() {
        return lastMetadataSyncSize;
    }

    public int dirtyKeyCount() {
        return dirtyPlayerKeys.values().stream().mapToInt(Set::size).sum()
                + dirtyWorldKeys.values().stream().mapToInt(Set::size).sum()
                + dirtyTeamKeys.values().stream().mapToInt(Set::size).sum();
    }

    private void migratePlayer(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(PLAYER_ROOT);
        int version = root.getIntOr("version", 0);
        if (version >= CURRENT_VERSION) {
            return;
        }
        root.putInt("version", CURRENT_VERSION);
        CompoundTag migrations = root.getCompoundOrEmpty("migrations");
        migrations.putInt(EchoDataCore.MODID, CURRENT_VERSION);
        root.put("migrations", migrations);
        player.getPersistentData().put(PLAYER_ROOT, root);
    }

    private CompoundTag clientValue(DataScope scope, String ownerId, Identifier keyId) {
        Map<String, Map<Identifier, CompoundTag>> source = switch (scope) {
            case PLAYER -> clientPlayerValues;
            case WORLD -> clientWorldValues;
            case TEAM -> clientTeamValues;
        };
        Map<Identifier, CompoundTag> values = source.get(ownerId);
        if (values == null) {
            return null;
        }
        CompoundTag entry = values.get(keyId);
        return entry == null ? null : entry.copy();
    }

    private static CompoundTag playerRoot(Player player) {
        return player == null ? new CompoundTag() : player.getPersistentData().getCompoundOrEmpty(PLAYER_ROOT);
    }

    private static CompoundTag valuesRoot(CompoundTag root) {
        return root.getCompoundOrEmpty(VALUES);
    }

    private static long gameTime(Player player) {
        return player == null || player.level() == null ? 0L : player.level().getGameTime();
    }

    private static long gameTime(Level level) {
        return level == null ? 0L : level.getGameTime();
    }

    private static <T> CompoundTag entryFor(IDataKey<T> key, T value, long gameTime) {
        CompoundTag entry = new CompoundTag();
        entry.putString(KIND, key.kind().name());
        entry.putLong(UPDATED, Math.max(0L, gameTime));
        T safeValue = value == null ? key.defaultValue() : value;
        try {
            entry.store(VALUE, key.codec(), NbtOps.INSTANCE, safeValue);
        } catch (RuntimeException exception) {
            EchoDataCore.LOGGER.warn("DataCore failed to encode key {}; storing default.", key.id(), exception);
            entry.store(VALUE, key.codec(), NbtOps.INSTANCE, key.defaultValue());
        }
        return entry;
    }

    private static <T> T decode(IDataKey<T> key, CompoundTag entry) {
        if (entry == null || !entry.contains(VALUE)) {
            return key.defaultValue();
        }
        try {
            Optional<T> decoded = entry.read(VALUE, key.codec(), NbtOps.INSTANCE);
            return decoded.orElse(key.defaultValue());
        } catch (RuntimeException exception) {
            EchoDataCore.LOGGER.warn("DataCore failed to decode key {}; using default.", key.id(), exception);
            return key.defaultValue();
        }
    }

    private static boolean sameStoredValue(CompoundTag existing, CompoundTag replacement) {
        if (existing == null || replacement == null) {
            return false;
        }
        Tag existingValue = existing.get(VALUE);
        Tag replacementValue = replacement.get(VALUE);
        return Objects.equals(existing.getStringOr(KIND, ""), replacement.getStringOr(KIND, ""))
                && Objects.equals(existingValue, replacementValue);
    }

    private static Map<Identifier, String> debugEntries(Map<String, CompoundTag> entries) {
        Map<Identifier, String> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, CompoundTag> entry : entries.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id != null) {
                snapshot.put(id, debugValue(entry.getValue()));
            }
        }
        return snapshot;
    }

    private static String debugValue(CompoundTag entry) {
        if (entry == null) {
            return "";
        }
        Tag value = entry.get(VALUE);
        return value == null ? entry.toString() : value.toString();
    }

    private static DataValueKind kindOf(IDataKey<?> key, CompoundTag entry) {
        if (key != null) {
            return key.kind();
        }
        try {
            return DataValueKind.valueOf(entry.getStringOr(KIND, DataValueKind.RECORD.name()));
        } catch (RuntimeException ignored) {
            return DataValueKind.RECORD;
        }
    }

    private static Identifier safeIdentifier(String value) {
        Identifier id = Identifier.tryParse(value == null ? "" : value);
        return id == null ? UNKNOWN_ID : id;
    }

    private static IDataKey<?> simpleKey(DataKeyMetadata meta) {
        return switch (meta.kind()) {
            case FLAG -> IDataKey.flag(meta.id(), meta.scope(), Boolean.parseBoolean(meta.defaultValue()), meta.synced());
            case COUNTER -> IDataKey.counter(meta.id(), meta.scope(), parseLong(meta.defaultValue()), meta.synced());
            case STRING -> IDataKey.string(meta.id(), meta.scope(), meta.defaultValue(), meta.synced());
            case ENUM -> IDataKey.enumName(meta.id(), meta.scope(), meta.defaultValue(), meta.synced());
            case RECORD -> IDataKey.record(meta.id(), meta.scope(), CompoundTag.CODEC, new CompoundTag(), meta.synced());
        };
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value == null || value.isBlank() ? "0" : value.strip());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static boolean scopeMatches(IDataKey<?> key, DataScope expected, String operation) {
        if (key == null) {
            return false;
        }
        if (key.scope() == expected) {
            return true;
        }
        EchoDataCore.LOGGER.warn("DataCore rejected {} {} through {} view; key scope is {}.",
                operation, key.id(), expected, key.scope());
        return false;
    }

    private static boolean isDatapackSource(String source) {
        return source != null && source.startsWith("datapack:");
    }

    private void rememberChange(DataScope scope, String ownerId, Identifier keyId, DataChangeKind kind) {
        String line = scope + " " + kind + " " + ownerId + " " + keyId + " @" + revision;
        synchronized (recentChanges) {
            recentChanges.add(line);
            while (recentChanges.size() > 12) {
                recentChanges.remove(0);
            }
        }
    }

    private final class PlayerView implements IPlayerDataView {
        private final Player player;

        private PlayerView(Player player) {
            this.player = player;
        }

        @Override
        public UUID playerId() {
            return player == null ? new UUID(0L, 0L) : player.getUUID();
        }

        @Override
        public <T> T get(IDataKey<T> key) {
            if (key == null) {
                return null;
            }
            if (!scopeMatches(key, DataScope.PLAYER, "read")) {
                return key.defaultValue();
            }
            registerKey(key);
            if (player == null) {
                return key.defaultValue();
            }
            if (player.level().isClientSide()) {
                return decode(key, clientValue(DataScope.PLAYER, player.getUUID().toString(), key.id()));
            }
            Optional<CompoundTag> legacy = DataCoreLegacyAdapters.read(player, key);
            if (legacy.isPresent()) {
                return decode(key, legacy.get());
            }
            return decode(key, valuesRoot(playerRoot(player)).getCompoundOrEmpty(key.id().toString()));
        }

        @Override
        public <T> boolean set(IDataKey<T> key, T value) {
            if (key == null || !scopeMatches(key, DataScope.PLAYER, "write")
                    || player == null || player.level().isClientSide()) {
                return false;
            }
            IDataKey<T> registered = registerKey(key);
            CompoundTag root = playerRoot(player);
            CompoundTag values = valuesRoot(root);
            CompoundTag replacement = entryFor(registered, value, gameTime(player));
            CompoundTag existing = values.getCompoundOrEmpty(registered.id().toString());
            if (sameStoredValue(existing, replacement)) {
                return false;
            }
            values.put(registered.id().toString(), replacement);
            root.put(VALUES, values);
            root.putInt("version", CURRENT_VERSION);
            player.getPersistentData().put(PLAYER_ROOT, root);
            dirtyPlayerKeys.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashSet<>()).add(registered.id());
            syncBridge.markDirty(DataScope.PLAYER, player.getUUID().toString(), registered.id());
            return true;
        }

        @Override
        public boolean clear(IDataKey<?> key) {
            if (key == null || !scopeMatches(key, DataScope.PLAYER, "clear")
                    || player == null || player.level().isClientSide()) {
                return false;
            }
            CompoundTag root = playerRoot(player);
            CompoundTag values = valuesRoot(root);
            if (!values.contains(key.id().toString())) {
                return false;
            }
            values.remove(key.id().toString());
            root.put(VALUES, values);
            player.getPersistentData().put(PLAYER_ROOT, root);
            dirtyPlayerKeys.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashSet<>()).add(key.id());
            syncBridge.markDirty(DataScope.PLAYER, player.getUUID().toString(), key.id(), true);
            return true;
        }

        @Override
        public boolean has(IDataKey<?> key) {
            if (key == null || !scopeMatches(key, DataScope.PLAYER, "has") || player == null) {
                return false;
            }
            if (player.level().isClientSide()) {
                return clientValue(DataScope.PLAYER, player.getUUID().toString(), key.id()) != null;
            }
            return DataCoreLegacyAdapters.read(player, key).isPresent()
                    || valuesRoot(playerRoot(player)).contains(key.id().toString());
        }

        @Override
        public CompoundTag record(Identifier id) {
            return get(IDataKey.record(id, DataScope.PLAYER, CompoundTag.CODEC, new CompoundTag(), true)).copy();
        }

        @Override
        public boolean putRecord(Identifier id, CompoundTag value) {
            return set(IDataKey.record(id, DataScope.PLAYER, CompoundTag.CODEC, new CompoundTag(), true),
                    value == null ? new CompoundTag() : value.copy());
        }

        @Override
        public Map<Identifier, String> debugSnapshot() {
            Map<Identifier, String> snapshot = new LinkedHashMap<>();
            if (player == null) {
                return snapshot;
            }
            if (!player.level().isClientSide()) {
                snapshot.putAll(DataCoreLegacyAdapters.snapshot(player));
                snapshot.putAll(debugEntries(compoundMap(valuesRoot(playerRoot(player)))));
                return snapshot;
            }
            Map<Identifier, CompoundTag> client = clientPlayerValues.getOrDefault(player.getUUID().toString(), Map.of());
            for (Map.Entry<Identifier, CompoundTag> entry : client.entrySet()) {
                snapshot.put(entry.getKey(), debugValue(entry.getValue()));
            }
            return snapshot;
        }
    }

    private final class WorldView implements IWorldDataView {
        private final Level level;

        private WorldView(Level level) {
            this.level = level;
        }

        @Override
        public Identifier dimensionId() {
            return level == null ? UNKNOWN_ID : level.dimension().identifier();
        }

        @Override
        public <T> T get(IDataKey<T> key) {
            if (key == null) {
                return null;
            }
            if (!scopeMatches(key, DataScope.WORLD, "read")) {
                return key.defaultValue();
            }
            registerKey(key);
            if (level instanceof ServerLevel serverLevel) {
                DataCoreWorldData data = DataCoreWorldData.get(serverLevel);
                data.ensureVersion();
                return decode(key, data.worldValue(key.id().toString()));
            }
            return decode(key, clientValue(DataScope.WORLD, dimensionId().toString(), key.id()));
        }

        @Override
        public <T> boolean set(IDataKey<T> key, T value) {
            if (key == null || !scopeMatches(key, DataScope.WORLD, "write")
                    || !(level instanceof ServerLevel serverLevel)) {
                return false;
            }
            IDataKey<T> registered = registerKey(key);
            CompoundTag entry = entryFor(registered, value, gameTime(level));
            boolean changed = DataCoreWorldData.get(serverLevel).putWorldValue(registered.id().toString(), entry);
            if (changed) {
                dirtyWorldKeys.computeIfAbsent(dimensionId().toString(), ignored -> new LinkedHashSet<>()).add(registered.id());
                syncBridge.markDirty(DataScope.WORLD, dimensionId().toString(), registered.id());
            }
            return changed;
        }

        @Override
        public boolean clear(IDataKey<?> key) {
            if (key == null || !scopeMatches(key, DataScope.WORLD, "clear")
                    || !(level instanceof ServerLevel serverLevel)) {
                return false;
            }
            boolean changed = DataCoreWorldData.get(serverLevel).removeWorldValue(key.id().toString());
            if (changed) {
                dirtyWorldKeys.computeIfAbsent(dimensionId().toString(), ignored -> new LinkedHashSet<>()).add(key.id());
                syncBridge.markDirty(DataScope.WORLD, dimensionId().toString(), key.id(), true);
            }
            return changed;
        }

        @Override
        public boolean has(IDataKey<?> key) {
            if (key == null || !scopeMatches(key, DataScope.WORLD, "has")) {
                return false;
            }
            if (level instanceof ServerLevel serverLevel) {
                return DataCoreWorldData.get(serverLevel).worldValue(key.id().toString()) != null;
            }
            return clientValue(DataScope.WORLD, dimensionId().toString(), key.id()) != null;
        }

        @Override
        public CompoundTag record(Identifier id) {
            return get(IDataKey.record(id, DataScope.WORLD, CompoundTag.CODEC, new CompoundTag(), true)).copy();
        }

        @Override
        public boolean putRecord(Identifier id, CompoundTag value) {
            return set(IDataKey.record(id, DataScope.WORLD, CompoundTag.CODEC, new CompoundTag(), true),
                    value == null ? new CompoundTag() : value.copy());
        }

        @Override
        public Map<Identifier, String> debugSnapshot() {
            if (level instanceof ServerLevel serverLevel) {
                return debugEntries(DataCoreWorldData.get(serverLevel).worldSnapshot());
            }
            Map<Identifier, String> snapshot = new LinkedHashMap<>();
            Map<Identifier, CompoundTag> client = clientWorldValues.getOrDefault(dimensionId().toString(), Map.of());
            for (Map.Entry<Identifier, CompoundTag> entry : client.entrySet()) {
                snapshot.put(entry.getKey(), debugValue(entry.getValue()));
            }
            return snapshot;
        }
    }

    private final class TeamView implements ITeamDataView {
        private final Level level;
        private final Identifier teamId;

        private TeamView(Level level, Identifier teamId) {
            this.level = level;
            this.teamId = teamId;
        }

        @Override
        public Identifier teamId() {
            return teamId;
        }

        @Override
        public <T> T get(IDataKey<T> key) {
            if (key == null) {
                return null;
            }
            if (!scopeMatches(key, DataScope.TEAM, "read")) {
                return key.defaultValue();
            }
            registerKey(key);
            if (level instanceof ServerLevel serverLevel) {
                return decode(key, DataCoreWorldData.get(serverLevel).teamValue(teamId, key.id().toString()));
            }
            return decode(key, clientValue(DataScope.TEAM, teamId.toString(), key.id()));
        }

        @Override
        public <T> boolean set(IDataKey<T> key, T value) {
            if (key == null || !scopeMatches(key, DataScope.TEAM, "write")
                    || !(level instanceof ServerLevel serverLevel)) {
                return false;
            }
            IDataKey<T> registered = registerKey(key);
            CompoundTag entry = entryFor(registered, value, gameTime(level));
            boolean changed = DataCoreWorldData.get(serverLevel).putTeamValue(teamId, registered.id().toString(), entry);
            if (changed) {
                dirtyTeamKeys.computeIfAbsent(teamId.toString(), ignored -> new LinkedHashSet<>()).add(registered.id());
                syncBridge.markDirty(DataScope.TEAM, teamId.toString(), registered.id());
            }
            return changed;
        }

        @Override
        public boolean clear(IDataKey<?> key) {
            if (key == null || !scopeMatches(key, DataScope.TEAM, "clear")
                    || !(level instanceof ServerLevel serverLevel)) {
                return false;
            }
            boolean changed = DataCoreWorldData.get(serverLevel).removeTeamValue(teamId, key.id().toString());
            if (changed) {
                dirtyTeamKeys.computeIfAbsent(teamId.toString(), ignored -> new LinkedHashSet<>()).add(key.id());
                syncBridge.markDirty(DataScope.TEAM, teamId.toString(), key.id(), true);
            }
            return changed;
        }

        @Override
        public boolean has(IDataKey<?> key) {
            if (key == null || !scopeMatches(key, DataScope.TEAM, "has")) {
                return false;
            }
            if (level instanceof ServerLevel serverLevel) {
                return DataCoreWorldData.get(serverLevel).teamValue(teamId, key.id().toString()) != null;
            }
            return clientValue(DataScope.TEAM, teamId.toString(), key.id()) != null;
        }

        @Override
        public CompoundTag record(Identifier id) {
            return get(IDataKey.record(id, DataScope.TEAM, CompoundTag.CODEC, new CompoundTag(), true)).copy();
        }

        @Override
        public boolean putRecord(Identifier id, CompoundTag value) {
            return set(IDataKey.record(id, DataScope.TEAM, CompoundTag.CODEC, new CompoundTag(), true),
                    value == null ? new CompoundTag() : value.copy());
        }

        @Override
        public Map<Identifier, String> debugSnapshot() {
            if (level instanceof ServerLevel serverLevel) {
                return debugEntries(DataCoreWorldData.get(serverLevel).teamSnapshot(teamId));
            }
            Map<Identifier, String> snapshot = new LinkedHashMap<>();
            Map<Identifier, CompoundTag> client = clientTeamValues.getOrDefault(teamId.toString(), Map.of());
            for (Map.Entry<Identifier, CompoundTag> entry : client.entrySet()) {
                snapshot.put(entry.getKey(), debugValue(entry.getValue()));
            }
            return snapshot;
        }
    }

    private final class DataCoreSyncBridge implements IDataSyncBridge {
        @Override
        public void requestFullSync(ServerPlayer player) {
            sendMetadataSync(player);
            flushDirty(player, true);
        }

        @Override
        public void markDirty(DataScope scope, String ownerId, Identifier keyId) {
            markDirty(scope, ownerId, keyId, false);
        }

        private void markDirty(DataScope scope, String ownerId, Identifier keyId, boolean clear) {
            revision++;
            DataChangeKind changeKind = clear ? DataChangeKind.CLEAR : DataChangeKind.SET;
            rememberChange(scope, ownerId, keyId, changeKind);
            EchoDataBus.publish(new DataChangeMessage(scope, ownerId, keyId,
                    key(keyId).map(IDataKey::kind).orElse(DataValueKind.RECORD), revision, false, changeKind));
        }

        @Override
        public long revision() {
            return revision;
        }

        private void flushDirty(ServerPlayer player, boolean fullSnapshot) {
            if (player == null) {
                return;
            }
            List<DataCoreSyncPacket.Entry> playerEntries = fullSnapshot
                    ? syncedEntries(valuesRoot(playerRoot(player)), null)
                    : dirtyEntries(player);
            if (fullSnapshot || !playerEntries.isEmpty()) {
                DataCoreSyncPacket packet = new DataCoreSyncPacket(DataScope.PLAYER, player.getUUID().toString(),
                        fullSnapshot, revision, playerEntries);
                if (!send(player, packet) && !fullSnapshot) {
                    requeueDirty(packet.scope(), packet.ownerId(), packet.entries());
                }
            }
            if (fullSnapshot && player.level() instanceof ServerLevel serverLevel) {
                send(player, new DataCoreSyncPacket(DataScope.WORLD, serverLevel.dimension().identifier().toString(),
                        true, revision, syncedEntries(DataCoreWorldData.get(serverLevel).worldSnapshot(), DataScope.WORLD)));
                for (Map.Entry<String, Map<String, CompoundTag>> team : DataCoreWorldData.get(serverLevel).teamSnapshots().entrySet()) {
                    send(player, new DataCoreSyncPacket(DataScope.TEAM, team.getKey(),
                            true, revision, syncedEntries(team.getValue(), DataScope.TEAM)));
                }
            }
        }

        private List<DataCoreSyncPacket.Entry> dirtyEntries(ServerPlayer player) {
            LinkedHashSet<Identifier> dirty = dirtyPlayerKeys.get(player.getUUID());
            if (dirty == null || dirty.isEmpty()) {
                return List.of();
            }
            int max = Math.max(1, Config.MAX_SYNC_KEYS_PER_BATCH.get());
            List<Identifier> selected = new ArrayList<>();
            for (Identifier id : List.copyOf(dirty)) {
                selected.add(id);
                if (selected.size() >= max) {
                    break;
                }
            }
            dirty.removeAll(selected);
            if (dirty.isEmpty()) {
                dirtyPlayerKeys.remove(player.getUUID());
            }
            CompoundTag values = valuesRoot(playerRoot(player));
            List<DataCoreSyncPacket.Entry> entries = new ArrayList<>();
            for (Identifier id : selected) {
                IDataKey<?> key = keys.get(id);
                if (key == null || !key.synced()) {
                    continue;
                }
                CompoundTag entry = values.getCompoundOrEmpty(id.toString());
                if (!entry.isEmpty()) {
                    entries.add(new DataCoreSyncPacket.Entry(id, kindOf(key, entry), entry));
                } else {
                    entries.add(new DataCoreSyncPacket.Entry(id, key.kind(), new CompoundTag(), true));
                }
            }
            return entries;
        }

        private void flushShared(ServerLevel level) {
            if (level == null) {
                return;
            }
            DataCoreWorldData data = DataCoreWorldData.get(level);
            String dimension = level.dimension().identifier().toString();
            List<DataCoreSyncPacket.Entry> worldEntries = ownerDirtyEntries(dirtyWorldKeys, dimension,
                    data.worldSnapshot(), DataScope.WORLD);
            List<DataCoreSyncPacket> packets = new ArrayList<>();
            if (!worldEntries.isEmpty()) {
                packets.add(new DataCoreSyncPacket(DataScope.WORLD, dimension, false, revision, worldEntries));
            }
            for (String teamId : List.copyOf(dirtyTeamKeys.keySet())) {
                Identifier parsed = Identifier.tryParse(teamId);
                if (parsed == null) {
                    dirtyTeamKeys.remove(teamId);
                    continue;
                }
                List<DataCoreSyncPacket.Entry> teamEntries = ownerDirtyEntries(dirtyTeamKeys, teamId,
                        data.teamSnapshot(parsed), DataScope.TEAM);
                if (!teamEntries.isEmpty()) {
                    packets.add(new DataCoreSyncPacket(DataScope.TEAM, teamId, false, revision, teamEntries));
                }
            }
            if (packets.isEmpty()) {
                return;
            }
            boolean sentToAnyPlayer = false;
            boolean allSendsSucceeded = true;
            for (ServerPlayer player : level.players()) {
                sentToAnyPlayer = true;
                for (DataCoreSyncPacket packet : packets) {
                    if (!send(player, packet)) {
                        allSendsSucceeded = false;
                    }
                }
            }
            if (!sentToAnyPlayer || !allSendsSucceeded) {
                for (DataCoreSyncPacket packet : packets) {
                    requeueDirty(packet.scope(), packet.ownerId(), packet.entries());
                }
            }
        }

        private List<DataCoreSyncPacket.Entry> ownerDirtyEntries(
                Map<String, LinkedHashSet<Identifier>> dirtyOwners,
                String ownerId,
                Map<String, CompoundTag> values,
                DataScope scope) {
            LinkedHashSet<Identifier> dirty = dirtyOwners.get(ownerId);
            if (dirty == null || dirty.isEmpty()) {
                return List.of();
            }
            int max = Math.max(1, Config.MAX_SYNC_KEYS_PER_BATCH.get());
            List<Identifier> selected = new ArrayList<>();
            for (Identifier id : List.copyOf(dirty)) {
                selected.add(id);
                if (selected.size() >= max) {
                    break;
                }
            }
            dirty.removeAll(selected);
            if (dirty.isEmpty()) {
                dirtyOwners.remove(ownerId);
            }
            List<DataCoreSyncPacket.Entry> entries = new ArrayList<>();
            for (Identifier id : selected) {
                IDataKey<?> key = keys.get(id);
                if (key == null || !key.synced() || key.scope() != scope) {
                    continue;
                }
                CompoundTag entry = values.get(id.toString());
                if (entry == null || entry.isEmpty()) {
                    entries.add(new DataCoreSyncPacket.Entry(id, key.kind(), new CompoundTag(), true));
                } else {
                    entries.add(new DataCoreSyncPacket.Entry(id, key.kind(), entry));
                }
            }
            return entries;
        }

        private List<DataCoreSyncPacket.Entry> syncedEntries(CompoundTag values, DataScope scope) {
            return syncedEntries(compoundMap(values), scope);
        }

        private List<DataCoreSyncPacket.Entry> syncedEntries(Map<String, CompoundTag> values, DataScope scope) {
            List<DataCoreSyncPacket.Entry> entries = new ArrayList<>();
            for (Map.Entry<String, CompoundTag> entry : values.entrySet()) {
                Identifier id = safeIdentifier(entry.getKey());
                IDataKey<?> key = keys.get(id);
                if (key != null && key.synced() && (scope == null || key.scope() == scope)) {
                    entries.add(new DataCoreSyncPacket.Entry(id, key.kind(), entry.getValue()));
                }
            }
            return entries;
        }

        private boolean send(ServerPlayer player, DataCoreSyncPacket packet) {
            return EchoNetSend.toPlayer(player, packet);
        }

        private void requeueDirty(DataScope scope, String ownerId, List<DataCoreSyncPacket.Entry> entries) {
            if (scope == null || ownerId == null || ownerId.isBlank() || entries == null || entries.isEmpty()) {
                return;
            }
            LinkedHashSet<Identifier> keyIds = new LinkedHashSet<>();
            for (DataCoreSyncPacket.Entry entry : entries) {
                if (entry != null && entry.keyId() != null) {
                    keyIds.add(entry.keyId());
                }
            }
            if (keyIds.isEmpty()) {
                return;
            }
            switch (scope) {
                case PLAYER -> {
                    try {
                        UUID playerId = UUID.fromString(ownerId);
                        dirtyPlayerKeys.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).addAll(keyIds);
                    } catch (IllegalArgumentException ignored) {
                        // Invalid player owners cannot be retried safely.
                    }
                }
                case WORLD -> dirtyWorldKeys.computeIfAbsent(ownerId, ignored -> new LinkedHashSet<>()).addAll(keyIds);
                case TEAM -> dirtyTeamKeys.computeIfAbsent(ownerId, ignored -> new LinkedHashSet<>()).addAll(keyIds);
            }
        }
    }

    private static Map<String, CompoundTag> compoundMap(CompoundTag values) {
        Map<String, CompoundTag> map = new LinkedHashMap<>();
        for (String key : values.keySet()) {
            CompoundTag entry = values.getCompoundOrEmpty(key);
            if (!entry.isEmpty()) {
                map.put(key, entry.copy());
            }
        }
        return map;
    }
}

