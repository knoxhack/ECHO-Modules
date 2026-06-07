package com.knoxhack.echobasegrid.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record ClaimRecord(
        String dimension,
        int chunkX,
        int chunkZ,
        UUID ownerId,
        String ownerName,
        Map<UUID, ClaimMember> members,
        long createdGameTime,
        long updatedGameTime) {
    public ClaimRecord {
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        ownerName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName.strip();
        members = members == null ? Map.of() : Map.copyOf(members);
    }

    public String key() {
        return key(dimension, chunkX, chunkZ);
    }

    public boolean ownedBy(UUID playerId) {
        return playerId != null && playerId.equals(ownerId);
    }

    public Optional<ClaimMember> member(UUID playerId) {
        return Optional.ofNullable(playerId == null ? null : members.get(playerId));
    }

    public boolean allows(UUID playerId, ClaimPermission permission) {
        if (ownedBy(playerId)) {
            return true;
        }
        ClaimMember member = playerId == null ? null : members.get(playerId);
        return member != null && member.permissions().contains(permission);
    }

    public ClaimRecord withMember(ClaimMember member, long gameTime) {
        if (member == null || member.playerId() == null || member.playerId().equals(ownerId)) {
            return this;
        }
        Map<UUID, ClaimMember> next = new LinkedHashMap<>(members);
        next.put(member.playerId(), member);
        return new ClaimRecord(dimension, chunkX, chunkZ, ownerId, ownerName, next, createdGameTime, gameTime);
    }

    public ClaimRecord withoutMember(UUID playerId, long gameTime) {
        if (playerId == null || !members.containsKey(playerId)) {
            return this;
        }
        Map<UUID, ClaimMember> next = new LinkedHashMap<>(members);
        next.remove(playerId);
        return new ClaimRecord(dimension, chunkX, chunkZ, ownerId, ownerName, next, createdGameTime, gameTime);
    }

    public ClaimRecord withUpdatedMember(ClaimMember member, long gameTime) {
        if (member == null || member.playerId() == null || !members.containsKey(member.playerId())) {
            return this;
        }
        Map<UUID, ClaimMember> next = new LinkedHashMap<>(members);
        next.put(member.playerId(), member);
        return new ClaimRecord(dimension, chunkX, chunkZ, ownerId, ownerName, next, createdGameTime, gameTime);
    }

    public List<ClaimMember> sortedMembers() {
        return members.values().stream()
                .sorted(Comparator.comparing(ClaimMember::playerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static String key(String dimension, int chunkX, int chunkZ) {
        String safeDimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        return safeDimension + "|" + chunkX + "|" + chunkZ;
    }
}
