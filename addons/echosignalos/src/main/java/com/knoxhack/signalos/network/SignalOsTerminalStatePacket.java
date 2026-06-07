package com.knoxhack.signalos.network;

import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalMission;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.content.SignalOsContentRegistry;
import com.knoxhack.signalos.service.SignalOsBuiltinActions;
import com.knoxhack.signalos.service.SignalOsPlayerData;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Mirrors the server-owned SignalOS player progression state needed by terminal pages.
 */
public record SignalOsTerminalStatePacket(
        Set<Identifier> completedMissions,
        Set<Identifier> claimedMissions,
        Set<Identifier> readArchives,
        int pendingRewardCount,
        String networkId,
        boolean networkOnline,
        int accessTier,
        int networkRadius,
        String networkAnchor,
        int terminalCount,
        int workstationCount,
        int serverRackCount,
        int relayCount,
        boolean activeDrivePresent,
        String activeDriveLabel,
        int activeDriveVersion,
        boolean activeDriveWritable,
        String activeDriveStatus,
        int activeDriveRecordCount,
        int activeDriveCapacity,
        String activeDriveSessionApp,
        String activeDriveSearchQuery,
        String lastActionStatus,
        List<SignalOsDataRecord> dataRecords) implements CustomPacketPayload {
    private static final int MAX_ID = 160;
    private static final int MAX_IDS = 4096;
    private static final int MAX_RECORDS = 256;
    private static final int MAX_TEXT = 4096;
    private static final Identifier OVERFLOW_RECORD_ID =
            Identifier.fromNamespaceAndPath(SignalOS.MODID, "terminal_state/overflow_records");

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SignalOS.MODID, "terminal_state");
    public static final Type<SignalOsTerminalStatePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SignalOsTerminalStatePacket> CODEC =
            StreamCodec.of(SignalOsTerminalStatePacket::write, SignalOsTerminalStatePacket::read);

    public SignalOsTerminalStatePacket {
        completedMissions = copyIds(completedMissions);
        claimedMissions = copyIds(claimedMissions);
        readArchives = copyIds(readArchives);
        pendingRewardCount = Math.max(0, pendingRewardCount);
        networkId = networkId == null || networkId.isBlank() ? "offline" : networkId;
        accessTier = Math.max(0, accessTier);
        networkRadius = Math.max(0, networkRadius);
        networkAnchor = networkAnchor == null ? "" : networkAnchor;
        terminalCount = Math.max(0, terminalCount);
        workstationCount = Math.max(0, workstationCount);
        serverRackCount = Math.max(0, serverRackCount);
        relayCount = Math.max(0, relayCount);
        activeDriveLabel = activeDriveLabel == null || activeDriveLabel.isBlank() ? "No Drive" : activeDriveLabel;
        activeDriveVersion = Math.max(0, activeDriveVersion);
        activeDriveStatus = activeDriveStatus == null || activeDriveStatus.isBlank()
                ? activeDrivePresent ? "READY" : "NO DRIVE"
                : activeDriveStatus;
        activeDriveRecordCount = Math.max(0, activeDriveRecordCount);
        activeDriveCapacity = Math.max(0, activeDriveCapacity);
        activeDriveSessionApp = activeDriveSessionApp == null ? "" : activeDriveSessionApp;
        activeDriveSearchQuery = activeDriveSearchQuery == null ? "" : activeDriveSearchQuery;
        lastActionStatus = lastActionStatus == null ? "" : lastActionStatus;
        dataRecords = copyRecords(dataRecords);
    }

    public SignalOsTerminalStatePacket(Set<Identifier> completedMissions,
            Set<Identifier> claimedMissions,
            Set<Identifier> readArchives,
            int pendingRewardCount) {
        this(completedMissions, claimedMissions, readArchives, pendingRewardCount,
                "offline", false, 0, 0, "", 0, 0, 0, 0,
                false, "No Drive", 0, false, "NO DRIVE", 0, SignalOsDriveData.MAX_PLAYER_RECORDS, "", "", "",
                List.of());
    }

    public SignalOsTerminalStatePacket(Set<Identifier> completedMissions,
            Set<Identifier> claimedMissions,
            Set<Identifier> readArchives,
            int pendingRewardCount,
            String networkId,
            boolean networkOnline,
            int accessTier,
            int networkRadius,
            String networkAnchor,
            int terminalCount,
            int workstationCount,
            int serverRackCount,
            int relayCount,
            List<SignalOsDataRecord> dataRecords) {
        this(completedMissions, claimedMissions, readArchives, pendingRewardCount,
                networkId, networkOnline, accessTier, networkRadius, networkAnchor,
                terminalCount, workstationCount, serverRackCount, relayCount,
                false, "No Drive", 0, false, "NO DRIVE", 0, SignalOsDriveData.MAX_PLAYER_RECORDS, "", "", "",
                dataRecords);
    }

    public static SignalOsTerminalStatePacket empty() {
        return new SignalOsTerminalStatePacket(Set.of(), Set.of(), Set.of(), 0);
    }

    public static SignalOsTerminalStatePacket from(ServerPlayer player) {
        if (player == null) {
            return empty();
        }
        SignalOsComputerNetworkService.NetworkSnapshot snapshot = SignalOsComputerNetworkService.snapshot(player);
        return create(player,
                mission -> SignalOsBuiltinActions.completed(player, mission),
                SignalOsTerminalServices.pendingRewardCount(player),
                snapshot);
    }

    public static SignalOsTerminalStatePacket createForTests(Player player,
            Predicate<TerminalMission> completionResolver, int pendingRewardCount) {
        return create(player, completionResolver, pendingRewardCount, SignalOsComputerNetworkService.NetworkSnapshot.offline());
    }

    private static SignalOsTerminalStatePacket create(Player player,
            Predicate<TerminalMission> completionResolver, int pendingRewardCount,
            SignalOsComputerNetworkService.NetworkSnapshot snapshot) {
        LinkedHashSet<Identifier> completed = new LinkedHashSet<>();
        LinkedHashSet<Identifier> claimed = new LinkedHashSet<>();
        LinkedHashSet<Identifier> read = new LinkedHashSet<>();

        if (player != null) {
            for (TerminalMission mission : SignalOsContentRegistry.missions()) {
                if (completionResolver != null && completionResolver.test(mission)) {
                    completed.add(mission.id());
                }
                if (SignalOsPlayerData.isMissionClaimed(player, mission.id())) {
                    claimed.add(mission.id());
                }
            }
            for (TerminalArchiveRecord archive : SignalOsContentRegistry.archives()) {
                if (SignalOsPlayerData.isArchiveRead(player, archive.id())) {
                    read.add(archive.id());
                }
            }
        }

        SignalOsComputerNetworkService.NetworkSnapshot safe =
                snapshot == null ? SignalOsComputerNetworkService.NetworkSnapshot.offline() : snapshot;
        SignalOsDriveData activeDrive = safe.activeDrivePresent()
                ? SignalOsTerminalServices.activeDriveData(player, false)
                : SignalOsDriveData.EMPTY;
        return new SignalOsTerminalStatePacket(completed, claimed, read, pendingRewardCount,
                safe.networkId(), safe.online(), safe.accessTier(), safe.radius(), safe.anchor(),
                safe.terminals(), safe.workstations(), safe.serverRacks(), safe.relays(),
                safe.activeDrivePresent(), safe.activeDriveLabel(), safe.activeDriveVersion(),
                safe.activeDriveWritable(), safe.activeDriveStatus(), safe.activeDriveRecordCount(),
                safe.activeDriveCapacity(), activeDrive.sessionValue("selected_app", ""),
                activeDrive.sessionValue("search", ""), SignalOsTerminalServices.lastActionStatus(player),
                safe.records());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, SignalOsTerminalStatePacket packet) {
        writeIds(buffer, packet.completedMissions());
        writeIds(buffer, packet.claimedMissions());
        writeIds(buffer, packet.readArchives());
        buffer.writeVarInt(packet.pendingRewardCount());
        buffer.writeUtf(packet.networkId(), MAX_TEXT);
        buffer.writeBoolean(packet.networkOnline());
        buffer.writeVarInt(packet.accessTier());
        buffer.writeVarInt(packet.networkRadius());
        buffer.writeUtf(packet.networkAnchor(), MAX_TEXT);
        buffer.writeVarInt(packet.terminalCount());
        buffer.writeVarInt(packet.workstationCount());
        buffer.writeVarInt(packet.serverRackCount());
        buffer.writeVarInt(packet.relayCount());
        buffer.writeBoolean(packet.activeDrivePresent());
        buffer.writeUtf(packet.activeDriveLabel(), MAX_TEXT);
        buffer.writeVarInt(packet.activeDriveVersion());
        buffer.writeBoolean(packet.activeDriveWritable());
        buffer.writeUtf(packet.activeDriveStatus(), MAX_TEXT);
        buffer.writeVarInt(packet.activeDriveRecordCount());
        buffer.writeVarInt(packet.activeDriveCapacity());
        buffer.writeUtf(packet.activeDriveSessionApp(), MAX_TEXT);
        buffer.writeUtf(packet.activeDriveSearchQuery(), MAX_TEXT);
        buffer.writeUtf(packet.lastActionStatus(), MAX_TEXT);
        writeRecords(buffer, packet.dataRecords());
    }

    private static SignalOsTerminalStatePacket read(RegistryFriendlyByteBuf buffer) {
        return new SignalOsTerminalStatePacket(
                readIds(buffer),
                readIds(buffer),
                readIds(buffer),
                buffer.readVarInt(),
                buffer.readUtf(MAX_TEXT),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(MAX_TEXT),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_TEXT),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_TEXT),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_TEXT),
                readRecords(buffer));
    }

    private static void writeIds(RegistryFriendlyByteBuf buffer, Set<Identifier> ids) {
        Set<Identifier> safeIds = copyIds(ids);
        if (safeIds.size() > MAX_IDS) {
            throw new IllegalArgumentException("SignalOS terminal state exceeded " + MAX_IDS + " ids.");
        }
        buffer.writeVarInt(safeIds.size());
        for (Identifier id : safeIds) {
            buffer.writeUtf(id.toString(), MAX_ID);
        }
    }

    private static Set<Identifier> readIds(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_IDS) {
            throw new IllegalArgumentException("Invalid SignalOS terminal state id count: " + count);
        }
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            ids.add(Identifier.parse(buffer.readUtf(MAX_ID)));
        }
        return ids;
    }

    private static Set<Identifier> copyIds(Set<Identifier> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<Identifier> copy = ids.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Identifier::toString))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(copy);
    }

    private static void writeRecords(RegistryFriendlyByteBuf buffer, List<SignalOsDataRecord> records) {
        List<SignalOsDataRecord> safe = copyRecords(records);
        buffer.writeVarInt(safe.size());
        for (SignalOsDataRecord record : safe) {
            buffer.writeUtf(record.id().toString(), MAX_ID);
            buffer.writeUtf(record.title(), MAX_TEXT);
            buffer.writeUtf(record.type(), MAX_ID);
            buffer.writeUtf(record.source(), MAX_TEXT);
            buffer.writeUtf(record.body(), MAX_TEXT);
            buffer.writeVarInt(record.order());
            buffer.writeBoolean(record.archived());
            writeMetadata(buffer, record.metadata());
        }
    }

    private static List<SignalOsDataRecord> copyRecords(List<SignalOsDataRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<SignalOsDataRecord> copy = records.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
        if (copy.size() <= MAX_RECORDS) {
            return List.copyOf(copy);
        }
        int keep = Math.max(0, MAX_RECORDS - 1);
        java.util.ArrayList<SignalOsDataRecord> capped = new java.util.ArrayList<>(copy.subList(0, keep));
        capped.add(overflowRecord(copy.size() - keep));
        return List.copyOf(capped);
    }

    private static SignalOsDataRecord overflowRecord(int hiddenCount) {
        int hidden = Math.max(1, hiddenCount);
        return new SignalOsDataRecord(
                OVERFLOW_RECORD_ID,
                "Additional Records Hidden",
                "system",
                "SignalOS",
                hidden + " additional network data records were omitted from this sync to keep the terminal connection stable.",
                Integer.MAX_VALUE,
                false);
    }

    private static List<SignalOsDataRecord> readRecords(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_RECORDS) {
            throw new IllegalArgumentException("Invalid SignalOS data record count: " + count);
        }
        java.util.ArrayList<SignalOsDataRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(new SignalOsDataRecord(
                    Identifier.parse(buffer.readUtf(MAX_ID)),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_ID),
                    buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_TEXT),
                buffer.readVarInt(),
                    buffer.readBoolean(),
                    readMetadata(buffer)));
        }
        return List.copyOf(records);
    }

    private static void writeMetadata(RegistryFriendlyByteBuf buffer, java.util.Map<String, String> metadata) {
        java.util.Map<String, String> safe = SignalOsDataRecord.cleanMetadata(metadata);
        buffer.writeVarInt(safe.size());
        for (java.util.Map.Entry<String, String> entry : safe.entrySet()) {
            buffer.writeUtf(entry.getKey(), SignalOsDataRecord.MAX_METADATA_KEY);
            buffer.writeUtf(entry.getValue(), SignalOsDataRecord.MAX_METADATA_VALUE);
        }
    }

    private static java.util.Map<String, String> readMetadata(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > SignalOsDataRecord.MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("Invalid SignalOS terminal state metadata count: " + count);
        }
        java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            metadata.put(buffer.readUtf(SignalOsDataRecord.MAX_METADATA_KEY),
                    buffer.readUtf(SignalOsDataRecord.MAX_METADATA_VALUE));
        }
        return metadata;
    }
}
