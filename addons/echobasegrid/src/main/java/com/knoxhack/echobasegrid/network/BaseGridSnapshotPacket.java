package com.knoxhack.echobasegrid.network;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimMember;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.config.BaseGridConfig;
import com.knoxhack.echobasegrid.data.BaseGridSavedData;
import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public record BaseGridSnapshotPacket(
        String dimension,
        int centerChunkX,
        int centerChunkZ,
        int selectedChunkX,
        int selectedChunkZ,
        String selectedKey,
        String selectedState,
        String selectedOwner,
        boolean selectedOwnedByPlayer,
        boolean selectedManageable,
        boolean selectedReleaseAllowed,
        int claimCount,
        int maxClaims,
        int gridRadius,
        String status,
        List<ChunkData> chunks,
        List<MemberData> members,
        List<PlayerData> candidates) implements CustomPacketPayload {
    private static final int MAX_TEXT = 256;
    private static final int MAX_CHUNKS = 625;
    private static final int MAX_MEMBERS = 128;
    private static final int MAX_CANDIDATES = 128;

    public static final Identifier ID = EchoBaseGrid.id("snapshot");
    public static final Type<BaseGridSnapshotPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseGridSnapshotPacket> CODEC =
            StreamCodec.of(BaseGridSnapshotPacket::write, BaseGridSnapshotPacket::read);

    public BaseGridSnapshotPacket {
        dimension = dimension == null ? "minecraft:overworld" : dimension;
        selectedKey = selectedKey == null ? "" : selectedKey;
        selectedState = selectedState == null ? "unclaimed" : selectedState;
        selectedOwner = selectedOwner == null ? "" : selectedOwner;
        status = status == null ? "" : status;
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
        members = members == null ? List.of() : List.copyOf(members);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static BaseGridSnapshotPacket create(ServerPlayer player, String selectedDimension,
            int selectedChunkX, int selectedChunkZ, String status) {
        String dimension = BaseGridClaimService.cleanDimension(player, selectedDimension);
        ChunkPos center = player.chunkPosition();
        if (selectedDimension == null || selectedDimension.isBlank()) {
            selectedChunkX = center.x();
            selectedChunkZ = center.z();
        }
        int radius = Math.max(1, BaseGridConfig.GRID_RADIUS.get());
        BaseGridSavedData data = BaseGridSavedData.get((net.minecraft.server.level.ServerLevel) player.level());
        int claimCount = data.claimCount(player.getUUID());
        int maxClaims = Math.max(0, BaseGridConfig.MAX_CLAIMS_PER_PLAYER.get());
        Optional<ClaimRecord> selected = data.claim(dimension, selectedChunkX, selectedChunkZ);
        List<ChunkData> chunks = chunkRows(player, data, dimension, center, selectedChunkX, selectedChunkZ, radius);
        List<MemberData> members = selected.map(claim -> memberRows(claim, player)).orElse(List.of());
        List<PlayerData> candidates = selected
                .filter(claim -> claim.ownedBy(player.getUUID()) || claim.allows(player.getUUID(), ClaimPermission.MANAGE)
                        || BaseGridClaimService.canBypass(player))
                .map(claim -> candidateRows(player, claim))
                .orElse(List.of());
        boolean manageable = selected
                .map(claim -> claim.ownedBy(player.getUUID()) || claim.allows(player.getUUID(), ClaimPermission.MANAGE)
                        || BaseGridClaimService.canBypass(player))
                .orElse(false);
        boolean releaseAllowed = selected
                .map(claim -> claim.ownedBy(player.getUUID()) || BaseGridClaimService.canBypass(player))
                .orElse(false);
        return new BaseGridSnapshotPacket(
                dimension,
                center.x(),
                center.z(),
                selectedChunkX,
                selectedChunkZ,
                ClaimRecord.key(dimension, selectedChunkX, selectedChunkZ),
                selected.map(claim -> claim.ownedBy(player.getUUID()) ? "mine"
                        : claim.members().containsKey(player.getUUID()) ? "trusted" : "occupied")
                        .orElse("unclaimed"),
                selected.map(ClaimRecord::ownerName).orElse("Unclaimed"),
                selected.map(claim -> claim.ownedBy(player.getUUID())).orElse(false),
                manageable,
                releaseAllowed,
                claimCount,
                maxClaims,
                radius,
                status,
                chunks,
                members,
                candidates);
    }

    private static List<ChunkData> chunkRows(ServerPlayer player, BaseGridSavedData data, String dimension,
            ChunkPos center, int selectedChunkX, int selectedChunkZ, int radius) {
        java.util.ArrayList<ChunkData> rows = new java.util.ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = center.x() + dx;
                int z = center.z() + dz;
                Optional<ClaimRecord> claim = data.claim(dimension, x, z);
                boolean selected = x == selectedChunkX && z == selectedChunkZ;
                String state = claim.map(value -> value.ownedBy(player.getUUID()) ? "mine"
                        : value.members().containsKey(player.getUUID()) ? "trusted" : "occupied")
                        .orElse("unclaimed");
                rows.add(new ChunkData(
                        ClaimRecord.key(dimension, x, z),
                        dimension,
                        x,
                        z,
                        dx,
                        dz,
                        selected,
                        x == center.x() && z == center.z(),
                        state,
                        claim.map(ClaimRecord::ownerName).orElse(""),
                        labelFor(x, z, selected, x == center.x() && z == center.z())));
            }
        }
        return rows;
    }

    private static String labelFor(int x, int z, boolean selected, boolean current) {
        if (current) {
            return "YOU";
        }
        if (selected) {
            return "SEL";
        }
        return x + "," + z;
    }

    private static List<MemberData> memberRows(ClaimRecord claim, ServerPlayer viewer) {
        return claim.sortedMembers().stream()
                .map(member -> memberRow(claim, member, viewer))
                .toList();
    }

    private static MemberData memberRow(ClaimRecord claim, ClaimMember member, ServerPlayer viewer) {
        boolean manageable = claim.ownedBy(viewer.getUUID()) || claim.allows(viewer.getUUID(), ClaimPermission.MANAGE)
                || BaseGridClaimService.canBypass(viewer);
        return new MemberData(
                member.playerId().toString(),
                member.playerName(),
                member.role().name(),
                member.role().label(),
                enabled(member, ClaimPermission.BUILD),
                enabled(member, ClaimPermission.INTERACT),
                enabled(member, ClaimPermission.CONTAINERS),
                enabled(member, ClaimPermission.MANAGE),
                manageable);
    }

    private static boolean enabled(ClaimMember member, ClaimPermission permission) {
        return member.permissions().contains(permission);
    }

    private static List<PlayerData> candidateRows(ServerPlayer player, ClaimRecord claim) {
        return player.level().getServer().getPlayerList().getPlayers().stream()
                .filter(candidate -> !candidate.getUUID().equals(player.getUUID()))
                .filter(candidate -> !candidate.getUUID().equals(claim.ownerId()))
                .filter(candidate -> !claim.members().containsKey(candidate.getUUID()))
                .sorted(Comparator.comparing(ServerPlayer::getScoreboardName, String.CASE_INSENSITIVE_ORDER))
                .limit(48)
                .map(candidate -> new PlayerData(candidate.getUUID().toString(), candidate.getScoreboardName()))
                .toList();
    }

    private static void write(RegistryFriendlyByteBuf buffer, BaseGridSnapshotPacket packet) {
        buffer.writeUtf(packet.dimension(), MAX_TEXT);
        buffer.writeInt(packet.centerChunkX());
        buffer.writeInt(packet.centerChunkZ());
        buffer.writeInt(packet.selectedChunkX());
        buffer.writeInt(packet.selectedChunkZ());
        buffer.writeUtf(packet.selectedKey(), MAX_TEXT);
        buffer.writeUtf(packet.selectedState(), MAX_TEXT);
        buffer.writeUtf(packet.selectedOwner(), MAX_TEXT);
        buffer.writeBoolean(packet.selectedOwnedByPlayer());
        buffer.writeBoolean(packet.selectedManageable());
        buffer.writeBoolean(packet.selectedReleaseAllowed());
        buffer.writeInt(packet.claimCount());
        buffer.writeInt(packet.maxClaims());
        buffer.writeInt(packet.gridRadius());
        buffer.writeUtf(packet.status(), MAX_TEXT);
        writeList(buffer, packet.chunks(), MAX_CHUNKS, BaseGridSnapshotPacket::writeChunk);
        writeList(buffer, packet.members(), MAX_MEMBERS, BaseGridSnapshotPacket::writeMember);
        writeList(buffer, packet.candidates(), MAX_CANDIDATES, BaseGridSnapshotPacket::writePlayer);
    }

    private static BaseGridSnapshotPacket read(RegistryFriendlyByteBuf buffer) {
        String dimension = buffer.readUtf(MAX_TEXT);
        int centerX = buffer.readInt();
        int centerZ = buffer.readInt();
        int selectedX = buffer.readInt();
        int selectedZ = buffer.readInt();
        String selectedKey = buffer.readUtf(MAX_TEXT);
        String selectedState = buffer.readUtf(MAX_TEXT);
        String selectedOwner = buffer.readUtf(MAX_TEXT);
        boolean selectedOwnedByPlayer = buffer.readBoolean();
        boolean selectedManageable = buffer.readBoolean();
        boolean selectedReleaseAllowed = buffer.readBoolean();
        int claimCount = buffer.readInt();
        int maxClaims = buffer.readInt();
        int radius = buffer.readInt();
        String status = buffer.readUtf(MAX_TEXT);
        List<ChunkData> chunks = readList(buffer, MAX_CHUNKS, BaseGridSnapshotPacket::readChunk);
        List<MemberData> members = readList(buffer, MAX_MEMBERS, BaseGridSnapshotPacket::readMember);
        List<PlayerData> candidates = readList(buffer, MAX_CANDIDATES, BaseGridSnapshotPacket::readPlayer);
        return new BaseGridSnapshotPacket(dimension, centerX, centerZ, selectedX, selectedZ, selectedKey,
                selectedState, selectedOwner, selectedOwnedByPlayer, selectedManageable, selectedReleaseAllowed,
                claimCount, maxClaims, radius, status, chunks, members, candidates);
    }

    private static void writeChunk(RegistryFriendlyByteBuf buffer, ChunkData chunk) {
        buffer.writeUtf(chunk.key(), MAX_TEXT);
        buffer.writeUtf(chunk.dimension(), MAX_TEXT);
        buffer.writeInt(chunk.chunkX());
        buffer.writeInt(chunk.chunkZ());
        buffer.writeInt(chunk.dx());
        buffer.writeInt(chunk.dz());
        buffer.writeBoolean(chunk.selected());
        buffer.writeBoolean(chunk.current());
        buffer.writeUtf(chunk.state(), MAX_TEXT);
        buffer.writeUtf(chunk.ownerName(), MAX_TEXT);
        buffer.writeUtf(chunk.label(), MAX_TEXT);
    }

    private static ChunkData readChunk(RegistryFriendlyByteBuf buffer) {
        return new ChunkData(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT), buffer.readInt(), buffer.readInt(),
                buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT));
    }

    private static void writeMember(RegistryFriendlyByteBuf buffer, MemberData member) {
        buffer.writeUtf(member.uuid(), MAX_TEXT);
        buffer.writeUtf(member.name(), MAX_TEXT);
        buffer.writeUtf(member.role(), MAX_TEXT);
        buffer.writeUtf(member.roleLabel(), MAX_TEXT);
        buffer.writeBoolean(member.build());
        buffer.writeBoolean(member.interact());
        buffer.writeBoolean(member.containers());
        buffer.writeBoolean(member.manage());
        buffer.writeBoolean(member.manageable());
    }

    private static MemberData readMember(RegistryFriendlyByteBuf buffer) {
        return new MemberData(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_TEXT), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    private static void writePlayer(RegistryFriendlyByteBuf buffer, PlayerData player) {
        buffer.writeUtf(player.uuid(), MAX_TEXT);
        buffer.writeUtf(player.name(), MAX_TEXT);
    }

    private static PlayerData readPlayer(RegistryFriendlyByteBuf buffer) {
        return new PlayerData(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT));
    }

    private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, int max, Writer<T> writer) {
        List<T> safe = values == null ? List.of() : values.stream().limit(max).toList();
        buffer.writeInt(safe.size());
        for (T value : safe) {
            writer.write(buffer, value);
        }
    }

    private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, int max, Reader<T> reader) {
        int size = Math.max(0, Math.min(max, buffer.readInt()));
        java.util.ArrayList<T> values = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(reader.read(buffer));
        }
        return List.copyOf(values);
    }

    @FunctionalInterface
    private interface Writer<T> {
        void write(RegistryFriendlyByteBuf buffer, T value);
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(RegistryFriendlyByteBuf buffer);
    }

    public record ChunkData(String key, String dimension, int chunkX, int chunkZ, int dx, int dz,
            boolean selected, boolean current, String state, String ownerName, String label) {
        public ChunkData {
            key = key == null ? "" : key;
            dimension = dimension == null ? "" : dimension;
            state = state == null ? "unclaimed" : state;
            ownerName = ownerName == null ? "" : ownerName;
            label = label == null ? "" : label;
        }
    }

    public record MemberData(String uuid, String name, String role, String roleLabel, boolean build,
            boolean interact, boolean containers, boolean manage, boolean manageable) {
        public MemberData {
            uuid = uuid == null ? "" : uuid;
            name = name == null ? "" : name;
            role = role == null ? "" : role;
            roleLabel = roleLabel == null ? "" : roleLabel;
        }
    }

    public record PlayerData(String uuid, String name) {
        public PlayerData {
            uuid = uuid == null ? "" : uuid;
            name = name == null ? "" : name;
        }
    }
}
