package com.knoxhack.echoholomap.world;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.integration.HoloMapMissionHooks;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class HoloMapWaypointSavedData extends SavedData {
    public static final Codec<HoloMapWaypointSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HoloMapWaypoint.CODEC.listOf().optionalFieldOf("waypoints", List.of())
                    .forGetter(HoloMapWaypointSavedData::storedWaypoints)
    ).apply(instance, HoloMapWaypointSavedData::new));

    public static final SavedDataType<HoloMapWaypointSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "waypoints"),
            HoloMapWaypointSavedData::new,
            CODEC);

    private final Map<String, HoloMapWaypoint> waypoints = new LinkedHashMap<>();

    public HoloMapWaypointSavedData() {
    }

    private HoloMapWaypointSavedData(List<HoloMapWaypoint> stored) {
        for (HoloMapWaypoint waypoint : stored) {
            if (waypoint != null && waypoint.scope() != Scope.LOCAL) {
                waypoints.put(waypoint.id().toString(), waypoint);
            }
        }
    }

    public static HoloMapWaypointSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static HoloMapWaypointSavedData get(MinecraftServer server) {
        return get(server.overworld());
    }

    public boolean upsert(ServerPlayer actor, HoloMapWaypoint waypoint, boolean mayEditShared) {
        if (actor == null || waypoint == null || waypoint.scope() == Scope.LOCAL) {
            return false;
        }
        Scope scope = waypoint.scope();
        if (scope == Scope.SHARED && !mayEditShared) {
            return false;
        }
        HoloMapWaypoint existing = waypoints.get(waypoint.id().toString());
        if (existing != null && !canEdit(actor.getUUID(), existing, mayEditShared)) {
            return false;
        }
        long now = actor.level().getGameTime();
        UUID owner = scope == Scope.PERSONAL ? actor.getUUID() : waypoint.owner();
        if (owner == null || owner.equals(HoloMapWaypoint.NO_OWNER)) {
            owner = actor.getUUID();
        }
        long created = existing == null ? waypoint.createdTime() : existing.createdTime();
        if (created <= 0L) {
            created = now;
        }
        HoloMapWaypoint normalized = new HoloMapWaypoint(
                waypoint.id(),
                owner,
                scope,
                waypoint.dimension(),
                waypoint.x(),
                waypoint.y(),
                waypoint.z(),
                waypoint.title(),
                waypoint.color(),
                waypoint.icon(),
                waypoint.visible(),
                created,
                Math.max(created, now));
        waypoints.put(normalized.id().toString(), normalized);
        evictPersonal(actor.getUUID(), personalLimit());
        setDirty();
        HoloMapMissionHooks.recordWaypointCreated(actor, normalized.scope().name().toLowerCase(java.util.Locale.ROOT));
        return true;
    }

    public boolean delete(ServerPlayer actor, Identifier waypointId, boolean mayEditShared) {
        if (actor == null || waypointId == null) {
            return false;
        }
        HoloMapWaypoint existing = waypoints.get(waypointId.toString());
        if (existing == null || !canEdit(actor.getUUID(), existing, mayEditShared)) {
            return false;
        }
        waypoints.remove(waypointId.toString());
        setDirty();
        return true;
    }

    public HoloMapWaypoint recordDeathpoint(ServerPlayer player, int maxDeathpoints) {
        if (player == null) {
            return null;
        }
        long now = player.level().getGameTime();
        Identifier id = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID,
                "deathpoint/" + player.getUUID() + "/" + UUID.randomUUID());
        HoloMapWaypoint waypoint = new HoloMapWaypoint(
                id,
                player.getUUID(),
                Scope.PERSONAL,
                player.level().dimension().identifier().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                "Deathpoint " + player.blockPosition().getX() + ", " + player.blockPosition().getZ(),
                0xFFFF6688,
                HoloMapWaypoint.DEATHPOINT_ICON,
                true,
                now,
                now);
        waypoints.put(waypoint.id().toString(), waypoint);
        evictDeathpoints(player.getUUID(), maxDeathpoints);
        setDirty();
        return waypoint;
    }

    public int evictDeathpoints(UUID owner, int maxDeathpoints) {
        if (owner == null) {
            return 0;
        }
        List<HoloMapWaypoint> deathpoints = waypoints.values().stream()
                .filter(waypoint -> waypoint.scope() == Scope.PERSONAL)
                .filter(waypoint -> waypoint.owner().equals(owner))
                .filter(HoloMapWaypoint::isDeathpoint)
                .sorted(Comparator.comparingLong(HoloMapWaypoint::updatedTime))
                .toList();
        int remove = deathpoints.size() - Math.max(0, maxDeathpoints);
        for (int i = 0; i < remove; i++) {
            waypoints.remove(deathpoints.get(i).id().toString());
        }
        if (remove > 0) {
            setDirty();
        }
        return Math.max(0, remove);
    }

    public List<HoloMapWaypoint> waypointsFor(ServerPlayer player, int limit) {
        if (player == null || limit <= 0) {
            return List.of();
        }
        return waypointsFor(player.getUUID(), limit);
    }

    public List<HoloMapWaypoint> waypointsFor(UUID playerId, int limit) {
        if (playerId == null || limit <= 0) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(2048, limit));
        return waypoints.values().stream()
                .filter(HoloMapWaypoint::visible)
                .filter(waypoint -> waypoint.scope() == Scope.SHARED
                        || (waypoint.scope() == Scope.PERSONAL && waypoint.owner().equals(playerId)))
                .sorted(Comparator.comparing(HoloMapWaypoint::scope)
                        .thenComparing(waypoint -> !waypoint.isDeathpoint())
                        .thenComparing(HoloMapWaypoint::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(waypoint -> waypoint.id().toString()))
                .limit(safeLimit)
                .toList();
    }

    public int countFor(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        int count = 0;
        for (HoloMapWaypoint waypoint : waypoints.values()) {
            if (waypoint.scope() == Scope.SHARED || waypoint.owner().equals(playerId)) {
                count++;
            }
        }
        return count;
    }

    public void putForTests(HoloMapWaypoint waypoint) {
        if (waypoint != null && waypoint.scope() != Scope.LOCAL) {
            waypoints.put(waypoint.id().toString(), waypoint);
            setDirty();
        }
    }

    public void clearForTests() {
        waypoints.clear();
        setDirty();
    }

    private boolean canEdit(UUID actor, HoloMapWaypoint waypoint, boolean mayEditShared) {
        if (waypoint.scope() == Scope.SHARED) {
            return mayEditShared;
        }
        return waypoint.scope() == Scope.PERSONAL && waypoint.owner().equals(actor);
    }

    private void evictPersonal(UUID owner, int maxPersonal) {
        if (owner == null || maxPersonal <= 0) {
            return;
        }
        List<HoloMapWaypoint> personal = waypoints.values().stream()
                .filter(waypoint -> waypoint.scope() == Scope.PERSONAL && waypoint.owner().equals(owner))
                .sorted(Comparator.comparingLong(HoloMapWaypoint::updatedTime))
                .toList();
        int remove = personal.size() - maxPersonal;
        for (int i = 0; i < remove; i++) {
            waypoints.remove(personal.get(i).id().toString());
        }
    }

    private List<HoloMapWaypoint> storedWaypoints() {
        return List.copyOf(waypoints.values());
    }

    private static int personalLimit() {
        try {
            return Math.max(16, Math.min(1024, Config.WAYPOINT_SYNC_LIMIT.get()));
        } catch (RuntimeException exception) {
            return 256;
        }
    }
}
