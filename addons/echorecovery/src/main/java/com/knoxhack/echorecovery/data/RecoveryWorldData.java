package com.knoxhack.echorecovery.data;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RecoveryWorldData extends SavedData {
    public static final Codec<RecoveryWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GraveEntry.CODEC.listOf().optionalFieldOf("graves", List.of()).forGetter(RecoveryWorldData::graveList),
        DeathRecord.CODEC.listOf().optionalFieldOf("history", List.of()).forGetter(RecoveryWorldData::historyList)
    ).apply(instance, RecoveryWorldData::fromCodec));

    public static final SavedDataType<RecoveryWorldData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "recovery_world_data"),
        RecoveryWorldData::new,
        CODEC
    );

    private final Map<UUID, GraveEntry> gravesById = new LinkedHashMap<>();
    private final Map<UUID, List<UUID>> activeGravesByPlayer = new LinkedHashMap<>();
    private final Map<UUID, List<DeathRecord>> deathHistory = new LinkedHashMap<>();

    public RecoveryWorldData() {}

    private RecoveryWorldData(List<GraveEntry> graves, List<DeathRecord> history) {
        for (GraveEntry grave : graves) {
            upsertGrave(grave, false);
        }
        for (DeathRecord record : history) {
            deathHistory.computeIfAbsent(record.playerId(), ignored -> new ArrayList<>()).add(record);
        }
        for (UUID playerId : List.copyOf(activeGravesByPlayer.keySet())) {
            capGraves(playerId, false);
        }
    }

    private static RecoveryWorldData fromCodec(List<GraveEntry> graves, List<DeathRecord> history) {
        return new RecoveryWorldData(graves, history);
    }

    public static RecoveryWorldData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static GraveLookup findLoaded(ServerPlayer player, UUID ownerId, String idPrefix) {
        if (player == null || player.level().getServer() == null || idPrefix == null || idPrefix.isBlank()) {
            return GraveLookup.missing();
        }
        GraveLookup found = GraveLookup.missing();
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            Match match = getOrCreate(level).match(ownerId, idPrefix);
            if (match.ambiguous()) {
                return GraveLookup.ambiguousLookup();
            }
            if (match.entry().isEmpty()) {
                continue;
            }
            if (found.entry().isPresent()) {
                return GraveLookup.ambiguousLookup();
            }
            found = new GraveLookup(level, match.entry().get(), false);
        }
        return found;
    }

    public List<GraveEntry> graveList() {
        return List.copyOf(gravesById.values());
    }

    public List<DeathRecord> historyList() {
        List<DeathRecord> all = new ArrayList<>();
        for (List<DeathRecord> list : deathHistory.values()) {
            all.addAll(list);
        }
        return List.copyOf(all);
    }

    public List<GraveEntry> getActiveGraves(UUID playerId) {
        List<UUID> ids = activeGravesByPlayer.getOrDefault(playerId, List.of());
        List<GraveEntry> graves = new ArrayList<>();
        for (UUID id : ids) {
            GraveEntry grave = gravesById.get(id);
            if (grave != null && grave.active()) {
                graves.add(grave);
            }
        }
        graves.sort(Comparator.comparingLong(GraveEntry::createdAt));
        return List.copyOf(graves);
    }

    public List<GraveEntry> getRecoverableGraves(UUID playerId) {
        return gravesById.values().stream()
                .filter(grave -> grave.ownerId().equals(playerId))
                .filter(GraveEntry::active)
                .sorted(Comparator.comparingLong(GraveEntry::createdAt))
                .toList();
    }

    public void addGrave(UUID playerId, GraveEntry grave) {
        upsertGrave(grave, true);
        capGraves(playerId, true);
    }

    public void upsertGrave(GraveEntry grave) {
        upsertGrave(grave, true);
    }

    private void upsertGrave(GraveEntry grave, boolean dirty) {
        if (grave == null) {
            return;
        }
        gravesById.put(grave.graveId(), grave);
        List<UUID> active = activeGravesByPlayer.computeIfAbsent(grave.ownerId(), ignored -> new ArrayList<>());
        if (grave.active() && !active.contains(grave.graveId())) {
            active.add(grave.graveId());
        }
        if (!grave.active()) {
            active.remove(grave.graveId());
        }
        if (active.isEmpty()) {
            activeGravesByPlayer.remove(grave.ownerId());
        }
        if (dirty) {
            setDirty();
        }
    }

    public void updateFromBlockEntity(GraveBlockEntity grave) {
        if (grave == null) {
            return;
        }
        GraveEntry existing = gravesById.get(grave.graveId());
        BlockPos deathPos = existing == null ? grave.getBlockPos() : existing.deathPos();
        String fallbackReason = existing == null ? "" : existing.fallbackReason();
        upsertGrave(GraveEntry.fromBlockEntity(grave, deathPos, fallbackReason));
    }

    public void removeGrave(UUID playerId, BlockPos pos) {
        updateByPosition(playerId, pos, GraveEntry::withDeleted);
    }

    public GraveEntry findGrave(UUID playerId, String idPrefix) {
        return match(playerId, idPrefix).entry().orElse(null);
    }

    public Match match(UUID playerId, String idPrefix) {
        if (idPrefix == null || idPrefix.isBlank()) {
            return Match.missing();
        }
        GraveEntry found = null;
        String prefix = idPrefix.trim().toLowerCase(java.util.Locale.ROOT);
        for (GraveEntry grave : gravesById.values()) {
            if (playerId != null && !grave.ownerId().equals(playerId)) {
                continue;
            }
            if (!grave.active()) {
                continue;
            }
            String full = grave.graveId().toString().toLowerCase(java.util.Locale.ROOT);
            if (!full.equals(prefix) && !full.startsWith(prefix)) {
                continue;
            }
            if (found != null) {
                return Match.ambiguousMatch();
            }
            found = grave;
        }
        return found == null ? Match.missing() : Match.found(found);
    }

    public void markRecovered(UUID playerId, BlockPos pos) {
        updateByPosition(playerId, pos, GraveEntry::withRecovered);
        updateHistory(playerId, pos, true, false);
    }

    public void markExpired(UUID playerId, BlockPos pos) {
        updateByPosition(playerId, pos, GraveEntry::withExpired);
        updateHistory(playerId, pos, false, true);
    }

    public void shareGrave(UUID ownerId, UUID graveId, Set<UUID> sharedPlayers) {
        GraveEntry grave = gravesById.get(graveId);
        if (grave != null && grave.ownerId().equals(ownerId)) {
            upsertGrave(grave.withSharedPlayers(sharedPlayers));
        }
    }

    public void addDeathRecord(UUID playerId, DeathRecord record) {
        List<DeathRecord> history = deathHistory.computeIfAbsent(playerId, ignored -> new ArrayList<>());
        history.add(record);
        int max = Math.max(1, RecoveryConfig.MAX_DEATH_HISTORY.get());
        while (history.size() > max) {
            history.remove(0);
        }
        setDirty();
    }

    public List<DeathRecord> getDeathHistory(UUID playerId) {
        return List.copyOf(deathHistory.getOrDefault(playerId, List.of()));
    }

    private void updateByPosition(UUID playerId, BlockPos pos, java.util.function.Function<GraveEntry, GraveEntry> updater) {
        for (GraveEntry grave : List.copyOf(gravesById.values())) {
            if (grave.ownerId().equals(playerId) && grave.pos().equals(pos)) {
                upsertGrave(updater.apply(grave));
            }
        }
    }

    private void updateHistory(UUID playerId, BlockPos pos, boolean recovered, boolean expired) {
        List<DeathRecord> history = deathHistory.get(playerId);
        if (history == null) {
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            DeathRecord record = history.get(i);
            if (record.pos().equals(pos)) {
                history.set(i, new DeathRecord(record.playerId(), record.time(), record.cause(),
                        record.dimension(), record.pos(), recovered || record.recovered(), expired || record.expired()));
                setDirty();
            }
        }
    }

    private void capGraves(UUID playerId, boolean dirty) {
        List<UUID> ids = activeGravesByPlayer.get(playerId);
        if (ids == null) {
            return;
        }
        ids.sort(Comparator.comparingLong(id -> {
            GraveEntry grave = gravesById.get(id);
            return grave == null ? Long.MAX_VALUE : grave.createdAt();
        }));
        int max = Math.max(1, RecoveryConfig.MAX_GRAVES_PER_PLAYER.get());
        while (ids.size() > max) {
            UUID oldest = ids.remove(0);
            EchoRecovery.LOGGER.info("Recovery grave cap reached for {}; oldest grave {} remains recoverable by id.",
                    playerId, oldest);
        }
        if (ids.isEmpty()) {
            activeGravesByPlayer.remove(playerId);
        }
        if (dirty) {
            setDirty();
        }
    }

    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException ignored) {
            return new UUID(0, 0);
        }
    }

    public record GraveLookup(ServerLevel level, GraveEntry grave, boolean ambiguous) {
        public static GraveLookup missing() {
            return new GraveLookup(null, null, false);
        }

        public static GraveLookup ambiguousLookup() {
            return new GraveLookup(null, null, true);
        }

        public Optional<GraveEntry> entry() {
            return Optional.ofNullable(grave);
        }
    }

    public record Match(Optional<GraveEntry> entry, boolean ambiguous) {
        public static Match missing() {
            return new Match(Optional.empty(), false);
        }

        public static Match found(GraveEntry entry) {
            return new Match(Optional.of(entry), false);
        }

        public static Match ambiguousMatch() {
            return new Match(Optional.empty(), true);
        }
    }

    public record StoredSlot(int graveSlot, int originalSlot, String kind) {
        public static final Codec<StoredSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("graveSlot", -1).forGetter(StoredSlot::graveSlot),
            Codec.INT.optionalFieldOf("originalSlot", -1).forGetter(StoredSlot::originalSlot),
            Codec.STRING.optionalFieldOf("kind", "inventory").forGetter(StoredSlot::kind)
        ).apply(instance, StoredSlot::new));

        public StoredSlot {
            kind = kind == null || kind.isBlank() ? "inventory" : kind;
        }
    }

    public record GraveEntry(UUID graveId, UUID ownerId, String ownerName, BlockPos pos, BlockPos deathPos,
                             String dimension, String sourceDimension, long createdAt, long expiresAt,
                             String deathCause, String deathMessage, String graveTypeId, int xpStored,
                             boolean recovered, boolean expired, boolean deleted, boolean contaminated,
                             boolean temporaryPlatform, String fallbackReason, List<String> hazardNotes,
                             List<String> sharedPlayers, List<StoredSlot> storedSlots) {
        public static final Codec<GraveEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GraveIdentity.CODEC.fieldOf("identity").forGetter(GraveIdentity::from),
            GraveLocation.CODEC.fieldOf("location").forGetter(GraveLocation::from),
            GraveLifecycle.CODEC.fieldOf("lifecycle").forGetter(GraveLifecycle::from),
            GraveFlags.CODEC.fieldOf("flags").forGetter(GraveFlags::from),
            GraveDetails.CODEC.fieldOf("details").forGetter(GraveDetails::from)
        ).apply(instance, GraveEntry::fromCodec));

        public GraveEntry {
            ownerName = ownerName == null ? "" : ownerName;
            dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension;
            sourceDimension = sourceDimension == null || sourceDimension.isBlank() ? dimension : sourceDimension;
            deathCause = deathCause == null ? "" : deathCause;
            deathMessage = deathMessage == null ? "" : deathMessage;
            graveTypeId = graveTypeId == null || graveTypeId.isBlank() ? "echorecovery:vanilla_grave" : graveTypeId;
            xpStored = Math.max(0, xpStored);
            fallbackReason = fallbackReason == null ? "" : fallbackReason;
            hazardNotes = List.copyOf(hazardNotes == null ? List.of() : hazardNotes);
            sharedPlayers = List.copyOf(sharedPlayers == null ? List.of() : sharedPlayers);
            storedSlots = List.copyOf(storedSlots == null ? List.of() : storedSlots);
        }

        public static GraveEntry fromCodec(GraveIdentity identity, GraveLocation location,
                GraveLifecycle lifecycle, GraveFlags flags, GraveDetails details) {
            BlockPos safeDeathPos = location.deathPos().equals(BlockPos.ZERO) ? location.pos() : location.deathPos();
            return new GraveEntry(uuid(identity.graveId()), uuid(identity.ownerId()), identity.ownerName(),
                    location.pos(), safeDeathPos, location.dimension(), location.sourceDimension(),
                    lifecycle.createdAt(), lifecycle.expiresAt(), lifecycle.deathCause(), lifecycle.deathMessage(),
                    details.graveTypeId(), details.xpStored(), flags.recovered(), flags.expired(), flags.deleted(),
                    flags.contaminated(), flags.temporaryPlatform(), details.fallbackReason(), details.hazardNotes(),
                    details.sharedPlayers(), details.storedSlots());
        }

        public static GraveEntry fromBlockEntity(GraveBlockEntity grave, BlockPos deathPos, String fallbackReason) {
            return new GraveEntry(grave.graveId(), grave.ownerId(), grave.ownerName(), grave.getBlockPos(),
                    deathPos == null ? grave.getBlockPos() : deathPos, grave.dimension(), grave.sourceDimension(),
                    grave.createdAt(), grave.expiresAt(), grave.deathCause(), grave.deathMessage(),
                    grave.graveTypeId(), grave.xpStored(), grave.isRecovered(), grave.isExpired(), false,
                    grave.contaminated(), grave.temporaryPlatform(), fallbackReason, grave.hazardNoteList(),
                    grave.sharedPlayers().stream().map(UUID::toString).toList(), grave.storedSlotMetadata());
        }

        public boolean active() {
            return !recovered && !deleted;
        }

        public GraveEntry withRecovered() {
            return new GraveEntry(graveId, ownerId, ownerName, pos, deathPos, dimension, sourceDimension, createdAt,
                    expiresAt, deathCause, deathMessage, graveTypeId, xpStored, true, expired, deleted, contaminated,
                    temporaryPlatform, fallbackReason, hazardNotes, sharedPlayers, storedSlots);
        }

        public GraveEntry withExpired() {
            return new GraveEntry(graveId, ownerId, ownerName, pos, deathPos, dimension, sourceDimension, createdAt,
                    expiresAt, deathCause, deathMessage, graveTypeId, xpStored, recovered, true, deleted, contaminated,
                    temporaryPlatform, fallbackReason, hazardNotes, sharedPlayers, storedSlots);
        }

        public GraveEntry withDeleted() {
            return new GraveEntry(graveId, ownerId, ownerName, pos, deathPos, dimension, sourceDimension, createdAt,
                    expiresAt, deathCause, deathMessage, graveTypeId, xpStored, recovered, expired, true, contaminated,
                    temporaryPlatform, fallbackReason, hazardNotes, sharedPlayers, storedSlots);
        }

        public GraveEntry withSharedPlayers(Set<UUID> players) {
            return new GraveEntry(graveId, ownerId, ownerName, pos, deathPos, dimension, sourceDimension, createdAt,
                    expiresAt, deathCause, deathMessage, graveTypeId, xpStored, recovered, expired, deleted, contaminated,
                    temporaryPlatform, fallbackReason, hazardNotes,
                    players == null ? List.of() : players.stream().map(UUID::toString).toList(), storedSlots);
        }

        private record GraveIdentity(String graveId, String ownerId, String ownerName) {
            private static final Codec<GraveIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("graveId").forGetter(GraveIdentity::graveId),
                Codec.STRING.fieldOf("ownerId").forGetter(GraveIdentity::ownerId),
                Codec.STRING.optionalFieldOf("ownerName", "").forGetter(GraveIdentity::ownerName)
            ).apply(instance, GraveIdentity::new));

            private static GraveIdentity from(GraveEntry entry) {
                return new GraveIdentity(entry.graveId().toString(), entry.ownerId().toString(), entry.ownerName());
            }
        }

        private record GraveLocation(BlockPos pos, BlockPos deathPos, String dimension, String sourceDimension) {
            private static final Codec<GraveLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(GraveLocation::pos),
                BlockPos.CODEC.optionalFieldOf("deathPos", BlockPos.ZERO).forGetter(GraveLocation::deathPos),
                Codec.STRING.optionalFieldOf("dimension", "minecraft:overworld").forGetter(GraveLocation::dimension),
                Codec.STRING.optionalFieldOf("sourceDimension", "minecraft:overworld").forGetter(GraveLocation::sourceDimension)
            ).apply(instance, GraveLocation::new));

            private static GraveLocation from(GraveEntry entry) {
                return new GraveLocation(entry.pos(), entry.deathPos(), entry.dimension(), entry.sourceDimension());
            }
        }

        private record GraveLifecycle(long createdAt, long expiresAt, String deathCause, String deathMessage) {
            private static final Codec<GraveLifecycle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.optionalFieldOf("createdAt", 0L).forGetter(GraveLifecycle::createdAt),
                Codec.LONG.optionalFieldOf("expiresAt", 0L).forGetter(GraveLifecycle::expiresAt),
                Codec.STRING.optionalFieldOf("deathCause", "").forGetter(GraveLifecycle::deathCause),
                Codec.STRING.optionalFieldOf("deathMessage", "").forGetter(GraveLifecycle::deathMessage)
            ).apply(instance, GraveLifecycle::new));

            private static GraveLifecycle from(GraveEntry entry) {
                return new GraveLifecycle(entry.createdAt(), entry.expiresAt(), entry.deathCause(), entry.deathMessage());
            }
        }

        private record GraveFlags(boolean recovered, boolean expired, boolean deleted, boolean contaminated,
                                  boolean temporaryPlatform) {
            private static final Codec<GraveFlags> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("recovered", false).forGetter(GraveFlags::recovered),
                Codec.BOOL.optionalFieldOf("expired", false).forGetter(GraveFlags::expired),
                Codec.BOOL.optionalFieldOf("deleted", false).forGetter(GraveFlags::deleted),
                Codec.BOOL.optionalFieldOf("contaminated", false).forGetter(GraveFlags::contaminated),
                Codec.BOOL.optionalFieldOf("temporaryPlatform", false).forGetter(GraveFlags::temporaryPlatform)
            ).apply(instance, GraveFlags::new));

            private static GraveFlags from(GraveEntry entry) {
                return new GraveFlags(entry.recovered(), entry.expired(), entry.deleted(), entry.contaminated(),
                        entry.temporaryPlatform());
            }
        }

        private record GraveDetails(String graveTypeId, int xpStored, String fallbackReason, List<String> hazardNotes,
                                    List<String> sharedPlayers, List<StoredSlot> storedSlots) {
            private static final Codec<GraveDetails> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("graveTypeId", "echorecovery:vanilla_grave").forGetter(GraveDetails::graveTypeId),
                Codec.INT.optionalFieldOf("xpStored", 0).forGetter(GraveDetails::xpStored),
                Codec.STRING.optionalFieldOf("fallbackReason", "").forGetter(GraveDetails::fallbackReason),
                Codec.STRING.listOf().optionalFieldOf("hazardNotes", List.of()).forGetter(GraveDetails::hazardNotes),
                Codec.STRING.listOf().optionalFieldOf("sharedPlayers", List.of()).forGetter(GraveDetails::sharedPlayers),
                StoredSlot.CODEC.listOf().optionalFieldOf("storedSlots", List.of()).forGetter(GraveDetails::storedSlots)
            ).apply(instance, GraveDetails::new));

            private static GraveDetails from(GraveEntry entry) {
                return new GraveDetails(entry.graveTypeId(), entry.xpStored(), entry.fallbackReason(),
                        entry.hazardNotes(), entry.sharedPlayers(), entry.storedSlots());
            }
        }
    }

    public record DeathRecord(UUID playerId, long time, String cause, String dimension,
                              BlockPos pos, boolean recovered, boolean expired) {
        public static final Codec<DeathRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("playerId").forGetter(r -> r.playerId().toString()),
            Codec.LONG.optionalFieldOf("time", 0L).forGetter(DeathRecord::time),
            Codec.STRING.optionalFieldOf("cause", "").forGetter(DeathRecord::cause),
            Codec.STRING.optionalFieldOf("dimension", "").forGetter(DeathRecord::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(DeathRecord::pos),
            Codec.BOOL.optionalFieldOf("recovered", false).forGetter(DeathRecord::recovered),
            Codec.BOOL.optionalFieldOf("expired", false).forGetter(DeathRecord::expired)
        ).apply(instance, DeathRecord::fromCodec));

        public static DeathRecord fromCodec(String playerId, long time, String cause,
                                            String dimension, BlockPos pos, boolean recovered, boolean expired) {
            return new DeathRecord(uuid(playerId), time, cause, dimension, pos, recovered, expired);
        }
    }
}
