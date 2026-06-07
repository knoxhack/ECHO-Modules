package com.knoxhack.echobasegrid.data;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimMember;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.api.ClaimRole;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class BaseGridSavedData extends SavedData {
    private static final Codec<StoredMember> MEMBER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("uuid").forGetter(StoredMember::uuid),
            Codec.STRING.optionalFieldOf("name", "Unknown").forGetter(StoredMember::name),
            Codec.STRING.optionalFieldOf("role", ClaimRole.MEMBER.name()).forGetter(StoredMember::role),
            Codec.STRING.listOf().optionalFieldOf("permissions", List.of()).forGetter(StoredMember::permissions)
    ).apply(instance, StoredMember::new));

    private static final Codec<StoredClaim> CLAIM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(StoredClaim::dimension),
            Codec.INT.fieldOf("chunkX").forGetter(StoredClaim::chunkX),
            Codec.INT.fieldOf("chunkZ").forGetter(StoredClaim::chunkZ),
            Codec.STRING.fieldOf("ownerUuid").forGetter(StoredClaim::ownerUuid),
            Codec.STRING.optionalFieldOf("ownerName", "Unknown").forGetter(StoredClaim::ownerName),
            MEMBER_CODEC.listOf().optionalFieldOf("members", List.of()).forGetter(StoredClaim::members),
            Codec.LONG.optionalFieldOf("createdGameTime", 0L).forGetter(StoredClaim::createdGameTime),
            Codec.LONG.optionalFieldOf("updatedGameTime", 0L).forGetter(StoredClaim::updatedGameTime)
    ).apply(instance, StoredClaim::new));

    public static final Codec<BaseGridSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, CLAIM_CODEC).optionalFieldOf("claims", Map.of())
                    .forGetter(BaseGridSavedData::storedClaims)
    ).apply(instance, BaseGridSavedData::new));

    public static final SavedDataType<BaseGridSavedData> TYPE = new SavedDataType<>(
            EchoBaseGrid.id("claims"),
            BaseGridSavedData::new,
            CODEC);

    private final Map<String, ClaimRecord> claims = new LinkedHashMap<>();

    public BaseGridSavedData() {
    }

    private BaseGridSavedData(Map<String, StoredClaim> storedClaims) {
        for (StoredClaim stored : storedClaims.values()) {
            ClaimRecord claim = stored.toClaim();
            if (claim != null) {
                claims.put(claim.key(), claim);
            }
        }
    }

    public static BaseGridSavedData get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        if (storageLevel == null) {
            storageLevel = level;
        }
        return storageLevel.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<ClaimRecord> claims() {
        return List.copyOf(claims.values());
    }

    public Optional<ClaimRecord> claim(String dimension, int chunkX, int chunkZ) {
        return Optional.ofNullable(claims.get(ClaimRecord.key(dimension, chunkX, chunkZ)));
    }

    public int claimCount(UUID ownerId) {
        if (ownerId == null) {
            return 0;
        }
        int count = 0;
        for (ClaimRecord claim : claims.values()) {
            if (ownerId.equals(claim.ownerId())) {
                count++;
            }
        }
        return count;
    }

    public void put(ClaimRecord claim) {
        if (claim == null) {
            return;
        }
        claims.put(claim.key(), claim);
        setDirty();
    }

    public boolean remove(String dimension, int chunkX, int chunkZ) {
        boolean removed = claims.remove(ClaimRecord.key(dimension, chunkX, chunkZ)) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    private Map<String, StoredClaim> storedClaims() {
        Map<String, StoredClaim> stored = new LinkedHashMap<>();
        for (ClaimRecord claim : claims.values()) {
            stored.put(claim.key(), StoredClaim.from(claim));
        }
        return stored;
    }

    private record StoredMember(String uuid, String name, String role, List<String> permissions) {
        private static StoredMember from(ClaimMember member) {
            return new StoredMember(
                    member.playerId().toString(),
                    member.playerName(),
                    member.role().name(),
                    member.permissions().stream().map(Enum::name).toList());
        }

        private ClaimMember toMember() {
            UUID id;
            try {
                id = UUID.fromString(uuid);
            } catch (RuntimeException exception) {
                return null;
            }
            EnumSet<ClaimPermission> permissionSet = EnumSet.noneOf(ClaimPermission.class);
            for (String permission : permissions == null ? List.<String>of() : permissions) {
                permissionSet.add(ClaimPermission.fromId(permission));
            }
            ClaimRole parsedRole = ClaimRole.fromId(role);
            if (permissionSet.isEmpty()) {
                permissionSet.addAll(parsedRole.defaultPermissions());
            }
            return new ClaimMember(id, name, parsedRole, permissionSet);
        }
    }

    private record StoredClaim(String dimension, int chunkX, int chunkZ, String ownerUuid, String ownerName,
            List<StoredMember> members, long createdGameTime, long updatedGameTime) {
        private static StoredClaim from(ClaimRecord claim) {
            return new StoredClaim(
                    claim.dimension(),
                    claim.chunkX(),
                    claim.chunkZ(),
                    claim.ownerId().toString(),
                    claim.ownerName(),
                    claim.sortedMembers().stream().map(StoredMember::from).toList(),
                    claim.createdGameTime(),
                    claim.updatedGameTime());
        }

        private ClaimRecord toClaim() {
            UUID owner;
            try {
                owner = UUID.fromString(ownerUuid);
            } catch (RuntimeException exception) {
                return null;
            }
            Map<UUID, ClaimMember> memberMap = new LinkedHashMap<>();
            for (StoredMember storedMember : members == null ? List.<StoredMember>of() : members) {
                ClaimMember member = storedMember.toMember();
                if (member != null && !member.playerId().equals(owner)) {
                    memberMap.put(member.playerId(), member);
                }
            }
            return new ClaimRecord(dimension, chunkX, chunkZ, owner, ownerName, memberMap,
                    createdGameTime, updatedGameTime);
        }
    }
}
